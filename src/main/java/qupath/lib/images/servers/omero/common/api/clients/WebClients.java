package qupath.lib.images.servers.omero.common.api.clients;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import qupath.lib.images.servers.omero.common.api.requests.RequestsUtilities;
import qupath.lib.images.servers.omero.common.api.authenticators.commandline.CommandLineAuthenticator;
import qupath.lib.images.servers.omero.common.api.authenticators.gui.GuiAuthenticator;
import qupath.lib.images.servers.omero.common.gui.UiUtilities;

import java.net.Authenticator;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Utility classes that monitors all opened clients.
 */
public class WebClients {
    final private static ObservableList<WebClient> clients = FXCollections.observableArrayList();
    final private static ObservableList<WebClient> clientsImmutable = FXCollections.unmodifiableObservableList(clients);
    private static Authenticator authenticator;

    private WebClients() {
        throw new AssertionError("This class is not instantiable.");
    }

    /**
     * <p>
     *     Create a WebClient from the server URI provided. Basically, this function will
     *     call {@link qupath.lib.images.servers.omero.common.api.clients.WebClient#create(URI, String...) WebClient.create()}
     *     and internally stores the newly created client.
     * </p>
     * <p>This function is asynchronous.</p>
     *
     * @param url  the URL of the server
     * @param args  optional arguments to login. See {@link qupath.lib.images.servers.omero.common.api.clients.WebClient#create(URI, String...) WebClient.create()}
     * @return a CompletableFuture with the client if the connection is successful, or an empty Optional otherwise
     */
    public static CompletableFuture<Optional<WebClient>> createClient(String url, String... args) {
        var uri = RequestsUtilities.createURI(url);

        if (uri.isPresent()) {
            var host = RequestsUtilities.getServerURI(uri.get());

            if (host.isPresent()) {
                var existingClient = clients.stream().filter(e -> e.getServerURI().equals(host.get())).findAny();

                if (existingClient.isEmpty()) {
                    return WebClient.create(host.get(), args).thenApply(client -> {
                        Platform.runLater(() -> {
                            if (client.isPresent()) {
                                ClientsPreferencesManager.addURI(client.get().getServerURI().toString());
                                clients.add(client.get());
                            }
                        });
                        return client;
                    });
                } else {
                    return CompletableFuture.completedFuture(existingClient);
                }
            } else {
                return CompletableFuture.completedFuture(Optional.empty());
            }
        } else {
            return CompletableFuture.completedFuture(Optional.empty());
        }
    }

    /**
     * Remove a client. This will close the connection and forget
     * all information about this client.
     *
     * @param client  the client to remove
     */
    public static void deleteClient(WebClient client) {
        ClientsPreferencesManager.removeURI(client.getServerURI().toString());
        clients.remove(client);
    }

    /**
     * Logout the client and close its connection.
     *
     * @param client  the client to disconnect
     */
    public static void logoutClient(WebClient client) {
        client.logout();
        clients.remove(client);
    }

    /**
     * Returns an unmodifiable list of all connected (but not necessarily authenticated) clients.
     *
     * @return the connected clients
     */
    public static ObservableList<WebClient> getClients() {
        return clientsImmutable;
    }

    /**
     * Get the {@link java.net.Authenticator Authenticator} to ask the user for credentials.
     * If the GUI is used, this is a window, otherwise the command line is used.
     *
     * @return the authenticator used to log in
     */
    public static Authenticator getAuthenticator() {
        if (authenticator == null) {
            authenticator = UiUtilities.usingGUI() ? new GuiAuthenticator() : new CommandLineAuthenticator();
        }
        return authenticator;
    }
}
