package qupath.lib.images.servers.omero.common.omero_entities.repository_entities.server_entities.image;

import com.google.gson.annotations.SerializedName;

import java.util.Optional;

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

    public int[] getImageDimensions() {
        return new int[] { width, height, c, z, t };
    }

    public Optional<PhysicalSize> getPhysicalSizeX() {
        return Optional.ofNullable(physicalSizeX);
    }

    public Optional<PhysicalSize> getPhysicalSizeY() {
        return Optional.ofNullable(physicalSizeY);
    }

    public Optional<PhysicalSize> getPhysicalSizeZ() {
        return Optional.ofNullable(physicalSizeZ);
    }

    public Optional<String> getPixelType() {
        return imageType == null ? Optional.empty() : imageType.getValue();
    }
}
