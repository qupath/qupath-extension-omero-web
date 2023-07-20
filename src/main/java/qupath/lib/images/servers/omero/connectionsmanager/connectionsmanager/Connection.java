package qupath.lib.images.servers.omero.connectionsmanager.connectionsmanager;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.SetChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import qupath.lib.gui.dialogs.Dialogs;
import qupath.lib.images.servers.omero.common.api.clients.ClientsPreferencesManager;
import qupath.lib.images.servers.omero.common.api.clients.WebClient;
import qupath.lib.images.servers.omero.common.api.clients.WebClients;
import qupath.lib.images.servers.omero.common.gui.UiUtilities;

import java.net.URI;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * <p>
 *     Pane displaying the status of a connection to a server as well as buttons to
 *     connect, log in, log out, or remove the connection.
 * </p>
 * <p>
 *     It also displays the list of images of this server that are currently opened using the
 *     {@link qupath.lib.images.servers.omero.connectionsmanager.connectionsmanager.Image Image} label.
 * </p>
 */
class Connection extends VBox {
    private final WebClient client;
    private final String serverURI;
    private static ResourceBundle resources;
    @FXML
    private Label uri;
    @FXML
    private TitledPane imagesPane;
    @FXML
    private VBox imagesContainer;
    @FXML
    private Button connection;

    /**
     * Creates the connection pane using a {@link qupath.lib.images.servers.omero.common.api.clients.WebClient WebClient}.
     * Since a WebClient is present, there is already a connection with the server, so the user will have the possibility
     * to log in, log out, or remove the connection, but not to connect to the server.
     *
     * @param client  the client corresponding to the connection with the server
     */
    public Connection(WebClient client) {
        this(client, client.getServerURI().toString());
    }

    /**
     * Creates the connection pane using the URI of a server.
     * Since there is no WebClient, there is no connection with the server for now, so the user will have the possibility
     * to connect to the server or to remove the connection, but not to log in or log out.
     * Logging in or logging out will only be possible after a connection to the server is made.
     *
     * @param serverURI  the URI of the server
     */
    public Connection(String serverURI) {
        this(null, serverURI);
    }

    private Connection(WebClient client, String serverURI) {
        this.client = client;
        this.serverURI = serverURI;

        initUI();
        setUpListeners();
    }

    @FXML
    private void onConnectionClicked(ActionEvent ignoredEvent) {
        if (client == null) {
            WebClients.createClient(serverURI).thenAccept(newClient -> Platform.runLater(() -> {
                if (newClient.isEmpty()) {
                    Dialogs.showErrorMessage(
                            resources.getString("ConnectionsManager.Connection.webServer"),
                            MessageFormat.format(resources.getString("ConnectionsManager.Connection.connectionFailed"), serverURI)
                    );
                } else {
                    Dialogs.showInfoNotification(
                            resources.getString("ConnectionsManager.Connection.webServer"),
                            MessageFormat.format(resources.getString("ConnectionsManager.Connection.connectedTo"), serverURI)
                    );
                }
            }));
        } else {
            if (client.getAuthenticated().get()) {
                if (client.canBeClosed()) {
                    boolean logOutConfirmed = Dialogs.showConfirmDialog(
                            resources.getString("ConnectionsManager.Connection.logout"),
                            resources.getString("ConnectionsManager.Connection.logoutConfirmation")
                    );

                    if (logOutConfirmed) {
                        WebClients.logoutClient(client);
                    }
                } else {
                    Dialogs.showMessageDialog(
                            resources.getString("ConnectionsManager.Connection.removeClient"),
                            resources.getString("ConnectionsManager.Connection.closeImages")
                    );
                }
            } else {
                client.login().thenAccept(loggedIn -> Platform.runLater(() -> {
                    if (!loggedIn) {
                        Dialogs.showErrorMessage(
                                resources.getString("ConnectionsManager.Connection.loginToServer"),
                                resources.getString("ConnectionsManager.Connection.loginFailed")
                        );
                    }
                }));
            }
        }
    }

    @FXML
    private void onRemoveClicked(ActionEvent ignoredEvent) {
        if (client == null) {
            ClientsPreferencesManager.removeURI(serverURI);
        } else {
            if (client.canBeClosed()) {
                boolean deletionConfirmed = Dialogs.showConfirmDialog(
                        resources.getString("ConnectionsManager.Connection.removeClient"),
                        resources.getString("ConnectionsManager.Connection.removeClientConfirmation")
                );

                if (deletionConfirmed) {
                    WebClients.deleteClient(client);
                }
            } else {
                Dialogs.showMessageDialog(
                        resources.getString("ConnectionsManager.Connection.removeClient"),
                        resources.getString("ConnectionsManager.Connection.closeImages")
                );
            }
        }
    }

    private void initUI() {
        resources = UiUtilities.loadFXMLAndGetResources(this, getClass().getResource("connection.fxml"));

        uri.setText(serverURI);
        uri.setGraphic(UiUtilities.createStateNode(client != null));

        if (client != null) {
            for (URI uri: client.getOpenedImagesURIs()) {
                imagesContainer.getChildren().add(new Image(uri));
            }
        }
    }

    private void setUpListeners() {
        if (client != null) {
            uri.textProperty().bind(Bindings.when(client.getAuthenticated())
                    .then(Bindings.concat(serverURI, " (", client.getUsername(), ")"))
                    .otherwise(serverURI)
            );

            imagesPane.textProperty().bind(Bindings.concat(
                    Bindings.size(client.getOpenedImagesURIs()),
                    " ",
                    Bindings.when(Bindings.size(client.getOpenedImagesURIs()).greaterThan(1))
                            .then(resources.getString("ConnectionsManager.Connection.images"))
                            .otherwise(resources.getString("ConnectionsManager.Connection.image"))
            ));

            client.getOpenedImagesURIs().addListener((SetChangeListener<? super URI>) change -> {
                imagesContainer.getChildren().clear();

                for (URI uri: change.getSet()) {
                    imagesContainer.getChildren().add(new Image(uri));
                }
            });

            connection.textProperty().bind(Bindings.when(client.getAuthenticated())
                    .then(resources.getString("ConnectionsManager.Connection.logout"))
                    .otherwise(resources.getString("ConnectionsManager.Connection.login"))
            );
        }

    }
}