package qupath.lib.images.servers.omero.common.imagesservers.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.images.servers.*;
import qupath.lib.images.servers.omero.common.api.requests.RequestsUtilities;
import qupath.lib.images.servers.omero.common.api.clients.WebClient;
import qupath.lib.images.servers.omero.common.imagesservers.OmeroImageServer;
import qupath.lib.objects.*;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * <p>{@link qupath.lib.images.servers.ImageServer Image server} that uses the OMERO JSON API.</p>
 * <p>It doesn't have dependencies but can only work with 8-bit RGB images, and the images are JPEG-compressed.</p>
 */
class WebImageServer extends AbstractTileableImageServer implements PathObjectReader, OmeroImageServer {
    private static final Logger logger = LoggerFactory.getLogger(WebImageServer.class);
    private static final double DEFAULT_JPEG_QUALITY = 0.9;
    private static final List<String> INVALID_PARAMETERS = List.of("--password",  "-password", "-p", "-u", "--username", "-username");
    private final URI uri;
    private final WebClient client;
    private final String[] args;
    private long id;
    private ImageServerMetadata originalMetadata;
    private double quality = DEFAULT_JPEG_QUALITY;

    private WebImageServer(URI uri, WebClient client, String... args) {
        this.uri = uri;
        this.client = client;
        this.args = args;
    }

    /**
     * Attempt to create a WebImageServer.
     *
     * @param uri  the image URI
     * @param client  the corresponding WebClient
     * @param args  optional arguments used to open the image
     * @return a WebImageServer, or an empty Optional if an error occurred
     */
    public static Optional<WebImageServer> create(URI uri, WebClient client, String... args) {
        try (WebImageServer webImageServer = new WebImageServer(uri, client, args)) {
            if (webImageServer.setId() && webImageServer.setOriginalMetadata() && webImageServer.setQuality(args)) {
                client.addOpenedImage(uri);
                return Optional.of(webImageServer);
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            logger.error("Error when creating web image server", e);
            return Optional.empty();
        }
    }

    @Override
    protected BufferedImage readTile(TileRequest tileRequest) {
        try {
            if (nResolutions() > 1) {
                return client.getRequestsHandler().readMultiDimensionalTile(
                        id,
                        tileRequest,
                        getMetadata().getPreferredTileWidth(),
                        getMetadata().getPreferredTileHeight(),
                        quality
                ).get().orElse(null);
            } else {
                return client.getRequestsHandler().readOneDimensionalTile(
                        id,
                        tileRequest,
                        getMetadata().getPreferredTileWidth(),
                        getMetadata().getPreferredTileHeight(),
                        quality,
                        allowSmoothInterpolation()
                ).get().orElse(null);
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Unable to read tile {}", tileRequest, e);
            return null;
        }
    }

    @Override
    protected ImageServerBuilder.ServerBuilder<BufferedImage> createServerBuilder() {
        return ImageServerBuilder.DefaultImageServerBuilder.createInstance(
                WebImageServerBuilder.class,
                getMetadata(),
                uri,
                args
        );
    }

    @Override
    protected String createID() {
        return getClass().getName() + ": " + uri.toString() + " quality=" + quality;
    }

    @Override
    public Collection<URI> getURIs() {
        return Collections.singletonList(uri);
    }

    @Override
    public String getServerType() {
        return "OMERO web";
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

    @Override
    public CompletableFuture<Boolean> sendAnnotations(Collection<PathObject> pathObjects) {
        return client.getRequestsHandler().writeROIs(id, pathObjects);
    }

    private boolean setId() {
        var id = RequestsUtilities.parseImageId(uri);

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
                        WebImageServer.class,
                        uri.toString(),
                        imageMetadataResponse.get().getSizeX(),
                        imageMetadataResponse.get().getSizeY()
                )
                        .name(imageMetadataResponse.get().getImageName())
                        .sizeT(imageMetadataResponse.get().getSizeT())
                        .sizeZ(imageMetadataResponse.get().getSizeZ())
                        .preferredTileSize(imageMetadataResponse.get().getTileSizeX(), imageMetadataResponse.get().getTileSizeY())
                        .levels(imageMetadataResponse.get().getLevels())
                        .channels(ImageChannel.getDefaultRGBChannels())
                        .pixelType(PixelType.UINT8)
                        .rgb(true);

                if (imageMetadataResponse.get().getMagnification().isPresent()) {
                    builder.magnification(imageMetadataResponse.get().getMagnification().get());
                }

                if (imageMetadataResponse.get().getPixelWidthMicrons().isPresent() && imageMetadataResponse.get().getPixelHeightMicrons().isPresent()) {
                    builder.pixelSizeMicrons(
                            imageMetadataResponse.get().getPixelWidthMicrons().get(),
                            imageMetadataResponse.get().getPixelHeightMicrons().get()
                    );
                }

                if (imageMetadataResponse.get().getzSpacingMicrons().isPresent() && imageMetadataResponse.get().getzSpacingMicrons().get() > 0) {
                    builder.zSpacingMicrons(imageMetadataResponse.get().getzSpacingMicrons().get());
                }

                originalMetadata = builder.build();
            }

            return imageMetadataResponse.isPresent();
        } catch (Exception e) {
            logger.error("Error when creating metadata", e);
            return false;
        }
    }

    private boolean setQuality(String... args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i].toLowerCase().strip();

            if (INVALID_PARAMETERS.contains(arg)) {
                logger.error("Cannot build server with arg " + arg + ". Consider removing such sensitive data.");
                return false;
            } else {
                if (arg.equals("--quality") || arg.equals("-q")) {
                    if (i < args.length-1) {
                        try {
                            var parsedQuality = Double.parseDouble(args[i+1]);
                            if (parsedQuality > 0 && parsedQuality <= 1) {
                                quality = parsedQuality;
                            } else {
                                logger.warn("Requested JPEG quality '{}' is invalid, must be between 0 and 1. I will use {} instead.", parsedQuality, quality);
                            }
                        } catch (NumberFormatException ex) {
                            logger.error("Unable to parse JPEG quality from {}", args[i+1], ex);
                        }
                    }
                }
            }
        }
        return true;
    }
}
