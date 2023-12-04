package qupath.ext.omero.core;

import com.drew.lang.annotations.Nullable;
import javafx.beans.property.*;
import javafx.collections.*;
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
import qupath.ext.omero.core.pixelapis.mspixelbuffer.MsPixelBufferAPI;
import qupath.lib.gui.viewer.QuPathViewer;

import java.net.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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
    private final ObservableList<PixelAPI> availablePixelAPIs = FXCollections.observableArrayList();
    private final ObservableList<PixelAPI> availablePixelAPIsImmutable = FXCollections.unmodifiableObservableList(availablePixelAPIs);
    private final StringProperty username = new SimpleStringProperty("");
    private final BooleanProperty authenticated = new SimpleBooleanProperty(false);
    private final ObjectProperty<PixelAPI> selectedPixelAPI = new SimpleObjectProperty<>();
    private final ObservableSet<URI> openedImagesURIs = FXCollections.observableSet();
    private final ObservableSet<URI> openedImagesURIsImmutable = FXCollections.unmodifiableObservableSet(openedImagesURIs);
    private Server server;
    private ApisHandler apisHandler;
    private List<PixelAPI> allPixelAPIs;
    private Timer timeoutTimer;
    private String sessionUuid;
    private Status status;
    private FailReason failReason;

    public enum Status {
        CANCELED,
        FAILED,
        SUCCESS
    }

    public enum FailReason {
        ALREADY_CREATING,
        INVALID_URI_FORMAT
    }

    private WebClient() {}

    /**
     * <p>
     *     Static factory method creating a new client.
     *     It will initialize the connection and ask for credentials if this is required to access the server.
     * </p>
     * <p>
     *     This function should only be used by {@link WebClients WebClients}
     *     which monitors opened clients (see {@link WebClients#createClient(String, boolean, String...)}).
     * </p>
     * <p>
     *     Note that this function is not guaranteed to create a valid client. Call the
     *     {@link #getStatus()} function to check the validity of the returned client
     *     before using it. If a client is not valid, some functions of this class
     *     will throw exceptions.
     * </p>
     * <p>The optional arguments must have one of the following format:</p>
     * <ul>
     *     <li>{@code --username [username] --password [password]}</li>
     *     <li>{@code -u [username] -p [password]}</li>
     * </ul>
     * <p>This function is asynchronous.</p>
     *
     * @param uri  the server URI to connect to
     * @param canSkipAuthentication  whether authentication can be skipped if the server allows it
     * @param args  optional arguments to authenticate (see description above)
     * @return a CompletableFuture with the client
     */
    static CompletableFuture<WebClient> create(URI uri, boolean canSkipAuthentication, String... args) {
        return new WebClient().initialize(uri, canSkipAuthentication, args);
    }

    /**
     * <p>Synchronous version of {@link #create(URI, boolean, String...)}.</p>
     * <p>This function may block the calling thread for around a second.</p>
     */
    static WebClient createSync(URI uri, boolean canSkipAuthentication, String... args) {
        WebClient webClient = new WebClient();
        webClient.initializeSync(uri, canSkipAuthentication, args);
        return webClient;
    }

    /**
     * <p>Creates an invalid client.</p>
     * <p>
     *     This function should only be used by {@link WebClients WebClients}
     *     which monitors opened clients.
     * </p>
     *
     * @param failReason  the reason why the creation failed
     * @return an invalid client
     */
    static WebClient createInvalidClient(FailReason failReason) {
        WebClient webClient = new WebClient();
        webClient.status = Status.FAILED;
        webClient.failReason = failReason;
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
     * @return the reason why the client creation failed, or an empty Optional
     * if the reason was not specified
     */
    public Optional<FailReason> getFailReason() {
        return Optional.ofNullable(failReason);
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
     * @return the session UUID of the authenticated user, or an empty Optional if
     * there is no authentication
     */
    public Optional<String> getSessionUuid() {
        return Optional.ofNullable(sessionUuid);
    }

    /**
     * @return the currently selected pixel API. This property may be updated from any thread
     * and may be null
     */
    public ReadOnlyObjectProperty<PixelAPI> getSelectedPixelAPI() {
        return selectedPixelAPI;
    }

    /**
     * Set the currently selected pixel API of this client.
     *
     * @param pixelAPI  the pixel API to select
     * @throws IllegalArgumentException when the provided pixel API is not available
     * or not part of the pixel APIs of this client
     */
    public void setSelectedPixelAPI(PixelAPI pixelAPI) {
        if (!pixelAPI.isAvailable().get()) {
            throw new IllegalArgumentException("The provided pixel API is not available");
        }
        if (!allPixelAPIs.contains(pixelAPI)) {
            throw new IllegalArgumentException("The provided pixel API is not part of the pixel APIs of this client");
        }

        selectedPixelAPI.set(pixelAPI);
    }

    /**
     * @return an immutable observable list of all pixel APIs available for this client.
     * This set may be updated from any thread
     */
    public ObservableList<PixelAPI> getAvailablePixelAPIs() {
        return availablePixelAPIsImmutable;
    }

    /**
     * Return the pixel API corresponding to the class passed in parameter.
     * This pixel API is not guaranteed to be available.
     *
     * @param pixelAPIClass  the class of the pixel API to retrieve
     * @return the pixel API corresponding to the class passed in parameter
     * @param <T>  the class of the pixel API to retrieve
     * @throws IllegalArgumentException if the pixel API was not found
     */
    public <T extends PixelAPI> T getPixelAPI(Class<T> pixelAPIClass) {
        var pixelAPI = allPixelAPIs.stream()
                .filter(pixelAPIClass::isInstance)
                .map(pixelAPIClass::cast)
                .findAny();

        if (pixelAPI.isPresent()) {
            return pixelAPI.get();
        } else {
            throw new IllegalArgumentException("The pixel API was not found");
        }
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

    private CompletableFuture<WebClient> initialize(URI uri, boolean canSkipAuthentication, String... args) {
        return ApisHandler.create(this, uri).thenApplyAsync(apisHandler -> {
            if (apisHandler.isPresent()) {
                this.apisHandler = apisHandler.get();
                try {
                    Optional<String> usernameFromArgs = getCredentialFromArgs("--username", "-u", args);
                    Optional<String> passwordFromArgs = getCredentialFromArgs("--password", "-p", args);

                    if (
                            (usernameFromArgs.isEmpty() || passwordFromArgs.isEmpty())
                                    && canSkipAuthentication
                                    && this.apisHandler.canSkipAuthentication().get()
                    ) {
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
            this.status = switch (status) {
                case SUCCESS, UNAUTHENTICATED -> Status.SUCCESS;
                case FAILED -> Status.FAILED;
                case CANCELED -> Status.CANCELED;
            };

            if (this.status.equals(Status.SUCCESS)) {
                setUpPixelAPIs();
            }

            return this;
        });
    }

    private void initializeSync(URI uri, boolean canSkipAuthentication, String... args) {
        try {
            var apisHandler = ApisHandler.create(this, uri).get();

            if (apisHandler.isPresent()) {
                this.apisHandler = apisHandler.get();

                Optional<String> usernameFromArgs = getCredentialFromArgs("--username", "-u", args);
                Optional<String> passwordFromArgs = getCredentialFromArgs("--password", "-p", args);

                LoginResponse loginResponse;
                if (
                        (usernameFromArgs.isEmpty() || passwordFromArgs.isEmpty())
                                && canSkipAuthentication
                                && this.apisHandler.canSkipAuthentication().get()
                ) {
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
                this.status = switch (status) {
                    case SUCCESS, UNAUTHENTICATED -> Status.SUCCESS;
                    case FAILED -> Status.FAILED;
                    case CANCELED -> Status.CANCELED;
                };

                if (this.status.equals(Status.SUCCESS)) {
                    setUpPixelAPIs();
                }
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
        sessionUuid = loginResponse.getSessionUuid();
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
        allPixelAPIs = List.of(
                new WebAPI(this),
                new IceAPI(this),
                new MsPixelBufferAPI(this)
        );

        availablePixelAPIs.setAll(allPixelAPIs.stream()
                .filter(pixelAPI -> pixelAPI.isAvailable().get())
                .toList()
        );
        for (PixelAPI pixelAPI: allPixelAPIs) {
            pixelAPI.isAvailable().addListener((p, o, n) -> {
                if (n && !availablePixelAPIs.contains(pixelAPI)) {
                    availablePixelAPIs.add(pixelAPI);
                } else {
                    availablePixelAPIs.remove(pixelAPI);
                }
            });
        }

        selectedPixelAPI.set(availablePixelAPIs.stream()
                .filter(PixelAPI::canAccessRawPixels)
                .findAny()
                .orElse(availablePixelAPIs.get(0))
        );
        availablePixelAPIs.addListener((ListChangeListener<? super PixelAPI>) change ->
                selectedPixelAPI.set(availablePixelAPIs.stream()
                        .filter(PixelAPI::canAccessRawPixels)
                        .findAny()
                        .orElse(availablePixelAPIs.get(0))
        ));
    }
}
