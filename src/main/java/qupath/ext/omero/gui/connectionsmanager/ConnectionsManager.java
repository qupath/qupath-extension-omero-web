package qupath.ext.omero.gui.connectionsmanager;

import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.omero.core.WebClient;
import qupath.ext.omero.gui.UiUtilities;
import qupath.ext.omero.gui.connectionsmanager.connection.Connection;

import java.io.IOException;
import java.util.ResourceBundle;

/**
 * <p>
 *     The connection manager provides a window that displays the connections to all servers.
 *     The user can connect, log in, log out, and remove a connection to a server.
 * </p>
 * <p>
 *     Each connexion is displayed using the
 *     {@link qupath.ext.omero.gui.connectionsmanager.connection connection} package.
 * </p>
 * <p>
 *     This class uses a {@link ConnectionsManagerModel} to update its state.
 * </p>
 */
public class ConnectionsManager extends Stage {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionsManager.class);
    private static final ResourceBundle resources = UiUtilities.getResources();
    @FXML
    private VBox container;

    /**
     * Creates the connection manager window.
     *
     * @param owner  the stage that should own this window
     * @throws IOException if an error occurs while creating the window
     */
    public ConnectionsManager(Stage owner) throws IOException {
        initUI(owner);
        setUpListeners();
    }

    private void initUI(Stage owner) throws IOException {
        UiUtilities.loadFXML(this, ConnectionsManager.class.getResource("connections_manager.fxml"));

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

        for (WebClient webClient: ConnectionsManagerModel.getClients()) {
            try {
                container.getChildren().add(new Connection(webClient));
            } catch (IOException e) {
                logger.error("Error while creating connection pane", e);
            }
        }

        for (String serverURI: ConnectionsManagerModel.getStoredServersURIs()) {
            if (!clientWithURIExists(serverURI)) {
                try {
                    container.getChildren().add(new Connection(serverURI));
                } catch (IOException e) {
                    logger.error("Error while creating connection pane", e);
                }
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
