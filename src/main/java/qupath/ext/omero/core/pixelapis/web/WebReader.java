package qupath.ext.omero.core.pixelapis.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.omero.core.apis.ApisHandler;
import qupath.lib.images.servers.TileRequest;
import qupath.ext.omero.core.pixelapis.PixelAPIReader;

import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutionException;

/**
 * Read pixel values using the <a href="https://docs.openmicroscopy.org/omero/5.6.0/developers/json-api.html">OMERO JSON API</a>.
 */
class WebReader implements PixelAPIReader {

    private static final Logger logger = LoggerFactory.getLogger(WebReader.class);
    private static final double DEFAULT_JPEG_QUALITY = 0.9;
    private final ApisHandler apisHandler;
    private final long imageID;
    private final boolean allowSmoothInterpolation;
    private final int numberOfResolutions;
    private final int preferredTileWidth;
    private final int preferredTileHeight;
    private double quality = DEFAULT_JPEG_QUALITY;

    /**
     * Creates a new WebAPI.
     *
     * @param apisHandler  the request handler which will be used to perform web requests
     * @param imageID  the ID of the image to open
     * @param allowSmoothInterpolation  whether to use smooth interpolation when resizing
     * @param numberOfResolutions  the number of resolutions of the image to open
     * @param preferredTileWidth  the preferred tile width of the image to open in pixels
     * @param preferredTileHeight  the preferred tile height of the image to open in pixels
     * @param args  optional arguments to set the JPEG quality of the image to open (between 0 and 1).
     *              Usage: --quality 0.5 or -q 0.5 for a quality of 0.5.
     */
    public WebReader(
            ApisHandler apisHandler,
            long imageID,
            boolean allowSmoothInterpolation,
            int numberOfResolutions,
            int preferredTileWidth,
            int preferredTileHeight,
            String... args
    ) {
        this.apisHandler = apisHandler;
        this.imageID = imageID;
        this.allowSmoothInterpolation = allowSmoothInterpolation;
        this.numberOfResolutions = numberOfResolutions;
        this.preferredTileWidth = preferredTileWidth;
        this.preferredTileHeight = preferredTileHeight;
        setQuality(args);
    }

    @Override
    public BufferedImage readTile(TileRequest tileRequest) {
        try {
            if (numberOfResolutions > 1) {
                return apisHandler.readMultiResolutionTile(
                        imageID,
                        tileRequest,
                        preferredTileWidth,
                        preferredTileHeight,
                        quality
                ).get().orElse(null);
            } else {
                return apisHandler.readSingleResolutionTile(
                        imageID,
                        tileRequest,
                        preferredTileWidth,
                        preferredTileHeight,
                        quality,
                        allowSmoothInterpolation
                ).get().orElse(null);
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Unable to read tile {}", tileRequest, e);
            return null;
        }
    }

    @Override
    public String getName() {
        return WebAPI.NAME;
    }

    @Override
    public void close() {}

    @Override
    public String toString() {
        return String.format("Web reader of image with ID %d", imageID);
    }

    private void setQuality(String... args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i].toLowerCase().strip();
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
}
