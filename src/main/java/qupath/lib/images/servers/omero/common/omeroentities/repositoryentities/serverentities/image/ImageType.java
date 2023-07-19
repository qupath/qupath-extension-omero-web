package qupath.lib.images.servers.omero.common.omeroentities.repositoryentities.serverentities.image;

import com.google.gson.annotations.SerializedName;

import java.util.Optional;

/**
 * This class contains the format the pixel values of an image use.
 * For example, uint8 (for 8-bit images).
 */
class ImageType {
    @SerializedName(value = "value") private String value;

    /**
     * @return the format the pixel values use, or an empty Optional if not found
     */
    public Optional<String> getValue() {
        return Optional.ofNullable(value);
    }
}
