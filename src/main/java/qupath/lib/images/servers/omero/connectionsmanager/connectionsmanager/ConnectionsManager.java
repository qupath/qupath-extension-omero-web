package qupath.lib.images.servers.omero.connectionsmanager.connectionsmanager;

import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import qupath.lib.images.servers.omero.common.api.clients.ClientsPreferencesManager;
import qupath.lib.images.servers.omero.common.api.clients.WebClient;
import qupath.lib.images.servers.omero.common.api.clients.WebClients;
import qupath.lib.images.servers.omero.common.gui.UiUtilities;

import java.util.ResourceBundle;

/**
 * <p>
 *     The connection manager provides a window that displays the connections to all servers.
 *     The user can connect, log in, log out, and remove a connection to a server.
 * </p>
 * <p>
 *     Each connexion is displayed using the
 *     {@link qupath.lib.images.servers.omero.connectionsmanager.connectionsmanager.Connection Connection} pane.
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
        WebClients.getClients().addListener((ListChangeListener<? super WebClient>) change -> populate());
        ClientsPreferencesManager.getURIs().addListener((ListChangeListener<? super String>) change -> populate());
    }

    private void populate() {
        container.getChildren().clear();

        for (String serverURI: ClientsPreferencesManager.getURIs()) {
            if (!clientWithURIExists(serverURI)) {
                container.getChildren().add(new Connection(serverURI));
            }
        }

        container.getChildren().addAll(WebClients.getClients().stream()
                .map(Connection::new)
                .toList());

        if (container.getChildren().size() == 0) {
            container.getChildren().add(new Label(resources.getString("ConnectionsManager.ConnectionManager.noClients")));
        }
    }

    private static boolean clientWithURIExists(String uri) {
        return WebClients.getClients().stream().anyMatch(client -> client.getServerURI().toString().equals(uri));
    }
}
