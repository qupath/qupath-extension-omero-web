package qupath.lib.images.servers.omero.common.imagesservers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.images.servers.*;
import qupath.lib.images.servers.omero.common.api.clients.WebClient;
import qupath.lib.images.servers.omero.common.api.requests.RequestsUtilities;
import qupath.lib.images.servers.omero.common.imagesservers.pixelsapis.ice.IceAPI;
import qupath.lib.images.servers.omero.common.imagesservers.pixelsapis.PixelAPI;
import qupath.lib.images.servers.omero.common.imagesservers.pixelsapis.web.WebAPI;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.PathObjectReader;

import java.awt.image.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * <p>{@link qupath.lib.images.servers.ImageServer Image server} of the extension.</p>
 * <p>Pixels are read using the {@link qupath.lib.images.servers.omero.common.imagesservers.pixelsapis pixelsapis} package:</p>
 * <ul>
 *     <li>
 *         If <a href="https://www.openmicroscopy.org/omero/downloads/">OMERO Java</a> files are found,
 *         the {@link qupath.lib.images.servers.omero.common.imagesservers.pixelsapis.ice ice} package is used.
 *     </li>
 *     <li>
 *         Else (or if an error occurred with the previous method), the
 *         {@link qupath.lib.images.servers.omero.common.imagesservers.pixelsapis.web web} package is used.
 *     </li>
 *  </ul>
 */
public class OmeroImageServer extends AbstractTileableImageServer implements PathObjectReader  {
    private static final Logger logger = LoggerFactory.getLogger(OmeroImageServer.class);
    private static final List<String> INVALID_PARAMETERS = List.of("--password",  "-password", "-p", "-u", "--username", "-username");
    private final URI uri;
    private final WebClient client;
    private final String[] args;
    private static Boolean gatewayAvailable = null;
    private PixelAPI pixelAPI;
    private long id;
    private ImageServerMetadata originalMetadata;

    private OmeroImageServer(URI uri, WebClient client, String... args) {
        this.uri = uri;
        this.client = client;
        this.args = args;
    }

    /**
     * Attempt to create an OmeroImageServer.
     *
     * @param uri  the image URI
     * @param client  the corresponding WebClient
     * @param args  optional arguments used to open the image
     * @return an OmeroImageServer, or an empty Optional if an error occurred
     */
    public static Optional<OmeroImageServer> create(URI uri, WebClient client, String... args) {
        OmeroImageServer omeroImageServer = new OmeroImageServer(uri, client, args);
        if (omeroImageServer.setId() && omeroImageServer.checkArguments(args) && omeroImageServer.setOriginalMetadata()) {

            omeroImageServer.createPixelAPI();
            client.addOpenedImage(uri);

            return Optional.of(omeroImageServer);
        } else {
            return Optional.empty();
        }
    }

    @Override
    protected BufferedImage readTile(TileRequest tileRequest) {
        return pixelAPI.readTile(tileRequest);
    }

    @Override
    protected ImageServerBuilder.ServerBuilder<BufferedImage> createServerBuilder() {
        return ImageServerBuilder.DefaultImageServerBuilder.createInstance(
                OmeroImageServerBuilder.class,
                getMetadata(),
                uri,
                args
        );
    }

    @Override
    protected String createID() {
        return getClass().getName() + ": " + uri.toString() + " args=" + Arrays.toString(args);
    }

    @Override
    public Collection<URI> getURIs() {
        return Collections.singletonList(uri);
    }

    @Override
    public String getServerType() {
        String api = pixelAPI instanceof IceAPI ? " (ICE)" : " (web)";
        return "OMERO" + api;
    }

    @Override
    public ImageServerMetadata getOriginalMetadata() {
        return originalMetadata;
    }

    @Override
    public Collection<PathObject> readPathObjects() {
        try {
            return client.getRequestsHandler().getROIs(id).get();
        } catch (Exception e) {
            logger.error("Error reading path objects", e);
            return Collections.emptyList();
        }
    }

    /**
     * @return whether image servers can read any image (e.g. 64-bits multi-channels images).
     * If not, only uint8 RGB images can be read.
     */
    public static boolean canReadAllImages() {
        if (gatewayAvailable == null) {
            try {
                Class.forName("omero.gateway.Gateway");
                gatewayAvailable = true;
            } catch (ClassNotFoundException e) {
                logger.debug(
                        "OMERO Ice gateway is unavailable ('omero.gateway.Gateway' not found)." +
                        "Falling back to the JSON API."
                );
                gatewayAvailable = false;
            }
        }
        return gatewayAvailable;
    }

    /**
     * Closes all image servers of a client.
     * This function should be called each time a client is destroyed.
     *
     * @param client  the client about to be destroyed
     */
    public static void closeImageServersOfClient(WebClient client) {
        IceAPI.closeApisOfClient(client);
    }

    /**
     * <p>Send annotations to the server of this image.</p>
     * <p>This function is asynchronous.</p>
     *
     * @param pathObjects  the annotations to send
     * @return a CompletableFuture indicating the success of the operation
     */
    public CompletableFuture<Boolean> sendAnnotations(Collection<PathObject> pathObjects) {
        return client.getRequestsHandler().writeROIs(id, pathObjects);
    }

    private boolean setId() {
        var id = RequestsUtilities.parseEntityId(uri);

        if (id.isPresent()) {
            this.id = id.get();
        } else {
            logger.error("Could not get image ID from " + uri);
        }
        return id.isPresent();
    }

    private boolean setOriginalMetadata() {
        try {
            var imageMetadataResponse = client.getRequestsHandler().getImageMetadata(id).get();

            if (imageMetadataResponse.isPresent()) {
                ImageServerMetadata.Builder builder = new ImageServerMetadata.Builder(
                        OmeroImageServer.class,
                        uri.toString(),
                        imageMetadataResponse.get().getSizeX(),
                        imageMetadataResponse.get().getSizeY()
                )
                        .name(imageMetadataResponse.get().getImageName())
                        .sizeT(imageMetadataResponse.get().getSizeT())
                        .sizeZ(imageMetadataResponse.get().getSizeZ())
                        .preferredTileSize(imageMetadataResponse.get().getTileSizeX(), imageMetadataResponse.get().getTileSizeY())
                        .levels(imageMetadataResponse.get().getLevels())
                        .pixelType(imageMetadataResponse.get().getPixelType())
                        .channels(imageMetadataResponse.get().getChannels())
                        .rgb(imageMetadataResponse.get().isRGB());

                if (imageMetadataResponse.get().getMagnification().isPresent()) {
                    builder.magnification(imageMetadataResponse.get().getMagnification().get());
                }

                if (imageMetadataResponse.get().getPixelWidthMicrons().isPresent() && imageMetadataResponse.get().getPixelHeightMicrons().isPresent()) {
                    builder.pixelSizeMicrons(
                            imageMetadataResponse.get().getPixelWidthMicrons().get(),
                            imageMetadataResponse.get().getPixelHeightMicrons().get()
                    );
                }

                if (imageMetadataResponse.get().getZSpacingMicrons().isPresent() && imageMetadataResponse.get().getZSpacingMicrons().get() > 0) {
                    builder.zSpacingMicrons(imageMetadataResponse.get().getZSpacingMicrons().get());
                }

                originalMetadata = builder.build();
            }

            return imageMetadataResponse.isPresent();
        } catch (Exception e) {
            logger.error("Error when creating metadata", e);
            return false;
        }
    }

    private boolean checkArguments(String... args) {
        for (String s : args) {
            String arg = s.toLowerCase().strip();

            if (INVALID_PARAMETERS.contains(arg)) {
                logger.error("Cannot build server with arg " + arg + ". Consider removing such sensitive data.");
                return false;
            }
        }
        return true;
    }

    private void createPixelAPI() {
        if (canReadAllImages()) {
            IceAPI.create(
                    client,
                    id,
                    getMetadata().getChannels()
            ).ifPresent(iceAPI -> pixelAPI = iceAPI);
        }

        if (pixelAPI == null) {
            pixelAPI = new WebAPI(
                    client.getRequestsHandler(),
                    id,
                    allowSmoothInterpolation(),
                    nResolutions(),
                    getMetadata().getPreferredTileWidth(),
                    getMetadata().getPreferredTileHeight(),
                    args
            );
        }
    }
}
