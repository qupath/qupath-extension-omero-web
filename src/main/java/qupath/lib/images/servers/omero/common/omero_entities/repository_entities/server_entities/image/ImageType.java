package qupath.lib.images.servers.omero.common.omero_entities.repository_entities.server_entities.image;

import com.google.gson.annotations.SerializedName;

import java.util.Optional;

class ImageType {
    @SerializedName(value = "value") private String value;

    public Optional<String> getValue() {
        return Optional.ofNullable(value);
    }
}
