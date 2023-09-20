package qupath.lib.images.servers.omero.web.entities.imagemetadata;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.common.ColorTools;
import qupath.lib.images.servers.ImageChannel;
import qupath.lib.images.servers.ImageServerMetadata;
import qupath.lib.images.servers.PixelType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reads the response from an image metadata request.
 */
public class ImageMetadataResponse {

    private static final Logger logger = LoggerFactory.getLogger(ImageMetadataResponse.class);
    private static final Map<String, PixelType> PIXEL_TYPE_MAP = Map.of(
            "uint8", PixelType.UINT8,
            "int8", PixelType.INT8,
            "uint16", PixelType.UINT16,
            "int16", PixelType.INT16,
            "int32", PixelType.INT32,
            "uint32", PixelType.UINT32,
            "float", PixelType.FLOAT32,
            "double", PixelType.FLOAT64
    );
    private final String imageName;
    private final int sizeX;
    private final int sizeY;
    private final int sizeT;
    private final int sizeZ;
    private final int tileSizeX;
    private final int tileSizeY;
    private final List<ImageServerMetadata.ImageResolutionLevel> levels;
    private final PixelType pixelType;
    private final List<ImageChannel> channels;
    private final boolean isRGB;
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
            PixelType pixelType,
            List<ImageChannel> channels,
            boolean isRGB,
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
        this.pixelType = pixelType;
        this.channels = channels;
        this.isRGB = isRGB;
        this.magnification = magnification;
        this.pixelWidthMicrons = pixelWidthMicrons;
        this.pixelHeightMicrons = pixelHeightMicrons;
        this.zSpacingMicrons = zSpacingMicrons;
    }

    @Override
    public String toString() {
        return String.format("ImageMetadataResponse of %s which is of type %s. Its channels are %s", imageName, pixelType, channels);
    }

    /**
     * Create a response from a JSON object.
     *
     * @param jsonObject  the API response
     * @return an ImageMetadataResponse if the json was successfully processed or an empty Optional otherwise
     */
    public static Optional<ImageMetadataResponse> createFromJson(JsonObject jsonObject) {
        try {
            JsonObject size = jsonObject.getAsJsonObject("size");
            int sizeX = size.getAsJsonPrimitive("width").getAsInt();
            int sizeY = size.getAsJsonPrimitive("height").getAsInt();

            double pixelWidthMicrons = Double.NaN;
            double pixelHeightMicrons = Double.NaN;
            double zSpacingMicrons = Double.NaN;

            JsonElement pixelSizeElement = jsonObject.get("pixel_size");
            if (pixelSizeElement != null) {
                JsonObject pixelSize = pixelSizeElement.getAsJsonObject();
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

            List<JsonElement> channelsJson = jsonObject.getAsJsonArray("channels").asList();
            List<ImageChannel> channels = new ArrayList<>();
            for (int i=0; i<channelsJson.size(); ++i) {
                try {
                    String color = channelsJson.get(i).getAsJsonObject().get("color").getAsString();
                    channels.add(ImageChannel.getInstance(
                            channelsJson.get(i).getAsJsonObject().get("label").getAsString(),
                            ColorTools.packRGB(
                                    Integer.valueOf(color.substring(0, 2), 16),
                                    Integer.valueOf(color.substring(2, 4), 16),
                                    Integer.valueOf(color.substring(4, 6), 16)
                            )
                    ));
                } catch (Exception e) {
                    channels.add(ImageChannel.getInstance(
                            "Channel " + i,
                            ImageChannel.getDefaultChannelColor(i)
                    ));
                }
            }

            String imageName = "";
            PixelType pixelType = null;
            if (jsonObject.has("meta")) {
                JsonObject meta = jsonObject.getAsJsonObject("meta");

                if (meta.has("imageName"))
                    imageName = meta.get("imageName").getAsString();

                if (meta.has("pixelsType"))
                    pixelType = PIXEL_TYPE_MAP.get(meta.get("pixelsType").getAsString());
            }

            if (pixelType == null) {
                throw new RuntimeException(
                        "Unable to set pixel type from " + jsonObject +
                                "\nAvailable pixel types are: " + PIXEL_TYPE_MAP.keySet() + "."
                );
            }

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
                    pixelType,
                    channels,
                    channels.size() == 3 && pixelType == PixelType.UINT8,
                    magnification,
                    pixelWidthMicrons,
                    pixelHeightMicrons,
                    zSpacingMicrons
            ));

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
     * @return the type of the pixels of the image
     */
    public PixelType getPixelType() {
        return pixelType;
    }

    /**
     * @return the channels of the image
     */
    public List<ImageChannel> getChannels() {
        return channels;
    }

    /**
     * @return whether the image stores pixels in (A)RGB form
     */
    public boolean isRGB() {
        return isRGB;
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
    public Optional<Double> getZSpacingMicrons() {
        return getDoubleField(zSpacingMicrons);
    }

    private Optional<Double> getDoubleField(double field) {
        return Double.isNaN(field) ? Optional.empty() : Optional.of(field);
    }
}
