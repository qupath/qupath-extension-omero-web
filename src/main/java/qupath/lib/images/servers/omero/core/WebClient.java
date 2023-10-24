package qupath.lib.images.servers.omero.core;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.images.servers.omero.imagesserver.OmeroImageServer;
import qupath.lib.images.servers.omero.core.apis.ApisHandler;
import qupath.lib.images.servers.omero.core.entities.login.LoginResponse;
import qupath.lib.images.servers.omero.core.entities.repositoryentities.OrphanedFolder;
import qupath.lib.images.servers.omero.core.entities.repositoryentities.RepositoryEntity;
import qupath.lib.images.servers.omero.core.entities.repositoryentities.Server;
import qupath.lib.images.servers.omero.core.entities.repositoryentities.serverentities.Dataset;
import qupath.lib.images.servers.omero.core.entities.repositoryentities.serverentities.Project;
import qupath.lib.images.servers.omero.core.entities.repositoryentities.serverentities.image.Image;
import qupath.lib.images.servers.omero.core.pixelapis.ice.IceAPI;
import qupath.lib.images.servers.omero.core.pixelapis.PixelAPI;
import qupath.lib.images.servers.omero.core.pixelapis.web.WebAPI;

import java.awt.image.BufferedImage;
import java.net.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

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
 *     It has a reference to a {@link ApisHandler}
 *     which can be used to retrieve information from the OMERO server,
 *     and a reference to a {@link Server Server}
 *     which is the ancestor of all OMERO entities.
 * </p>
 * <p>A client must be {@link #close() closed} once no longer used.</p>
 */
public class WebClient implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(WebClient.class);
    private static final int THUMBNAIL_SIZE = 256;
    private static final long PING_DELAY_MILLISECONDS = 60000L;
    private final StringProperty username = new SimpleStringProperty("");
    private final BooleanProperty authenticated = new SimpleBooleanProperty(false);
    private final ObjectProperty<PixelAPI> selectedPixelAPI = new SimpleObjectProperty<>();
    private final ObservableSet<URI> openedImagesURIs = FXCollections.observableSet();
    private final ObservableSet<URI> openedImagesURIsImmutable = FXCollections.unmodifiableObservableSet(openedImagesURIs);
    private final Map<Long, BufferedImage> thumbnails = new ConcurrentHashMap<>();
    private final Map<Class<? extends RepositoryEntity>, BufferedImage> omeroIcons = new ConcurrentHashMap<>();
    private Server server;
    private List<PixelAPI> availablePixelAPIs;
    private ApisHandler apisHandler;
    private Timer timeoutTimer;
    private char[] password;
    private Status status;

    public enum Status {
        CANCELED,
        FAILED,
        SUCCESS
    }

    private WebClient() {}

    /**
     * <p>
     *     Static factory method creating a new client.
     *     It will initialize the connection and ask for credentials if this is required to access the server.
     * </p>
     * <p>
     *     This function should only be used by {@link WebClients WebClients}
     *     which monitors opened clients (see {@link WebClients#createClient(String, String...)}).
     * </p>
     * <p>
     *     Note that this function is not guaranteed to create a valid client. Call the
     *     {@link #getStatus()} function to check the validity of the returned client
     *     before using it.
     * </p>
     * <p>This function is asynchronous.</p>
     *
     * @param uri  the server URI to connect to
     * @param args  optional arguments used to authenticate. See the {@link #login(String...) login()} function.
     * @return a CompletableFuture with the client
     */
    static CompletableFuture<WebClient> create(URI uri, String... args) {
        return new WebClient().initialize(uri, args);
    }

    /**
     * <p>Synchronous version of {@link #create(URI, String...)}.</p>
     * <p>This function may block the calling thread for around a second.</p>
     */
    static WebClient createSync(URI uri, String... args) {
        WebClient webClient = new WebClient();
        webClient.initializeSync(uri, args);
        return webClient;
    }

    /**
     * <p>Creates an invalid client.</p>
     * <p>
     *     This function should only be used by {@link WebClients WebClients}
     *     which monitors opened clients.
     * </p>
     * @return an invalid client
     */
    static WebClient createInvalidClient() {
        WebClient webClient = new WebClient();
        webClient.status = Status.FAILED;
        return webClient;
    }

    @Override
    public void close() throws Exception {
        if (apisHandler != null) {
            apisHandler.close();
        }
        if (timeoutTimer != null) {
            timeoutTimer.cancel();
        }
    }

    @Override
    public String toString() {
        return String.format("""
                Web client of:
                    status: %s
                    username: %s
                    authenticated: %s
                    selectedPixelAPI: %s
                """, status, username.get().isEmpty() ? "no username" : username.get(), authenticated.get(), selectedPixelAPI.get());
    }

    /**
     * @return the status of the client
     */
    public Status getStatus() {
        return status;
    }

    /**
     * @return the {@link ApisHandler} of this client
     */
    public ApisHandler getApisHandler() {
        return apisHandler;
    }

    /**
     * @return the {@link Server Server} of this client
     */
    public Server getServer() {
        return server;
    }

    /**
     * @return a read only property indicating if the client is currently authenticated.
     * This property may be updated from any thread
     */
    public ReadOnlyBooleanProperty getAuthenticated() {
        return authenticated;
    }

    /**
     * @return a read only property indicating the username of the client if it is currently authenticated,
     * or an empty String else. This property may be updated from any thread
     */
    public ReadOnlyStringProperty getUsername() {
        return username;
    }

    /**
     * @return the base URI of the server
     */
    public URI getServerURI() {
        return apisHandler.getHost();
    }

    public ObjectProperty<PixelAPI> getSelectedPixelAPI() {
        return selectedPixelAPI;
    }

    public List<PixelAPI> getAvailablePixelAPIs() {
        return availablePixelAPIs;
    }

    /**
     * <p>
     *     Returns a set of image URIs of this server which have been opened in this session.
     *     This class does not automatically detect if new images are opened, so this function
     *     actually only returns the URIs given to {@link #addOpenedImage(URI) addOpenedImage}.
     * </p>
     * <p>This function returns an unmodifiable list, use {@link #addOpenedImage(URI) addOpenedImage} to update its state.</p>
     * <p>This list may be updated from any thread.</p>
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
    public synchronized void addOpenedImage(URI imageURI) {
        openedImagesURIs.add(imageURI);
    }

    /**
     * @return the password of the authenticated user, or an empty Optional if
     * there is no authentication
     */
    public Optional<char[]> getPassword() {
        return Optional.ofNullable(password);
    }

    /**
     * @return the server port of this session
     */
    public int getPort() {
        return apisHandler.getPort();
    }

    /**
     * <p>Attempt to authenticate to the server using the optional arguments.</p>
     * <p>
     *     Take a look at the {@link ApisHandler#login(String...) JsonApi.login()}
     *     function to know the accepted format for the arguments.
     * </p>
     * <p>
     *     If a username or a password cannot be retrieved from the arguments, a window (if the GUI is used)
     *     or the command line (else) will be used to ask the user for credentials.
     * </p>
     * <p>This function is asynchronous.</p>
     *
     * @param args  the optional arguments containing username and password information
     * @return a CompletableFuture with the login response
     */
    public CompletableFuture<LoginResponse> login(String... args) {
        return apisHandler.login(args).thenApply(loginResponse -> {
            if (loginResponse.getStatus().equals(LoginResponse.Status.SUCCESS)) {
                setAuthenticationInformation(loginResponse);
                startTimer();
            }

            return loginResponse;
        });
    }

    /**
     * <p>Attempt to retrieve the thumbnail of an image from its id.</p>
     * <p>This function is asynchronous.</p>
     *
     * @param id  the id of the image whose thumbnail is to be retrieved
     * @return a CompletableFuture with the thumbnail if the operation succeeded, or an empty Optional otherwise
     */
    public CompletableFuture<Optional<BufferedImage>> getThumbnail(long id) {
        if (thumbnails.containsKey(id)) {
            return CompletableFuture.completedFuture(Optional.of(thumbnails.get(id)));
        } else {
            return apisHandler.getThumbnail(id, THUMBNAIL_SIZE).thenApply(thumbnail -> {
                thumbnail.ifPresent(bufferedImage -> thumbnails.put(id, bufferedImage));
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
     * opened image in the QuPath viewer that belongs to this client.
     *
     * @return whether this client can be closed
     */
    public boolean canBeClosed() {
        return !(QuPathGUI.getInstance().getViewer().getServer() instanceof OmeroImageServer omeroImageServer &&
                omeroImageServer.getClient().equals(this));
    }

    private CompletableFuture<WebClient> initialize(URI uri, String... args) {
        return ApisHandler.create(this, uri).thenApplyAsync(apisHandler -> {
            if (apisHandler.isPresent()) {
                this.apisHandler = apisHandler.get();
                try {
                    if (this.apisHandler.canSkipAuthentication().get()) {
                        return LoginResponse.createNonSuccessfulLoginResponse(LoginResponse.Status.UNAUTHENTICATED);
                    } else {
                        return login(args).get();
                    }
                } catch (ExecutionException | InterruptedException e) {
                    logger.error("Error initializing client", e);
                    return LoginResponse.createNonSuccessfulLoginResponse(LoginResponse.Status.FAILED);
                }
            } else {
                return LoginResponse.createNonSuccessfulLoginResponse(LoginResponse.Status.FAILED);
            }
        }).thenApplyAsync(loginResponse -> {
            LoginResponse.Status status = loginResponse.getStatus();
            if (status.equals(LoginResponse.Status.SUCCESS) || status.equals(LoginResponse.Status.UNAUTHENTICATED)) {
                try {
                    var server = status.equals(LoginResponse.Status.SUCCESS) ?
                            Server.create(apisHandler, loginResponse.getGroup(), loginResponse.getUserId()).get() :
                            Server.create(apisHandler).get();

                    if (server.isPresent()) {
                        this.server = server.get();
                    } else {
                        status = LoginResponse.Status.FAILED;
                    }
                } catch (InterruptedException | ExecutionException e) {
                    logger.error("Error initializing client", e);
                    status = LoginResponse.Status.FAILED;
                }
            }
            if (status.equals(LoginResponse.Status.SUCCESS) || status.equals(LoginResponse.Status.UNAUTHENTICATED)) {
                populateIcons();
                setUpPixelAPIs();
            }
            this.status = switch (status) {
                case SUCCESS, UNAUTHENTICATED -> Status.SUCCESS;
                case FAILED -> Status.FAILED;
                case CANCELED -> Status.CANCELED;
            };

            return this;
        });
    }

    private void initializeSync(URI uri, String... args) {
        try {
            var apisHandler = ApisHandler.create(this, uri).get();

            if (apisHandler.isPresent()) {
                this.apisHandler = apisHandler.get();

                LoginResponse loginResponse = this.apisHandler.canSkipAuthentication().get() ?
                        LoginResponse.createNonSuccessfulLoginResponse(LoginResponse.Status.UNAUTHENTICATED) :
                        login(args).get();

                LoginResponse.Status status = loginResponse.getStatus();
                if (status.equals(LoginResponse.Status.SUCCESS) || status.equals(LoginResponse.Status.UNAUTHENTICATED)) {
                    try {
                        var server = status.equals(LoginResponse.Status.SUCCESS) ?
                                Server.create(this.apisHandler, loginResponse.getGroup(), loginResponse.getUserId()).get() :
                                Server.create(this.apisHandler).get();

                        if (server.isPresent()) {
                            this.server = server.get();
                        } else {
                            status = LoginResponse.Status.FAILED;
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        logger.error("Error initializing client", e);
                        status = LoginResponse.Status.FAILED;
                    }
                }
                if (status.equals(LoginResponse.Status.SUCCESS) || status.equals(LoginResponse.Status.UNAUTHENTICATED)) {
                    populateIcons();
                    setUpPixelAPIs();
                }
                this.status = switch (status) {
                    case SUCCESS, UNAUTHENTICATED -> Status.SUCCESS;
                    case FAILED -> Status.FAILED;
                    case CANCELED -> Status.CANCELED;
                };
            } else {
                status = Status.FAILED;
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error initializing client", e);
            status = Status.FAILED;
        }
    }

    private synchronized void setUsername(String username) {
        this.username.set(username);
    }

    private synchronized void setAuthenticationInformation(LoginResponse loginResponse) {
        this.authenticated.set(true);

        setUsername(loginResponse.getUsername());
        password = loginResponse.getPassword();
    }

    private synchronized void startTimer() {
        if (timeoutTimer == null) {
            timeoutTimer = new Timer("omero-keep-alive", true);
            timeoutTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    apisHandler.ping().thenAccept(success -> {
                        if (!success) {
                            WebClients.removeClient(WebClient.this);
                        }
                    });
                }
            }, PING_DELAY_MILLISECONDS, PING_DELAY_MILLISECONDS);
        }
    }

    private void populateIcons() {
        apisHandler.getProjectIcon().thenAccept(icon -> icon.ifPresent(bufferedImage -> omeroIcons.put(Project.class, bufferedImage)));
        apisHandler.getDatasetIcon().thenAccept(icon -> icon.ifPresent(bufferedImage -> omeroIcons.put(Dataset.class, bufferedImage)));
        apisHandler.getOrphanedFolderIcon().thenAccept(icon -> icon.ifPresent(bufferedImage -> omeroIcons.put(OrphanedFolder.class, bufferedImage)));
        apisHandler.getImageIcon().thenAccept(icon -> icon.ifPresent(bufferedImage -> omeroIcons.put(Image.class, bufferedImage)));
    }

    private void setUpPixelAPIs() {
        availablePixelAPIs = Stream.of(new WebAPI(this), new IceAPI(this))
                .filter(PixelAPI::isAvailable)
                .toList();
        selectedPixelAPI.set(availablePixelAPIs.stream()
                .filter(PixelAPI::canAccessRawPixels)
                .findAny()
                .orElse(availablePixelAPIs.get(0))
        );
    }
}
