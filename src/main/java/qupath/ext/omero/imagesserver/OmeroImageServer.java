package qupath.ext.omero.imagesserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.omero.core.entities.shapes.Shape;
import qupath.lib.images.servers.*;
import qupath.ext.omero.core.WebClient;
import qupath.ext.omero.core.WebUtilities;
import qupath.ext.omero.core.pixelapis.PixelAPI;
import qupath.ext.omero.core.pixelapis.PixelAPIReader;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.PathObjectReader;

import java.awt.image.*;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * <p>{@link qupath.lib.images.servers.ImageServer Image server} of the extension.</p>
 * <p>Pixels are read using the {@link qupath.ext.omero.core.pixelapis PixelAPIs} package.</p>
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
     * This should only be used by an {@link OmeroImageServerBuilder}.
     *
     * @param uri  the image URI
     * @param client  the corresponding WebClient
     * @param args  optional arguments used to open the image
     * @return an OmeroImageServer, or an empty Optional if an error occurred
     */
    static Optional<OmeroImageServer> create(URI uri, WebClient client, String... args) {
        OmeroImageServer omeroImageServer = new OmeroImageServer(uri, client, args);
        if (omeroImageServer.setId() && omeroImageServer.setOriginalMetadata()) {
            try {
                PixelAPI pixelAPI;
                var pixelAPIFromArgs = getPixelAPIFromArgs(client, args);

                if (pixelAPIFromArgs.isPresent()) {
                    pixelAPI = pixelAPIFromArgs.get();
                } else {
                    pixelAPI = client.getSelectedPixelAPI().get();
                    if (pixelAPI == null) {
                        logger.error("No selected pixel API");
                        return Optional.empty();
                    }

                    omeroImageServer.savePixelAPIToArgs(pixelAPI);
                }

                pixelAPI.setParametersFromArgs(args);

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
                return client.getApisHandler().getThumbnail(id).get().orElse(null);
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
            List<Shape> shapes = client.getApisHandler().getROIs(id).get();

            Map<UUID, UUID> idToParentId = new HashMap<>();
            Map<UUID, PathObject> idToPathObject = new HashMap<>();
            for (Shape shape: shapes) {
                UUID id = shape.getQuPathId();
                idToPathObject.put(id, shape.createPathObject());
                idToParentId.put(id, shape.getQuPathParentId().orElse(null));
            }

            List<PathObject> pathObjects = new ArrayList<>();
            for (Map.Entry<UUID, UUID> entry: idToParentId.entrySet()) {
                if (idToPathObject.containsKey(entry.getValue())) {
                    idToPathObject.get(entry.getValue()).addChildObject(idToPathObject.get(entry.getKey()));
                } else {
                    pathObjects.add(idToPathObject.get(entry.getKey()));
                }
            }

            return pathObjects;
        } catch (InterruptedException | ExecutionException e) {
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
     * Attempt to send some path objects to the OMERO server.
     *
     * @param pathObjects  the path objects to send
     * @param removeExistingAnnotations  whether to remove existing annotations of the image in the OMERO server
     * @return whether the operation succeeded
     */
    public boolean sendPathObjects(Collection<PathObject> pathObjects, boolean removeExistingAnnotations) {
        try {
            return client.getApisHandler().writeROIs(
                    id,
                    pathObjects.stream()
                            .map(Shape::createFromPathObject)
                            .flatMap(List::stream)
                            .toList(),
                    removeExistingAnnotations
            ).get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Could not send path objects");
            return false;
        }
    }

    /**
     * @return the client owning this image server
     */
    public WebClient getClient() {
        return client;
    }

    /**
     * @return the ID of the image
     */
    public long getId() {
        return id;
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
            if (PIXEL_API_ARGUMENT.equalsIgnoreCase(parameter.trim())) {
                pixelAPIName = args[i++].trim();
            }
        }

        if (pixelAPIName != null) {
            for (PixelAPI pixelAPI: client.getAvailablePixelAPIs()) {
                if (pixelAPI.getName().equalsIgnoreCase(pixelAPIName)) {
                    return Optional.of(pixelAPI);
                }
            }
            logger.warn(
                    "The provided pixel API (" + pixelAPIName + ") was not recognized, or the corresponding OMERO server doesn't support it." +
                            "Another one will be used."
            );
        }

        return Optional.empty();
    }

    private void savePixelAPIToArgs(PixelAPI pixelAPI) {
        String[] pixelApiArgs = pixelAPI.getArgs();
        int currentArgsSize = args.length;

        args = Arrays.copyOf(args, args.length + 2 + pixelApiArgs.length);

        args[currentArgsSize] = PIXEL_API_ARGUMENT;
        args[currentArgsSize + 1] = pixelAPI.getName();

        for (int i=0; i<pixelApiArgs.length; ++i) {
            args[currentArgsSize + 2 + i] = pixelApiArgs[i];
        }
    }
}
