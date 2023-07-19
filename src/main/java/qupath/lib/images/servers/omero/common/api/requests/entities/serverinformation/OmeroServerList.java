package qupath.lib.images.servers.omero.common.api.requests.entities.serverinformation;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Optional;

/**
 * Response of the <a href="https://docs.openmicroscopy.org/omero/5.6.0/developers/json-api.html#list-available-omero-servers">server list API request</a>
 */
public class OmeroServerList {
    @SerializedName("data") private List<OmeroServerInfo> serverInfos;

    /**
     * @return the ID of the first server we can connect to,
     * or an empty Optional if it was not found
     */
    public Optional<Integer> getServerId() {
        if (serverInfos == null || serverInfos.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(serverInfos.get(0).getId());
        }
    }
}

class OmeroServerInfo {
    @SerializedName("id") private int id;

    public int getId() {
        return id;
    }
}
