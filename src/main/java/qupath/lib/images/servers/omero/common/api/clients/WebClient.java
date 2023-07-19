package qupath.lib.images.servers.omero.common.api.clients;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.dialogs.Dialogs;
import qupath.lib.gui.viewer.QuPathViewer;
import qupath.lib.images.servers.omero.common.api.requests.RequestsHandler;
import qupath.lib.images.servers.omero.common.api.requests.entities.login.LoginResponse;
import qupath.lib.images.servers.omero.common.gui.UiUtilities;
import qupath.lib.images.servers.omero.common.omeroentities.repositoryentities.OrphanedFolder;
import qupath.lib.images.servers.omero.common.omeroentities.repositoryentities.RepositoryEntity;
import qupath.lib.images.servers.omero.common.omeroentities.repositoryentities.Server;
import qupath.lib.images.servers.omero.common.omeroentities.repositoryentities.serverentities.Dataset;
import qupath.lib.images.servers.omero.common.omeroentities.repositoryentities.serverentities.Project;
import qupath.lib.images.servers.omero.common.omeroentities.repositoryentities.serverentities.image.Image;

import java.awt.image.BufferedImage;
import java.net.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * <p>Class representing an OMERO Web Client.</p>
 * <p>
 *     It handles creating a connection with an OMERO server, logging in, keeping the connection alive,
 *     logging out, and retrieving images (icons, thumbnails) from the server.
 * </p>
 * <p>
 *     A client can be connected to a server without being authenticated if the server allows it.
 * </p>
 * <p>
 *     It has a reference to a {@link qupath.lib.images.servers.omero.common.api.requests.RequestsHandler RequestsHandler}
 *     which can be used to retrieve information from the OMERO server,
 *     and a reference to a {@link qupath.lib.images.servers.omero.common.omeroentities.repositoryentities.Server Server}
 *     which is the ancestor of all OMERO entities.
 * </p>
 */
public class WebClient {
    private final static int THUMBNAIL_SIZE = 256;
    private final static long PING_DELAY_MILLISECONDS = 60000L;
    private final static ResourceBundle resources = UiUtilities.getResources();
    private final static Logger logger = LoggerFactory.getLogger(WebClient.class);
    private final StringProperty username = new SimpleStringProperty("");
    private final BooleanProperty authenticated = new SimpleBooleanProperty(false);
    private final ObservableSet<URI> openedImagesURIs = FXCollections.observableSet();
    private final ObservableSet<URI> openedImagesURIsImmutable = FXCollections.unmodifiableObservableSet(openedImagesURIs);
    private final Map<Integer, BufferedImage> thumbnails = new HashMap<>();
    private final Map<Class<? extends RepositoryEntity>, BufferedImage> omeroIcons = new HashMap<>();
    private final Server server = new Server();
    private RequestsHandler requestsHandler;
    private Timer timeoutTimer;
    private String sessionUUID;

    /**
     * <p>
     *     Static factory method creating a new client.
     *     It will initialize the connection and ask for credentials if this is required to access the server.
     * </p>
     * <p>
     *     This function should only be used by {@link qupath.lib.images.servers.omero.common.api.clients.WebClients WebClients}
     *     which monitors opened clients.
     * </p>
     * <p>This function is asynchronous.</p>
     *
     * @param uri  the server URI to connect to
     * @param args  optional arguments used to authenticate. See the {@link #login(String...) login()} function.
     * @return a CompletableFuture with the client if the connection is successful, or an empty Optional otherwise
     */
    static CompletableFuture<Optional<WebClient>> create(URI uri, String... args) {
        WebClient webClient = new WebClient();

        return webClient.initialize(uri, args).thenApply(success -> success ? Optional.of(webClient) : Optional.empty());
    }

    /**
     * @return the {@link qupath.lib.images.servers.omero.common.api.requests.RequestsHandler RequestsHandler} of this client
     */
    public RequestsHandler getRequestsHandler() {
        return requestsHandler;
    }

    /**
     * @return the {@link qupath.lib.images.servers.omero.common.omeroentities.repositoryentities.Server Server} of this client
     */
    public Server getServer() {
        return server;
    }

    /**
     * @return a read only property indicating if the client is currently authenticated.
     */
    public ReadOnlyBooleanProperty getAuthenticated() {
        return authenticated;
    }

    /**
     * @return a read only property indicating the username of the client if it is currently authenticated,
     * or an empty String else
     */
    public ReadOnlyStringProperty getUsername() {
        return username;
    }

    /**
     * @return the base URI of the server
     */
    public URI getServerURI() {
        return requestsHandler.getHost();
    }

    /**
     * <p>
     *     Returns a set of image URIs of this server which have been opened in this session.
     *     This class does not automatically detect if new images are opened, so this function
     *     actually only returns the URIs given to {@link #addOpenedImage(URI) addOpenedImage}.
     * </p>
     * <p>This function returns an unmodifiable list, use {@link #addOpenedImage(URI) addOpenedImage} to update its state.</p>
     *
     * @return a set of image URIs
     */
    public ObservableSet<URI> getOpenedImagesURIs() {
        return openedImagesURIsImmutable;
    }

    /**
     * Add an image URI to the list of currently opened images given by
     * {@link #getOpenedImagesURIs() getOpenedImagesURIs}.
     *
     * @param imageURI  the image URI
     */
    public void addOpenedImage(URI imageURI) {
        openedImagesURIs.add(imageURI);
    }

    /**
     * @return the session UUID of this connection
     */
    public String getSessionUUID() {
        return sessionUUID;
    }

    /**
     * <p>Attempt to authenticate to the server using the optional arguments.</p>
     * <p>
     *     Take a look at the {@link qupath.lib.images.servers.omero.common.api.requests.apis.JsonApi#login(String...) JsonApi.login()}
     *     function to know the accepted format for the arguments.
     * </p>
     * <p>
     *     If a username or a password cannot be retrieved from the arguments, a window (if the GUI is used)
     *     or the command line (else) will be used to ask the user for credentials.
     * </p>
     * <p>This function is asynchronous.</p>
     *
     * @param args  the optional arguments containing username and password information
     * @return a CompletableFuture indicating if the authentication was successful
     */
    public CompletableFuture<Boolean> login(String... args) {
        return requestsHandler.login(args).thenApply(loginResponse -> {
            if (loginResponse.isLoginSuccessful()) {
                Platform.runLater(() -> {
                    authenticated.set(true);
                    setAuthenticationInformation(loginResponse);

                    Dialogs.showInfoNotification(
                            resources.getString("Common.Api.Clients.login"),
                            MessageFormat.format(resources.getString("Common.Api.Clients.loginSuccessful"), requestsHandler.getHost(), username.get())
                    );
                    startTimer();
                });
            }

            return loginResponse.isLoginSuccessful();
        });
    }

    /**
     * <p>Logout from the server.</p>
     * <p>
     *     This function should only be used by {@link qupath.lib.images.servers.omero.common.api.clients.WebClients WebClients}
     *     which monitors opened clients.
     * </p>
     */
    void logout() {
        requestsHandler.logout();

        authenticated.set(false);
        username.set("");
        stopTimer();
    }

    /**
     * <p>Attempt to retrieve the thumbnail of an image from its id.</p>
     * <p>This function is asynchronous.</p>
     *
     * @param id  the id of the image whose thumbnail is to be retrieved
     * @return a CompletableFuture with the thumbnail if the operation succeeded, or an empty Optional otherwise
     */
    public CompletableFuture<Optional<BufferedImage>> getThumbnail(int id) {
        if (thumbnails.containsKey(id)) {
            return CompletableFuture.completedFuture(Optional.of(thumbnails.get(id)));
        } else {
            return requestsHandler.getThumbnail(id, THUMBNAIL_SIZE).thenApply(thumbnail -> {
                Platform.runLater(() -> thumbnail.ifPresent(bufferedImage -> thumbnails.put(id, bufferedImage)));
                return thumbnail;
            });
        }
    }

    /**
     * <p>Attempt to retrieve the icon of an OMERO entity.</p>
     * <p>This function is asynchronous.</p>
     *
     * @param type  the class of the entity whose icon is to be retrieved
     * @return a CompletableFuture with the icon if the operation succeeded, or an empty Optional otherwise
     */
    public Optional<BufferedImage> getOmeroIcon(Class<? extends RepositoryEntity> type) {
        if (omeroIcons.containsKey(type)) {
            return Optional.of(omeroIcons.get(type));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Indicates if this client can be closed, by checking if there is any
     * opened image in the QuPath viewer that belongs to the client.
     *
     * @return whether this client can be closed
     */
    public boolean canBeClosed() {
        return QuPathGUI
                .getInstance()
                .getViewers()
                .stream()
                .map(QuPathViewer::getServer)
                .filter(Objects::nonNull)
                .map(server -> server.getURIs().iterator().next())
                .noneMatch(openedImagesURIs::contains);
    }

    private CompletableFuture<Boolean> initialize(URI uri, String... args) {
        return RequestsHandler.create(uri).thenApplyAsync(requestsHandler -> {
            if (requestsHandler.isPresent()) {
                this.requestsHandler = requestsHandler.get();
                try {
                    if (requestsHandler.get().canSkipAuthentication().join()) {
                        return true;
                    } else {
                        return login(args).join();
                    }
                } catch (Exception e) {
                    logger.error("Error initializing client", e);
                    return false;
                }
            } else {
                return false;
            }
        }).thenApply(loggedIn -> {
            if (loggedIn) {
                Platform.runLater(() -> {
                    populateIcons();
                    server.initialize(requestsHandler);
                });
            }

            return loggedIn;
        });
    }

    private void setAuthenticationInformation(LoginResponse loginResponse) {
        server.setDefaultGroup(loginResponse.getGroup());
        server.setDefaultOwnerId(loginResponse.getUserId());

        sessionUUID = loginResponse.getSessionUUID();
        username.set(loginResponse.getUsername());
    }

    private void startTimer() {
        if (timeoutTimer == null) {
            timeoutTimer = new Timer("omero-keep-alive", true);
            timeoutTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    requestsHandler.ping().thenAccept(success -> {
                        if (!success) {
                            Platform.runLater(() -> logout());
                        }
                    });
                }
            }, PING_DELAY_MILLISECONDS, PING_DELAY_MILLISECONDS);
        }
    }

    private void stopTimer() {
        timeoutTimer.cancel();
        timeoutTimer = null;
    }

    private void populateIcons() {
        requestsHandler.getProjectIcon().thenAccept(icon -> Platform.runLater(() -> icon.ifPresent(bufferedImage -> omeroIcons.put(Project.class, bufferedImage))));
        requestsHandler.getDatasetIcon().thenAccept(icon -> Platform.runLater(() -> icon.ifPresent(bufferedImage -> omeroIcons.put(Dataset.class, bufferedImage))));
        requestsHandler.getOrphanedFolderIcon().thenAccept(icon -> Platform.runLater(() -> icon.ifPresent(bufferedImage -> omeroIcons.put(OrphanedFolder.class, bufferedImage))));
        requestsHandler.getImageIcon().thenAccept(icon -> Platform.runLater(() -> icon.ifPresent(bufferedImage -> omeroIcons.put(Image.class, bufferedImage))));
    }
}
