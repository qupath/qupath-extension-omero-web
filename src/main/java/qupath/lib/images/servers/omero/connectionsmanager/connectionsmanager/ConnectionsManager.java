package qupath.lib.images.servers.omero.connectionsmanager.connectionsmanager;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import qupath.lib.images.servers.omero.common.api.clients.WebClient;
import qupath.lib.images.servers.omero.common.gui.UiUtilities;
import qupath.lib.images.servers.omero.connectionsmanager.connectionsmanager.connection.Connection;

import java.util.ResourceBundle;

/**
 * <p>
 *     The connection manager provides a window that displays the connections to all servers.
 *     The user can connect, log in, log out, and remove a connection to a server.
 * </p>
 * <p>
 *     Each connexion is displayed using the
 *     {@link qupath.lib.images.servers.omero.connectionsmanager.connectionsmanager.connection connection} package.
 * </p>
 * <p>
 *     This class uses a {@link ConnectionsManagerModel} to update its state.
 * </p>
 */
public class ConnectionsManager extends Stage {
    private static ResourceBundle resources;
    @FXML
    private VBox container;

    /**
     * Creates the connection manager window.
     *
     * @param owner  the stage that should own this window
     */
    public ConnectionsManager(Stage owner) {
        initUI(owner);
        setUpListeners();
    }

    private void initUI(Stage owner) {
        resources = UiUtilities.loadFXMLAndGetResources(this, getClass().getResource("connections_manager.fxml"));

        if (owner != null) {
            initOwner(owner);
        }
        populate();
        show();
    }

    private void setUpListeners() {
        ConnectionsManagerModel.getClients().addListener((ListChangeListener<? super WebClient>) change -> populate());
        ConnectionsManagerModel.getStoredServersURIs().addListener((ListChangeListener<? super String>) change -> populate());
    }

    private void populate() {
        container.getChildren().clear();

        container.getChildren().addAll(ConnectionsManagerModel.getClients().stream()
                .map(Connection::new)
                .toList());

        for (String serverURI: ConnectionsManagerModel.getStoredServersURIs()) {
            if (!clientWithURIExists(serverURI)) {
                container.getChildren().add(new Connection(serverURI));
            }
        }

        if (container.getChildren().size() == 0) {
            container.getChildren().add(new Label(resources.getString("ConnectionsManager.ConnectionManager.noClients")));
        }
    }

    private static boolean clientWithURIExists(String uri) {
        return ConnectionsManagerModel.getClients().stream().anyMatch(client -> client.getServerURI().toString().equals(uri));
    }
}
