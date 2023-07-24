package qupath.lib.images.servers.omero.connectionsmanager.connectionsmanager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import qupath.lib.images.servers.omero.common.api.clients.ClientsPreferencesManager;
import qupath.lib.images.servers.omero.common.api.clients.WebClient;
import qupath.lib.images.servers.omero.common.api.clients.WebClients;

/**
 * <p>
 *     The model of the connections manager. It contains lists which determine
 *     parts of the UI rendered by the connections manager.
 * </p>
 * <p>
 *     In effect, this class acts as an intermediate between a connections manager and a
 *     {@link qupath.lib.images.servers.omero.common.api.clients.WebClients WebClients} and a
 *     {@link qupath.lib.images.servers.omero.common.api.clients.ClientsPreferencesManager ClientsPreferencesManager}.
 *     Lists of these classes can be updated from any thread but the connections manager can
 *     only be accessed from the UI thread, so this class propagates changes made to these elements
 *     from any thread to the UI thread.
 * </p>
 */
class ConnectionsManagerModel {
    private final static ObservableList<String> storedServersURIs = FXCollections.observableArrayList();
    private final static ObservableList<String> storedServersURIsImmutable = FXCollections.unmodifiableObservableList(storedServersURIs);
    private final static ObservableList<WebClient> clients = FXCollections.observableArrayList();
    private final static ObservableList<WebClient> clientsImmutable = FXCollections.unmodifiableObservableList(clients);

    private ConnectionsManagerModel() {
        throw new RuntimeException("This class is not instantiable.");
    }

    static {
        storedServersURIs.setAll(ClientsPreferencesManager.getURIs());
        ClientsPreferencesManager.getURIs().addListener((ListChangeListener<? super String>) change -> Platform.runLater(() ->
                storedServersURIs.setAll(change.getList())
        ));

        clients.setAll(WebClients.getClients());
        WebClients.getClients().addListener((ListChangeListener<? super WebClient>) change -> Platform.runLater(() ->
                clients.setAll(change.getList())
        ));
    }

    /**
     * See {@link ClientsPreferencesManager#getURIs()}.
     */
    public static ObservableList<String> getStoredServersURIs() {
        return storedServersURIsImmutable;
    }

    /**
     * See {@link WebClients#getClients()}.
     */
    public static ObservableList<WebClient> getClients() {
        return clientsImmutable;
    }
}
