package qupath.lib.images.servers.omero.connections_manager.connections_manager;

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

    public Connection(WebClient client) {
        this(client, client.getServerURI().toString());
    }

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
