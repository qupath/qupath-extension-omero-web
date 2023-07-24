package qupath.lib.images.servers.omero.connectionsmanager.connectionsmanager.connection;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import qupath.lib.images.servers.omero.common.api.clients.WebClient;

import java.net.URI;

/**
 * <p>
 *     The model of a connection. It contains properties and list which determine
 *     parts of the UI rendered by the connection.
 * </p>
 * <p>
 *     In effect, this class acts as an intermediate between a connection and a
 *     {@link qupath.lib.images.servers.omero.common.api.clients.WebClient WebClient}.
 *     Properties and lists of a WebClient can be updated from any thread but the connection can
 *     only be accessed from the UI thread, so this class propagates changes made to these elements
 *     from any thread to the UI thread.
 * </p>
 */
class ConnectionModel {
    private final BooleanProperty authenticated = new SimpleBooleanProperty();
    private final StringProperty username = new SimpleStringProperty();
    private final ObservableSet<URI> openedImagesURIs = FXCollections.observableSet();
    private final ObservableSet<URI> openedImagesURIsImmutable = FXCollections.unmodifiableObservableSet(openedImagesURIs);

    public ConnectionModel(WebClient client) {
        authenticated.set(client.getAuthenticated().get());
        client.getAuthenticated().addListener((p, o, n) -> Platform.runLater(() -> authenticated.set(n)));

        username.set(client.getUsername().get());
        client.getUsername().addListener((p, o, n) -> Platform.runLater(() -> username.set(n)));

        openedImagesURIs.addAll(client.getOpenedImagesURIs());
        client.getOpenedImagesURIs().addListener((SetChangeListener<? super URI>) change -> Platform.runLater(() -> {
            if (change.wasAdded()) {
                openedImagesURIs.add(change.getElementAdded());
            }
            if (change.wasRemoved()) {
                openedImagesURIs.remove(change.getElementRemoved());
            }
        }));
    }

    /**
     * See {@link WebClient#getAuthenticated()}.
     */
    public ReadOnlyBooleanProperty getAuthenticated() {
        return authenticated;
    }

    /**
     * See {@link WebClient#getUsername()}.
     */
    public ReadOnlyStringProperty getUsername() {
        return username;
    }

    /**
     * See {@link WebClient#getOpenedImagesURIs()}.
     */
    public ObservableSet<URI> getOpenedImagesURIs() {
        return openedImagesURIsImmutable;
    }
}
