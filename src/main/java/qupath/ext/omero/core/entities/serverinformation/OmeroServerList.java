package qupath.ext.omero.core.entities.serverinformation;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.OptionalInt;

/**
 * Response of the <a href="https://docs.openmicroscopy.org/omero/5.6.0/developers/json-api.html#list-available-omero-servers">server list API request</a>
 */
public class OmeroServerList {

    @SerializedName("data") private List<OmeroServerInfo> serverInfos;

    @Override
    public String toString() {
        return String.format("OMERO server list: %s", serverInfos);
    }

    /**
     * @return the ID of the first server we can connect to,
     * or an empty Optional if it was not found
     */
    public OptionalInt getServerId() {
        if (serverInfos == null || serverInfos.isEmpty()) {
            return OptionalInt.empty();
        } else {
            return OptionalInt.of(serverInfos.get(0).getId());
        }
    }

    /**
     * @return the port of the first server we can connect to,
     * or an empty Optional if it was not found
     */
    public OptionalInt getServerPort() {
        if (serverInfos == null || serverInfos.isEmpty()) {
            return OptionalInt.empty();
        } else {
            return OptionalInt.of(serverInfos.get(0).getPort());
        }
    }
}

class OmeroServerInfo {

    @SerializedName("id") private int id;
    @SerializedName("port") private int port;

    @Override
    public String toString() {
        return String.format("ID: %d, port: %d", id, port);
    }

    public int getId() {
        return id;
    }

    public int getPort() {
        return port;
    }
}
