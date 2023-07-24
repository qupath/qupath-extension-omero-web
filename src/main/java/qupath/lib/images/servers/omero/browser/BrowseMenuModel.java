package qupath.lib.images.servers.omero.browser;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import qupath.lib.images.servers.omero.common.api.clients.WebClient;
import qupath.lib.images.servers.omero.common.api.clients.WebClients;

/**
 * <p>
 *     The model of the browse menu. It contains a list which determines
 *     parts of the UI rendered by the browse menu.
 * </p>
 * <p>
 *     In effect, this class acts as an intermediate between a browser and a
 *     {@link qupath.lib.images.servers.omero.common.api.clients.WebClients WebClients}.
 *     Lists of a WebClients can be updated from any thread but the browse menu can
 *     only be accessed from the UI thread, so this class propagates changes made to these elements
 *     from any thread to the UI thread.
 * </p>
 * <p>This class is not instantiable as it only contains a static method.</p>
 */
class BrowseMenuModel {
    private final static ObservableList<WebClient> clients = FXCollections.observableArrayList();
    private final static ObservableList<WebClient> clientsImmutable = FXCollections.unmodifiableObservableList(clients);

    static {
        clients.setAll(WebClients.getClients());
        WebClients.getClients().addListener((ListChangeListener<? super WebClient>) change -> Platform.runLater(() -> {
            clients.setAll(change.getList());
        }));
    }

    private BrowseMenuModel() {
        throw new RuntimeException("This class is not instantiable.");
    }

    /**
     * See {@link WebClients#getClients()}.
     */
    public static ObservableList<WebClient> getClients() {
        return clientsImmutable;
    }
}
