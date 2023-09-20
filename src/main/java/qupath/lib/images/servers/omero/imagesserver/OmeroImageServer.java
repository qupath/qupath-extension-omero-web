package qupath.lib.images.servers.omero.imagesserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.images.servers.*;
import qupath.lib.images.servers.omero.web.WebClient;
import qupath.lib.images.servers.omero.web.WebUtilities;
import qupath.lib.images.servers.omero.web.pixelapis.PixelAPIReader;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.PathObjectReader;

import java.awt.image.*;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * <p>{@link qupath.lib.images.servers.ImageServer Image server} of the extension.</p>
 * <p>Pixels are read using the {@link qupath.lib.images.servers.omero.web.pixelapis PixelAPIs} package.</p>
 */
public class OmeroImageServer extends AbstractTileableImageServer implements PathObjectReader  {

    private static final Logger logger = LoggerFactory.getLogger(OmeroImageServer.class);
    private static final List<String> INVALID_PARAMETERS = List.of("--password",  "-password", "-p", "-u", "--username", "-username");
    private final URI uri;
    private final WebClient client;
    private final String[] args;
    private PixelAPIReader pixelAPIReader;
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
            try {
                if (client.getSelectedPixelAPI().get().canReadImage(omeroImageServer.getMetadata())) {
                    omeroImageServer.pixelAPIReader = client.getSelectedPixelAPI().get().createReader(
                            omeroImageServer.id,
                            omeroImageServer.getMetadata(),
                            omeroImageServer.allowSmoothInterpolation(),
                            omeroImageServer.nResolutions(),
                            args
                    );
                } else {
                    logger.error("The selected pixel API can't read the provided image");
                    return Optional.empty();
                }
            } catch (IOException e) {
                logger.error("Couldn't create pixel API reader", e);
                return Optional.empty();
            }
            client.addOpenedImage(uri);

            return Optional.of(omeroImageServer);
        } else {
            return Optional.empty();
        }
    }

    @Override
    protected BufferedImage readTile(TileRequest tileRequest) throws IOException {
        return pixelAPIReader.readTile(tileRequest);
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
        return String.format("OMERO (%s)", pixelAPIReader.getName());
    }

    @Override
    public ImageServerMetadata getOriginalMetadata() {
        return originalMetadata;
    }

    @Override
    public Collection<PathObject> readPathObjects() {
        try {
            return client.getApisHandler().getROIs(id).get();
        } catch (Exception e) {
            logger.error("Error reading path objects", e);
            return Collections.emptyList();
        }
    }

    @Override
    public void close() throws Exception {
        if (pixelAPIReader != null) {
            pixelAPIReader.close();
        }
    }

    @Override
    public String toString() {
        return String.format("OMERO image server of %s for the image whose id is %d", client, id);
    }

    /**
     * <p>Send annotations to the server of this image.</p>
     * <p>Detections are not supported by OMERO and won't be sent to the server.</p>
     * <p>This function is asynchronous.</p>
     *
     * @param pathObjects  the annotations to send
     * @return a CompletableFuture indicating the success of the operation
     */
    public CompletableFuture<Boolean> sendAnnotations(Collection<PathObject> pathObjects) {
        var detections = pathObjects.stream().filter(PathObject::isDetection).toList();
        if (!detections.isEmpty()) {
            logger.warn(detections.size() + " detections have been detected and won't be sent.");
        }

        return client.getApisHandler().writeROIs(id, pathObjects.stream().filter(e -> !e.isDetection()).toList());
    }

    /**
     * @return the client owning this image server
     */
    public WebClient getClient() {
        return client;
    }

    private boolean setId() {
        var id = WebUtilities.parseEntityId(uri);

        if (id.isPresent()) {
            this.id = id.getAsInt();
        } else {
            logger.error("Could not get image ID from " + uri);
        }
        return id.isPresent();
    }

    private boolean setOriginalMetadata() {
        try {
            var imageMetadataResponse = client.getApisHandler().getImageMetadata(id).get();

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
}
