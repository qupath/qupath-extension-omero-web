package qupath.ext.omero.gui.connectionsmanager.connection;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.SetChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.omero.OmeroExtension;
import qupath.ext.omero.core.ClientsPreferencesManager;
import qupath.ext.omero.core.WebClient;
import qupath.ext.omero.core.WebClients;
import qupath.ext.omero.core.entities.login.LoginResponse;
import qupath.ext.omero.gui.UiUtilities;
import qupath.fx.dialogs.Dialogs;

import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * <p>
 *     Pane displaying the status of a connection to a server as well as buttons to
 *     connect, log in, log out, or remove the connection.
 * </p>
 * <p>
 *     It also displays the list of images of this server that are currently opened using the
 *     {@link Image Image} label.
 * </p>
 * <p>
 *     This class uses a {@link ConnectionModel} to update its state.
 * </p>
 */
public class Connection extends VBox {

    private static final Logger logger = LoggerFactory.getLogger(Connection.class);
    private static final ResourceBundle resources = UiUtilities.getResources();
    private final WebClient client;
    private final String serverURI;
    private final ConnectionModel connectionModel;
    @FXML
    private Label uri;
    @FXML
    private TitledPane imagesPane;
    @FXML
    private VBox imagesContainer;
    @FXML
    private Button browse;
    @FXML
    private Button connection;
    @FXML
    private Button remove;

    /**
     * Creates the connection pane using a {@link WebClient WebClient}.
     * Since a WebClient is present, there is already a connection with the server, so the user will have the possibility
     * to log in, log out, or remove the connection, but not to connect to the server.
     *
     * @param client  the client corresponding to the connection with the server
     * @throws IOException if an error occurs while creating the pane
     */
    public Connection(WebClient client) throws IOException {
        this(client, client.getApisHandler().getWebServerURI().toString());
    }

    /**
     * Creates the connection pane using the URI of a server.
     * Since there is no WebClient, there is no connection with the server for now, so the user will have the possibility
     * to connect to the server or to remove the connection, but not to log in or log out.
     * Logging in or logging out will only be possible after a connection to the server is made.
     *
     * @param serverURI  the URI of the server
     * @throws IOException if an error occurs while creating the pane
     */
    public Connection(String serverURI) throws IOException {
        this(null, serverURI);
    }

    private Connection(WebClient client, String serverURI) throws IOException {
        this.client = client;
        this.serverURI = serverURI;

        if (client == null) {
            connectionModel = null;
        } else {
            connectionModel = new ConnectionModel(client);
        }

        initUI();
        setUpListeners();
    }

    @FXML
    private void onBrowseClicked(ActionEvent ignoredEvent) {
        OmeroExtension.getBrowseMenu().openBrowserOfClient(client);
    }

    @FXML
    private void onConnectionClicked(ActionEvent ignoredEvent) {
        if (client == null) {
            WebClients.createClient(serverURI, true).thenAccept(newClient -> Platform.runLater(() -> {
                if (newClient.getStatus().equals(WebClient.Status.SUCCESS)) {
                    Dialogs.showInfoNotification(
                            resources.getString("ConnectionsManager.Connection.webServer"),
                            MessageFormat.format(resources.getString("ConnectionsManager.Connection.connectedTo"), serverURI)
                    );
                } else if (newClient.getStatus().equals(WebClient.Status.FAILED)) {
                    Optional<WebClient.FailReason> failReason = newClient.getFailReason();
                    String message = null;

                    if (failReason.isPresent()) {
                        if (failReason.get().equals(WebClient.FailReason.INVALID_URI_FORMAT)) {
                            message = MessageFormat.format(resources.getString("ConnectionsManager.Connection.invalidURI"), serverURI);
                        } else if (failReason.get().equals(WebClient.FailReason.ALREADY_CREATING)) {
                            message = MessageFormat.format(resources.getString("ConnectionsManager.Connection.alreadyCreating"), serverURI);
                        }
                    } else {
                        message = MessageFormat.format(resources.getString("ConnectionsManager.Connection.connectionFailed"), serverURI);
                    }

                    if (message != null) {
                        Dialogs.showErrorMessage(
                                resources.getString("ConnectionsManager.Connection.webServer"),
                                message
                        );
                    }
                }
            }));
        } else {
            if (connectionModel.getAuthenticated().get()) {
                if (client.canBeClosed()) {
                    boolean logOutConfirmed = Dialogs.showConfirmDialog(
                            resources.getString("ConnectionsManager.Connection.logout"),
                            resources.getString("ConnectionsManager.Connection.logoutConfirmation")
                    );

                    if (logOutConfirmed) {
                        WebClients.removeClient(client);
                    }
                } else {
                    Dialogs.showMessageDialog(
                            resources.getString("ConnectionsManager.Connection.removeClient"),
                            resources.getString("ConnectionsManager.Connection.closeImages")
                    );
                }
            } else {
                client.login().thenAccept(loginResponse -> Platform.runLater(() -> {
                    if (loginResponse.getStatus().equals(LoginResponse.Status.SUCCESS)) {
                        Dialogs.showInfoNotification(
                                resources.getString("ConnectionsManager.Connection.login"),
                                MessageFormat.format(
                                        resources.getString("ConnectionsManager.Connection.loginSuccessful"),
                                        client.getApisHandler().getWebServerURI(),
                                        client.getUsername().get()
                                )
                        );
                    } else if (loginResponse.getStatus().equals(LoginResponse.Status.FAILED)) {
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
            boolean deletionConfirmed = Dialogs.showConfirmDialog(
                    resources.getString("ConnectionsManager.Connection.removeClient"),
                    resources.getString("ConnectionsManager.Connection.removeClientConfirmation")
            );

            if (deletionConfirmed) {
                ClientsPreferencesManager.removeURI(serverURI);
            }
        } else {
            if (client.canBeClosed()) {
                boolean deletionConfirmed = Dialogs.showConfirmDialog(
                        resources.getString("ConnectionsManager.Connection.disconnectClient"),
                        resources.getString("ConnectionsManager.Connection.disconnectClientConfirmation")
                );

                if (deletionConfirmed) {
                    WebClients.removeClient(client);
                }
            } else {
                Dialogs.showMessageDialog(
                        resources.getString("ConnectionsManager.Connection.removeClient"),
                        resources.getString("ConnectionsManager.Connection.closeImages")
                );
            }
        }
    }

    private void initUI() throws IOException {
        UiUtilities.loadFXML(this, Connection.class.getResource("connection.fxml"));

        uri.setText(serverURI);
        uri.setGraphic(UiUtilities.createStateNode(client != null));

        if (client == null) {
            remove.setText(resources.getString("ConnectionsManager.Connection.removeClient"));
            browse.setDisable(true);
        } else {
            for (URI uri: connectionModel.getOpenedImagesURIs()) {
                imagesContainer.getChildren().add(new Image(client, uri));
            }

            remove.setText(resources.getString("ConnectionsManager.Connection.disconnect"));
        }
    }

    private void setUpListeners() {
        if (client != null) {
            uri.textProperty().bind(Bindings.when(connectionModel.getAuthenticated())
                    .then(Bindings.concat(serverURI, " (", connectionModel.getUsername(), ")"))
                    .otherwise(serverURI)
            );

            imagesPane.textProperty().bind(Bindings.concat(
                    Bindings.size(connectionModel.getOpenedImagesURIs()),
                    " ",
                    Bindings.when(Bindings.size(connectionModel.getOpenedImagesURIs()).greaterThan(1))
                            .then(resources.getString("ConnectionsManager.Connection.images"))
                            .otherwise(resources.getString("ConnectionsManager.Connection.image"))
            ));

            connectionModel.getOpenedImagesURIs().addListener((SetChangeListener<? super URI>) change -> {
                imagesContainer.getChildren().clear();

                for (URI uri: change.getSet()) {
                    try {
                        imagesContainer.getChildren().add(new Image(client, uri));
                    } catch (IOException e) {
                        logger.error("Error while creating image pane", e);
                    }
                }
            });

            connection.textProperty().bind(Bindings.when(connectionModel.getAuthenticated())
                    .then(resources.getString("ConnectionsManager.Connection.logout"))
                    .otherwise(resources.getString("ConnectionsManager.Connection.login"))
            );
        }
    }
}
