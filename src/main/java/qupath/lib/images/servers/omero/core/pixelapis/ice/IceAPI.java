package qupath.lib.images.servers.omero.core.pixelapis.ice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.images.servers.ImageServerMetadata;
import qupath.lib.images.servers.omero.core.WebClient;
import qupath.lib.images.servers.omero.core.pixelapis.PixelAPI;
import qupath.lib.images.servers.omero.core.pixelapis.PixelAPIReader;

import java.io.IOException;

/**
 * <p>
 *     This API uses the <a href="https://omero.readthedocs.io/en/v5.6.7/developers/Java.html">OMERO gateway</a>
 *     to access pixel values of an image. Any image can be used, and pixel values are accurate.
 * </p>
 */
public class IceAPI implements PixelAPI {

    private static final Logger logger = LoggerFactory.getLogger(IceAPI.class);
    static final String NAME = "Ice";
    private static Boolean gatewayAvailable;
    private final WebClient client;

    static {
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

    /**
     * Creates a new IceAPI.
     *
     * @param client  the WebClient owning this API
     */
    public IceAPI(WebClient client) {
        this.client = client;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean isAvailable() {
        return client.getAuthenticated().get() && gatewayAvailable;
    }

    @Override
    public boolean canAccessRawPixels() {
        return true;
    }

    @Override
    public boolean canReadImage(boolean isUint8, boolean has3Channels) {
        return true;
    }

    @Override
    public PixelAPIReader createReader(
            long id,
            ImageServerMetadata metadata,
            boolean allowSmoothInterpolation,
            int nResolutions,
            String... args
    ) throws IOException {
        if (!isAvailable()) {
            throw new IllegalStateException("This API is not available and cannot be used");
        }
        if (!canReadImage(metadata)) {
            throw new IllegalArgumentException("The provided image cannot be read by this API");
        }

        return new IceReader(client, id, metadata.getChannels());
    }

    @Override
    public String toString() {
        return String.format("Ice API of %s", client.getServerURI());
    }
}
