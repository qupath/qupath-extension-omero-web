package qupath.lib.images.servers.omero.gui.connectionsmanager.connection;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import qupath.lib.images.servers.omero.web.WebClient;
import qupath.lib.images.servers.omero.gui.UiUtilities;

import java.net.URI;

/**
 * <p>
 *     The model of a connection. It contains properties and list which determine
 *     parts of the UI rendered by the connection.
 * </p>
 * <p>
 *     In effect, this class acts as an intermediate between a connection and a
 *     {@link WebClient WebClient}.
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
        UiUtilities.bindPropertyInUIThread(authenticated, client.getAuthenticated());
        UiUtilities.bindPropertyInUIThread(username, client.getUsername());

        UiUtilities.bindSetInUIThread(openedImagesURIs, client.getOpenedImagesURIs());
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
