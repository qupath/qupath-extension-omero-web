package qupath.ext.omero.core;

import com.drew.lang.annotations.Nullable;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.omero.core.apis.ApisHandler;
import qupath.ext.omero.core.entities.login.LoginResponse;
import qupath.ext.omero.imagesserver.OmeroImageServer;
import qupath.lib.gui.QuPathGUI;
import qupath.ext.omero.core.entities.repositoryentities.Server;
import qupath.ext.omero.core.pixelapis.ice.IceAPI;
import qupath.ext.omero.core.pixelapis.PixelAPI;
import qupath.ext.omero.core.pixelapis.web.WebAPI;
import qupath.lib.gui.viewer.QuPathViewer;

import java.net.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

/**
 * <p>Class representing an OMERO Web Client.</p>
 * <p>
 *     It handles creating a connection with an OMERO server, logging in, keeping the connection alive,
 *     and logging out.
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
 * <p>
 *     A client must be {@link #close() closed} once no longer used.
 *     This is handled by {@link WebClients#removeClient(WebClient)}.
 * </p>
 */
public class WebClient implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(WebClient.class);
    private static final long PING_DELAY_MILLISECONDS = 60000L;
    private final StringProperty username = new SimpleStringProperty("");
    private final BooleanProperty authenticated = new SimpleBooleanProperty(false);
    private final ObjectProperty<PixelAPI> selectedPixelAPI = new SimpleObjectProperty<>();
    private final ObservableSet<URI> openedImagesURIs = FXCollections.observableSet();
    private final ObservableSet<URI> openedImagesURIsImmutable = FXCollections.unmodifiableObservableSet(openedImagesURIs);
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
     * <p>The optional arguments must have one of the following format:</p>
     * <ul>
     *     <li>{@code --username [username] --password [password]}</li>
     *     <li>{@code -u [username] -p [password]}</li>
     * </ul>
     * <p>This function is asynchronous.</p>
     *
     * @param uri  the server URI to connect to
     * @param args  optional arguments to authenticate (see description above)
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
     * @return the password of the authenticated user, or an empty Optional if
     * there is no authentication
     */
    public Optional<char[]> getPassword() {
        return Optional.ofNullable(password);
    }

    /**
     * @return the currently selected pixel API
     */
    public ObjectProperty<PixelAPI> getSelectedPixelAPI() {
        return selectedPixelAPI;
    }

    /**
     * @return a list of all pixel APIs available for this client
     */
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
     * Indicates if this client can be closed, by checking if there is any
     * opened image in the QuPath viewer that belongs to this client.
     *
     * @return whether this client can be closed
     */
    public boolean canBeClosed() {
        return !(QuPathGUI.getInstance() != null && QuPathGUI.getInstance().getAllViewers().stream()
                .map(QuPathViewer::getServer)
                .anyMatch(server -> server instanceof OmeroImageServer omeroImageServer && omeroImageServer.getClient().equals(this)));
    }

    /**
     * <p>Attempt to authenticate to the server using the provided credentials.</p>
     * <p>
     *     A window (if the GUI is used) or the command line (else) will ask the user for credentials
     *     if one of the parameter is null.
     * </p>
     * <p>This function is asynchronous.</p>
     *
     * @param username  the username used for login
     * @param password  the password used for login
     * @return a CompletableFuture with the login response
     */
    public CompletableFuture<LoginResponse> login(@Nullable String username, @Nullable String password) {
        return apisHandler.login(username, password).thenApply(loginResponse -> {
            if (loginResponse.getStatus().equals(LoginResponse.Status.SUCCESS)) {
                setAuthenticationInformation(loginResponse);
                startTimer();
            }

            return loginResponse;
        });
    }

    /**
     * <p>Attempt to authenticate to the server.</p>
     * <p>
     *     A window (if the GUI is used) or the command line (else) will ask the user for credentials.
     * </p>
     * <p>This function is asynchronous.</p>
     *
     * @return a CompletableFuture with the login response
     */
    public CompletableFuture<LoginResponse> login() {
        return apisHandler.login(null, null).thenApply(loginResponse -> {
            if (loginResponse.getStatus().equals(LoginResponse.Status.SUCCESS)) {
                setAuthenticationInformation(loginResponse);
                startTimer();
            }

            return loginResponse;
        });
    }

    private CompletableFuture<WebClient> initialize(URI uri, String... args) {
        return ApisHandler.create(this, uri).thenApplyAsync(apisHandler -> {
            if (apisHandler.isPresent()) {
                this.apisHandler = apisHandler.get();
                try {
                    Optional<String> usernameFromArgs = getCredentialFromArgs("--username", "-u", args);
                    Optional<String> passwordFromArgs = getCredentialFromArgs("--password", "-p", args);

                    if ((usernameFromArgs.isEmpty() || passwordFromArgs.isEmpty()) && this.apisHandler.canSkipAuthentication().get()) {
                        return LoginResponse.createNonSuccessfulLoginResponse(LoginResponse.Status.UNAUTHENTICATED);
                    } else {
                        return login(
                                usernameFromArgs.orElse(null),
                                passwordFromArgs.orElse(null)
                        ).get();
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

                Optional<String> usernameFromArgs = getCredentialFromArgs("--username", "-u", args);
                Optional<String> passwordFromArgs = getCredentialFromArgs("--password", "-p", args);

                LoginResponse loginResponse;
                if ((usernameFromArgs.isEmpty() || passwordFromArgs.isEmpty()) && this.apisHandler.canSkipAuthentication().get()) {
                    loginResponse = LoginResponse.createNonSuccessfulLoginResponse(LoginResponse.Status.UNAUTHENTICATED);
                } else {
                    loginResponse = login(
                            usernameFromArgs.orElse(null),
                            passwordFromArgs.orElse(null)
                    ).get();
                }

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

    private synchronized void setAuthenticationInformation(LoginResponse loginResponse) {
        this.authenticated.set(true);

        username.set(loginResponse.getUsername());
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

    private static Optional<String> getCredentialFromArgs(
            String credentialLabel,
            String credentialLabelAlternative,
            String... args
    ) {
        String credential = null;
        int i = 0;
        while (i < args.length-1) {
            String parameter = args[i++];
            if (credentialLabel.equals(parameter) || credentialLabelAlternative.equals(parameter)) {
                credential = args[i++];
            }
        }

        return Optional.ofNullable(credential);
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
