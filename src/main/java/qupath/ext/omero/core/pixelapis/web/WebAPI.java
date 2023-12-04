package qupath.ext.omero.core.pixelapis.web;

import javafx.beans.property.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.omero.core.ClientsPreferencesManager;
import qupath.ext.omero.core.WebClient;
import qupath.lib.images.servers.ImageServerMetadata;
import qupath.ext.omero.core.pixelapis.PixelAPI;
import qupath.ext.omero.core.pixelapis.PixelAPIReader;
import qupath.lib.images.servers.PixelType;

/**
 * <p>
 *     This API uses the <a href="https://docs.openmicroscopy.org/omero/5.6.0/developers/json-api.html">OMERO JSON API</a>
 *     to access pixel values of an image. It doesn't have dependencies but can only work with 8-bit RGB images,
 *     and the images are JPEG-compressed.
 * </p>
 */
public class WebAPI implements PixelAPI {

    static final String NAME = "Web";
    private static final float DEFAULT_JPEG_QUALITY = 0.9F;
    private static final String JPEG_QUALITY_PARAMETER = "--jpegQuality";
    private static final Logger logger = LoggerFactory.getLogger(WebAPI.class);
    private final WebClient client;
    private final FloatProperty jpegQuality;

    /**
     * Creates a new WebAPI.
     *
     * @param client  the WebClient owning this API
     */
    public WebAPI(WebClient client) {
        this.client = client;
        jpegQuality = new SimpleFloatProperty(
                ClientsPreferencesManager.getWebJpegQuality(client.getApisHandler().getWebServerURI().toString()).orElse(DEFAULT_JPEG_QUALITY)
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof WebAPI webAPI))
            return false;
        return webAPI.client.equals(client);
    }

    @Override
    public int hashCode() {
        return client.hashCode();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String[] getArgs() {
        return new String[] {JPEG_QUALITY_PARAMETER, String.valueOf(jpegQuality.get())};
    }

    @Override
    public void setParametersFromArgs(String... args) {
        for (int i=0; i<args.length-1; ++i) {
            if (args[i].equals(JPEG_QUALITY_PARAMETER)) {
                try {
                    setJpegQuality(Float.parseFloat(args[i+1]));
                } catch (NumberFormatException e) {
                    logger.warn(String.format("Can't convert %s to float", args[i+1]), e);
                }
            }
        }
    }

    @Override
    public ReadOnlyBooleanProperty isAvailable() {
        return new SimpleBooleanProperty(true);
    }

    @Override
    public boolean canAccessRawPixels() {
        return false;
    }

    @Override
    public boolean canReadImage(PixelType pixelType) {
        return pixelType.equals(PixelType.UINT8);
    }

    @Override
    public boolean canReadImage(int numberOfChannels) {
        return numberOfChannels == 3;
    }

    @Override
    public PixelAPIReader createReader(
            long id,
            ImageServerMetadata metadata,
            boolean allowSmoothInterpolation,
            int nResolutions
    ) {
        if (!isAvailable().get()) {
            throw new IllegalStateException("This API is not available and cannot be used");
        }
        if (!canReadImage(metadata.getPixelType(), metadata.getSizeC())) {
            throw new IllegalArgumentException("The provided image cannot be read by this API");
        }

        return new WebReader(
                client.getApisHandler(),
                id,
                allowSmoothInterpolation,
                nResolutions,
                metadata.getPreferredTileWidth(),
                metadata.getPreferredTileHeight(),
                jpegQuality.get()
        );
    }

    @Override
    public String toString() {
        return String.format("Web API of %s", client.getApisHandler().getWebServerURI());
    }

    /**
     * @return the JPEG quality used by this pixel API.
     * This property may be updated from any thread
     */
    public ReadOnlyFloatProperty getJpegQuality() {
        return jpegQuality;
    }

    /**
     * Set the JPEG quality used by this pixel API.
     *
     * @param jpegQuality  the JPEG quality (number between 0 and 1)
     */
    public void setJpegQuality(float jpegQuality) {
        if (jpegQuality > 0 && jpegQuality <= 1) {
            this.jpegQuality.set(jpegQuality);

            ClientsPreferencesManager.setWebJpegQuality(
                    client.getApisHandler().getWebServerURI().toString(),
                    jpegQuality
            );
        } else {
            logger.warn("Requested JPEG quality '{}' is invalid, must be between 0 and 1.", jpegQuality);
        }
    }
}
