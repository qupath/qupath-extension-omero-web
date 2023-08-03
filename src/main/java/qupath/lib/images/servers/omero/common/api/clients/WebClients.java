package qupath.lib.images.servers.omero.common.api.clients;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import qupath.lib.images.servers.omero.common.api.requests.RequestsUtilities;

import java.net.URI;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Utility classes that monitors all active connections to servers.
 */
public class WebClients {
    private final static ObservableList<WebClient> clients = FXCollections.observableArrayList();
    private final static ObservableList<WebClient> clientsImmutable = FXCollections.unmodifiableObservableList(clients);

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
     * @param url  the URL of the server. It doesn't have to be the base URL of the server
     * @param args  optional arguments to login. See {@link qupath.lib.images.servers.omero.common.api.clients.WebClient#create(URI, String...) WebClient.create()}
     * @return a CompletableFuture with the client if the connection is successful, or an empty Optional otherwise
     */
    public static CompletableFuture<Optional<WebClient>> createClient(String url, String... args) {
        var serverURI = getServerURI(url);

        if (serverURI.isPresent()) {
            var existingClient = getExistingClient(serverURI.get());

            if (existingClient.isEmpty()) {
                return WebClient.create(serverURI.get(), args).thenApply(client -> {
                    if (client.isPresent()) {
                        ClientsPreferencesManager.addURI(client.get().getServerURI().toString());
                        updateClients(client.get(), true);
                    }
                    return client;
                });
            } else {
                return CompletableFuture.completedFuture(existingClient);
            }
        } else {
            return CompletableFuture.completedFuture(Optional.empty());
        }
    }

    /**
     * <p>
     *     Synchronous version of {@link #createClient(String, String...)} that calls
     *     {@link qupath.lib.images.servers.omero.common.api.clients.WebClient#createSync(URI, String...) WebClient.createSync()}.
     * </p>
     * <p>This function may block the calling thread for around a second.</p>
     */
    public static Optional<WebClient> createClientSync(String url, String... args) {
        var serverURI = getServerURI(url);

        if (serverURI.isPresent()) {
            var existingClient = getExistingClient(serverURI.get());

            if (existingClient.isEmpty()) {
                var client = WebClient.createSync(serverURI.get(), args);
                if (client.isPresent()) {
                    ClientsPreferencesManager.addURI(client.get().getServerURI().toString());
                    updateClients(client.get(), true);
                }

                return client;
            } else {
                return existingClient;
            }
        } else {
            return Optional.empty();
        }
    }

    /**
     * Logout the client and close its connection.
     *
     * @param client  the client to disconnect and remove
     */
    public static void logoutAndRemoveClient(WebClient client) {
        client.logout();
        updateClients(client, false);
    }

    /**
     * <p>Returns an unmodifiable list of all connected (but not necessarily authenticated) clients.</p>
     * <p>This list may be updated from any thread.</p>
     *
     * @return the connected clients
     */
    public static ObservableList<WebClient> getClients() {
        return clientsImmutable;
    }

    private static synchronized void updateClients(WebClient client, boolean add) {
        if (add) {
            clients.add(client);
        } else {
            clients.remove(client);
        }
    }

    private static Optional<URI> getServerURI(String url) {
        var uri = RequestsUtilities.createURI(url);
        if (uri.isPresent()) {
            return RequestsUtilities.getServerURI(uri.get());
        } else {
            return Optional.empty();
        }
    }

    private static Optional<WebClient> getExistingClient(URI uri) {
        return clients.stream().filter(e -> e.getServerURI().equals(uri)).findAny();
    }
}
