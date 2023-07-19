package qupath.lib.images.servers.omero.common.omero_entities.repository_entities.server_entities.image;

import com.google.gson.annotations.SerializedName;

import java.util.Optional;

/**
 * <p>This class contains various information related to the pixels of an image.</p>
 * <p>
 *     It uses the {@link qupath.lib.images.servers.omero.common.omero_entities.repository_entities.server_entities.image.PhysicalSize PhysicalSize}
 *     and {@link qupath.lib.images.servers.omero.common.omero_entities.repository_entities.server_entities.image.ImageType ImageType} classes.
 * </p>
 */
class PixelInfo {
    @SerializedName(value = "SizeX") private int width;
    @SerializedName(value = "SizeY") private int height;
    @SerializedName(value = "SizeZ") private int z;
    @SerializedName(value = "SizeC") private int c;
    @SerializedName(value = "SizeT") private int t;
    @SerializedName(value = "PhysicalSizeX") private PhysicalSize physicalSizeX;
    @SerializedName(value = "PhysicalSizeY") private PhysicalSize physicalSizeY;
    @SerializedName(value = "PhysicalSizeZ") private PhysicalSize physicalSizeZ;
    @SerializedName(value = "Type") private ImageType imageType;

    /**
     * @return the dimensions (width in pixels, height in pixels, number of z-slices, number of channels, number of time points)
     * of the image. If a dimension was not found, 0 is returned
     */
    public int[] getImageDimensions() {
        return new int[] { width, height, c, z, t };
    }

    /**
     * @return the pixel width, or an empty Optional if not found
     */
    public Optional<PhysicalSize> getPhysicalSizeX() {
        return Optional.ofNullable(physicalSizeX);
    }

    /**
     * @return the pixel height, or an empty Optional if not found
     */
    public Optional<PhysicalSize> getPhysicalSizeY() {
        return Optional.ofNullable(physicalSizeY);
    }

    /**
     * @return the spacing between z-slices, or an empty Optional if not found
     */
    public Optional<PhysicalSize> getPhysicalSizeZ() {
        return Optional.ofNullable(physicalSizeZ);
    }

    /**
     * @return the format of the pixel values (e.g. uint8)
     */
    public Optional<String> getPixelType() {
        return imageType == null ? Optional.empty() : imageType.getValue();
    }
}
