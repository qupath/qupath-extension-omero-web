package qupath.lib.images.servers.omero.imagesserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.images.servers.*;
import qupath.lib.images.servers.omero.web.WebClient;
import qupath.lib.images.servers.omero.web.WebUtilities;
import qupath.lib.images.servers.omero.web.pixelapis.PixelAPI;
import qupath.lib.images.servers.omero.web.pixelapis.PixelAPIReader;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.PathObjectReader;

import java.awt.image.*;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * <p>{@link qupath.lib.images.servers.ImageServer Image server} of the extension.</p>
 * <p>Pixels are read using the {@link qupath.lib.images.servers.omero.web.pixelapis PixelAPIs} package.</p>
 */
public class OmeroImageServer extends AbstractTileableImageServer implements PathObjectReader  {

    private static final Logger logger = LoggerFactory.getLogger(OmeroImageServer.class);
    private static final String PIXEL_API_ARGUMENT = "--pixelAPI";
    private final URI uri;
    private final WebClient client;
    private String[] args;
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
        if (omeroImageServer.setId() && omeroImageServer.setOriginalMetadata()) {
            try {
                PixelAPI pixelAPI;
                var pixelAPIFromArgs = getPixelAPIFromArgs(client, args);

                if (pixelAPIFromArgs.isPresent()) {
                    pixelAPI = pixelAPIFromArgs.get();
                } else {
                    pixelAPI = client.getSelectedPixelAPI().get();
                    omeroImageServer.savePixelAPIToArgs(pixelAPI);
                }

                if (pixelAPI.canReadImage(omeroImageServer.getMetadata())) {
                    omeroImageServer.pixelAPIReader = pixelAPI.createReader(
                            omeroImageServer.id,
                            omeroImageServer.getMetadata(),
                            omeroImageServer.allowSmoothInterpolation(),
                            omeroImageServer.nResolutions(),
                            args
                    );
                } else {
                    logger.error("The selected pixel API (" + pixelAPI + ") can't read the provided image");
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
    public BufferedImage getDefaultThumbnail(int z, int t) throws IOException {
        if (isRGB()) {
            try {
                return client.getThumbnail(id).get().orElse(null);
            } catch (InterruptedException | ExecutionException e) {
                throw new IOException(e);
            }
        } else {
            return super.getDefaultThumbnail(z, t);
        }
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
        return String.format("OMERO image server of %s", uri);
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

    private static Optional<PixelAPI> getPixelAPIFromArgs(WebClient client, String... args) {
        String pixelAPIName = null;
        int i = 0;
        while (i < args.length-1) {
            String parameter = args[i++];
            if (PIXEL_API_ARGUMENT.equalsIgnoreCase(parameter)) {
                pixelAPIName = args[i++];
            }
        }

        if (pixelAPIName != null) {
            for (PixelAPI pixelAPI: client.getAvailablePixelAPIs()) {
                if (pixelAPI.getName().equalsIgnoreCase(pixelAPIName)) {
                    return Optional.of(pixelAPI);
                }
            }
            logger.warn(
                    "The provided pixel API " + pixelAPIName + " was not recognized, or the corresponding OMERO server doesn't support it."
            );
        }

        return Optional.empty();
    }

    private void savePixelAPIToArgs(PixelAPI pixelAPI) {
        args = Arrays.copyOf(args, args.length + 2);
        args[args.length - 2] = PIXEL_API_ARGUMENT;
        args[args.length - 1] = pixelAPI.getName();
    }
}