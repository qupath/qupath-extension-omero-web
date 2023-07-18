package qupath.lib.images.servers.omero.common.api.requests.entities.server_information;

import com.google.gson.annotations.SerializedName;

class OmeroServerInfo {
    @SerializedName("id") private int id;

    public int getId() {
        return id;
    }
}
