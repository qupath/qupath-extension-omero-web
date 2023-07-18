package qupath.lib.images.servers.omero.common.api.requests.entities.server_information;

import com.google.gson.annotations.SerializedName;

import java.util.Optional;

class OmeroAPIVersion {
    @SerializedName("url:base") private String versionURL;

    public Optional<String> getVersionURL() {
        return Optional.ofNullable(versionURL);
    }
}
