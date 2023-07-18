package qupath.lib.images.servers.omero.common.api.requests.entities.image_metadata;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.images.servers.ImageChannel;
import qupath.lib.images.servers.ImageServerMetadata;

import java.util.List;
import java.util.Optional;

/**
 * Reads the response from an image metadata request.
 */
public class ImageMetadataResponse {
    private static final Logger logger = LoggerFactory.getLogger(ImageMetadataResponse.class);
    private final String imageName;
    private final int sizeX;
    private final int sizeY;
    private final int sizeT;
    private final int sizeZ;
    private final int tileSizeX;
    private final int tileSizeY;
    private final List<ImageServerMetadata.ImageResolutionLevel> levels;
    private final double magnification;
    private final double pixelWidthMicrons;
    private final double pixelHeightMicrons;
    private final double zSpacingMicrons;

    private ImageMetadataResponse(
            String imageName,
            int sizeX,
            int sizeY,
            int sizeT,
            int sizeZ,
            int tileSizeX,
            int tileSizeY,
            List<ImageServerMetadata.ImageResolutionLevel> levels,
            double magnification,
            double pixelWidthMicrons,
            double pixelHeightMicrons,
            double zSpacingMicrons
    ) {
        this.imageName = imageName;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeT = sizeT;
        this.sizeZ = sizeZ;
        this.tileSizeX = tileSizeX;
        this.tileSizeY = tileSizeY;
        this.levels = levels;
        this.magnification = magnification;
        this.pixelWidthMicrons = pixelWidthMicrons;
        this.pixelHeightMicrons = pixelHeightMicrons;
        this.zSpacingMicrons = zSpacingMicrons;
    }

    /**
     * Create a response from a JSON object.
     *
     * @param jsonObject  the API response
     * @return an ImageMetadataResponse if the json was successfully processed or an empty Optional
     */
    public static Optional<ImageMetadataResponse> createFromJson(JsonObject jsonObject) {
        try {
            JsonObject size = jsonObject.getAsJsonObject("size");
            int sizeX = size.getAsJsonPrimitive("width").getAsInt();
            int sizeY = size.getAsJsonPrimitive("height").getAsInt();
            int sizeC = size.getAsJsonPrimitive("c").getAsInt();

            double pixelWidthMicrons = Double.NaN;
            double pixelHeightMicrons = Double.NaN;
            double zSpacingMicrons = Double.NaN;

            JsonElement pixelSizeElement = jsonObject.get("pixel_size");
            if (pixelSizeElement != null) {
                JsonObject pixelSize = pixelSizeElement.getAsJsonObject();
                // TODO: Check micron assumption

                if (pixelSize.has("x") && !pixelSize.get("x").isJsonNull())
                    pixelWidthMicrons = pixelSize.getAsJsonPrimitive("x").getAsDouble();
                if (pixelSize.has("y") && !pixelSize.get("y").isJsonNull())
                    pixelHeightMicrons = pixelSize.getAsJsonPrimitive("y").getAsDouble();
                if (pixelSize.has("z")) {
                    JsonElement zSpacing = pixelSize.get("z");
                    if (!zSpacing.isJsonNull())
                        zSpacingMicrons = zSpacing.getAsDouble();
                }
            }

            String pixelsType = "";
            String imageName = "";
            if (jsonObject.has("meta")) {
                JsonObject meta = jsonObject.getAsJsonObject("meta");
                if (meta.has("imageName"))
                    imageName = meta.get("imageName").getAsString();
                if (meta.has("pixelsType"))
                    pixelsType = meta.get("pixelsType").getAsString();
            }

            List<ImageChannel> channels = sizeC == 3 ? ImageChannel.getDefaultRGBChannels() : null;

            if (channels == null || (pixelsType != null && !"uint8".equals(pixelsType))) {
                throw new RuntimeException("Only 8-bit RGB images supported! Selected image has " + sizeC + " channel(s) & pixel type " + pixelsType);
            } else {
                int tileSizeX;
                int tileSizeY;
                var levelBuilder = new ImageServerMetadata.ImageResolutionLevel.Builder(sizeX, sizeY);

                if (jsonObject.getAsJsonPrimitive("tiles").getAsBoolean()) {
                    int levels = jsonObject.getAsJsonPrimitive("levels").getAsInt();
                    if (levels > 1) {
                        JsonObject zoom = jsonObject.getAsJsonObject("zoomLevelScaling");
                        for (int i = 0; i < levels; i++) {
                            levelBuilder.addLevelByDownsample(1.0 / zoom.getAsJsonPrimitive(Integer.toString(i)).getAsDouble());
                        }
                    } else {
                        levelBuilder.addFullResolutionLevel();
                    }

                    if (jsonObject.has("tile_size")) {
                        JsonObject tileSizeJson = jsonObject.getAsJsonObject("tile_size");
                        tileSizeX = (int) tileSizeJson.getAsJsonPrimitive("width").getAsDouble();
                        tileSizeY = (int) tileSizeJson.getAsJsonPrimitive("height").getAsDouble();
                    } else {
                        tileSizeX = sizeX;
                        tileSizeY = sizeY;
                    }
                } else {
                    tileSizeX = Math.min(sizeX, 3192);
                    tileSizeY = Math.min(sizeY, 3192);
                }

                double magnification = Double.NaN;
                if (jsonObject.has("nominalMagnification"))
                    magnification = jsonObject.getAsJsonPrimitive("nominalMagnification").getAsDouble();

                return Optional.of(new ImageMetadataResponse(
                        imageName,
                        sizeX,
                        sizeY,
                        size.getAsJsonPrimitive("t").getAsInt(),
                        size.getAsJsonPrimitive("z").getAsInt(),
                        tileSizeX,
                        tileSizeY,
                        levelBuilder.build(),
                        magnification,
                        pixelWidthMicrons,
                        pixelHeightMicrons,
                        zSpacingMicrons
                ));
            }
        } catch (Exception e) {
            logger.error("Could not create image metadata", e);
            return Optional.empty();
        }
    }

    /**
     * @return the image name
     */
    public String getImageName() {
        return imageName;
    }

    /**
     * @return the image width in pixels
     */
    public int getSizeX() {
        return sizeX;
    }

    /**
     * @return the image height in pixels
     */
    public int getSizeY() {
        return sizeY;
    }

    /**
     * @return the number of time points
     */
    public int getSizeT() {
        return sizeT;
    }

    /**
     * @return the number of z-slices
     */
    public int getSizeZ() {
        return sizeZ;
    }

    /**
     * @return the preferred tile width in pixels
     */
    public int getTileSizeX() {
        return tileSizeX;
    }

    /**
     * @return the preferred tile height in pixels
     */
    public int getTileSizeY() {
        return tileSizeY;
    }

    /**
     * @return the resolution levels (see {@link qupath.lib.images.servers.ImageServerMetadata.Builder#levels levels})
     */
    public List<ImageServerMetadata.ImageResolutionLevel> getLevels() {
        return levels;
    }

    /**
     * @return the magnification value for the highest-resolution image, or an empty Optional
     * if this value could not be retrieved
     */
    public Optional<Double> getMagnification() {
        return getDoubleField(magnification);
    }

    /**
     * @return the pixel width in microns, or an empty Optional
     * if this value could not be retrieved
     */
    public Optional<Double> getPixelWidthMicrons() {
        return getDoubleField(pixelWidthMicrons);
    }

    /**
     * @return the pixel height in microns, or an empty Optional
     * if this value could not be retrieved
     */
    public Optional<Double> getPixelHeightMicrons() {
        return getDoubleField(pixelHeightMicrons);
    }

    /**
     * @return the spacing between z-slices in microns, or an empty Optional
     * if this value could not be retrieved
     */
    public Optional<Double> getzSpacingMicrons() {
        return getDoubleField(zSpacingMicrons);
    }

    private Optional<Double> getDoubleField(double field) {
        return Double.isNaN(field) ? Optional.empty() : Optional.of(field);
    }
}
