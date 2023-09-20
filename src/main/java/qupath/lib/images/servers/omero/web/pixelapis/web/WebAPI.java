package qupath.lib.images.servers.omero.web.pixelapis.web;

import qupath.lib.images.servers.ImageServerMetadata;
import qupath.lib.images.servers.omero.web.WebClient;
import qupath.lib.images.servers.omero.web.pixelapis.PixelAPI;
import qupath.lib.images.servers.omero.web.pixelapis.PixelAPIReader;

/**
 * <p>
 *     This API uses the <a href="https://docs.openmicroscopy.org/omero/5.6.0/developers/json-api.html">OMERO JSON API</a>
 *     to access pixel values of an image. It doesn't have dependencies but can only work with 8-bit RGB images,
 *     and the images are JPEG-compressed.
 * </p>
 */
public class WebAPI implements PixelAPI {

    static final String NAME = "Web API";
    private final WebClient client;

    /**
     * Creates a new WebAPI.
     *
     * @param client  the WebClient owning this API
     */
    public WebAPI(WebClient client) {
        this.client = client;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean canAccessRawPixels() {
        return false;
    }

    @Override
    public boolean canReadImage(boolean isUint8, boolean has3Channels) {
        return isUint8 && has3Channels;
    }

    @Override
    public PixelAPIReader createReader(
            long id,
            ImageServerMetadata metadata,
            boolean allowSmoothInterpolation,
            int nResolutions,
            String... args
    ) {
        if (!isAvailable()) {
            throw new IllegalStateException("This API is not available and cannot be used");
        }
        if (!canReadImage(metadata)) {
            throw new IllegalArgumentException("The provided image cannot be read by this API");
        }

        return new WebReader(
                client.getApisHandler(),
                id,
                allowSmoothInterpolation,
                nResolutions,
                metadata.getPreferredTileWidth(),
                metadata.getPreferredTileHeight(),
                args
        );
    }

    @Override
    public String toString() {
        return String.format("Web API of %s", client);
    }
}
