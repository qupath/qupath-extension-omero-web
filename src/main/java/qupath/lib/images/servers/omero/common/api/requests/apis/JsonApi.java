package qupath.lib.images.servers.omero.common.api.requests.apis;

import com.google.common.collect.Lists;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.beans.property.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.images.servers.omero.common.api.RequestsUtilities;
import qupath.lib.images.servers.omero.common.api.clients.WebClients;
import qupath.lib.images.servers.omero.common.api.requests.Requests;
import qupath.lib.images.servers.omero.common.api.requests.RequestsHandler;
import qupath.lib.images.servers.omero.common.api.requests.entities.login.LoginResponse;
import qupath.lib.images.servers.omero.common.api.requests.entities.server_information.OmeroAPI;
import qupath.lib.images.servers.omero.common.api.requests.entities.server_information.OmeroServerList;
import qupath.lib.images.servers.omero.common.gui.UiUtilities;
import qupath.lib.images.servers.omero.common.omero_entities.repository_entities.RepositoryEntity;
import qupath.lib.images.servers.omero.common.omero_entities.repository_entities.server_entities.Dataset;
import qupath.lib.images.servers.omero.common.omero_entities.repository_entities.server_entities.Project;
import qupath.lib.images.servers.omero.common.omero_entities.repository_entities.server_entities.ServerEntity;
import qupath.lib.images.servers.omero.images_servers.web.shapes.Shape;
import qupath.lib.objects.PathObject;

import java.net.PasswordAuthentication;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * <p>The OMERO <a href="https://docs.openmicroscopy.org/omero/5.6.0/developers/json-api.html">JSON API</a>.</p>
 * <p>
 *     This API is used to get basic information on the server, authenticate, get details on OMERO entities
 *     (e.g. images, datasets), and get ROIs of an image.
 * </p>
 */
public class JsonApi {
    private final static ResourceBundle resources = UiUtilities.getResources();
    private final static Logger logger = LoggerFactory.getLogger(JsonApi.class);
    private final static String SERVERS_URL_KEY = "url:servers";
    private final static String TOKEN_URL_KEY = "url:token";
    private final static String LOGIN_URL_KEY = "url:login";
    private final static String PROJECTS_URL_KEY = "url:projects";
    private final static String DATASETS_URL_KEY = "url:datasets";
    private final static String IMAGES_URL_KEY = "url:images";
    private static final String API_URL = "%s/api/";
    private static final String PROJECTS_URL = "%s?childCount=true";
    private static final String DATASETS_URL = "%s%d/datasets/?childCount=true";
    private static final String IMAGES_URL = "%s%d/images/?childCount=true";
    private static final String ORPHANED_DATASETS_URL = "%s?childCount=true&orphaned=true";
    private static final String ROIS_URL = "%s/api/v0/m/rois/?image=%s";
    private final IntegerProperty numberOfEntitiesLoading = new SimpleIntegerProperty(0);
    private final BooleanProperty areOrphanedImagesLoading = new SimpleBooleanProperty(false);
    private final IntegerProperty numberOfOrphanedImages = new SimpleIntegerProperty(0);
    private final IntegerProperty numberOfOrphanedImagesLoaded = new SimpleIntegerProperty(0);
    private final URI host;
    private final RequestsHandler requestsHandler;
    private Map<String, String> urls;
    private int serverID;
    private String token;

    private JsonApi(RequestsHandler requestsHandler, URI host) {
        this.requestsHandler = requestsHandler;
        this.host = host;
    }

    /**
     * <p>Attempt to create a JSON API client.</p>
     * <p>This function is asynchronous.</p>
     *
     * @param requestsHandler  the RequestsHandler using this API. It is used to have access to other APIs
     * @param host  the base server URI (e.g. <a href="https://idr.openmicroscopy.org">https://idr.openmicroscopy.org</a>)
     * @return a CompletableFuture with the JSON API client, an empty Optional if an error occurred
     */
    public static CompletableFuture<Optional<JsonApi>> createJsonApi(RequestsHandler requestsHandler, URI host) {
        JsonApi jsonApi = new JsonApi(requestsHandler, host);
        return jsonApi.initialize().thenApply(initialized -> {
            if (initialized) {
                return Optional.of(jsonApi);
            } else {
                return Optional.empty();
            }
        });
    }

    /**
     * @return the <a href="https://docs.openmicroscopy.org/omero/5.6.0/developers/json-api.html#get-csrf-token">CSRF token</a> used by this session
     */
    public String getToken() {
        return token;
    }

    /**
     * @return the number of OMERO entities (e.g. datasets, images) currently being loaded by the API
     */
    public ReadOnlyIntegerProperty getNumberOfEntitiesLoading() {
        return numberOfEntitiesLoading;
    }

    /**
     * @return the number of orphaned images currently being loaded by the API
     */
    public ReadOnlyBooleanProperty getOrphanedImagesLoading() {
        return areOrphanedImagesLoading;
    }

    /**
     * @return the total number of orphaned images
     */
    public ReadOnlyIntegerProperty getNumberOfOrphanedImages() {
        return numberOfOrphanedImages;
    }

    /**
     * @return the number of orphaned images which have been loaded
     */
    public ReadOnlyIntegerProperty getNumberOfOrphanedImagesLoaded() {
        return numberOfOrphanedImagesLoaded;
    }

    /**
     * <p>Attempt to authenticate to the current server.</p>
     * <p>This function is asynchronous.</p>
     *
     * <p>The arguments must have one of the following format:</p>
     * <ul>
     *     <li>{@code --username [username] --password [password]}</li>
     *     <li>{@code -u [username] -p [password]}</li>
     * </ul>
     * <p>
     *     If the arguments are not enough to authenticate, the user will
     *     automatically be asked for credentials.
     * </p>
     *
     * @return a CompletableFuture with the authentication status
     */
    public CompletableFuture<LoginResponse> login(String... args) {
        var uri = RequestsUtilities.createURI(urls.get(LOGIN_URL_KEY));

        PasswordAuthentication authentication = getPasswordAuthenticationFromArgs(args).orElse(
                WebClients.getAuthenticator().requestPasswordAuthenticationInstance(
                    host.toString(),
                    null,
                    0,
                    null,
                    resources.getString("Common.Api.Request.enterLoginDetails"),
                    null,
                    null,
                    null
                )
        );

        if (uri.isEmpty() || authentication == null) {
            return CompletableFuture.completedFuture(LoginResponse.createFailedLoginResponse());
        } else {
            byte[] body = ApiUtilities.concatAndConvertToBytes(
                    String.join("&", "server=" + serverID, "username=" + authentication.getUserName(), "password=").toCharArray(),
                    authentication.getPassword()
            );

            return Requests.post(
                    uri.get(),
                    body,
                    uri.get().toString(),
                    token
            ).thenApply(response -> {
                Arrays.fill(body, (byte) 0);

                return response.map(LoginResponse::createLoginResponse).orElseGet(LoginResponse::createFailedLoginResponse);
            });
        }
    }

    /**
     * <p>Attempt to retrieve all projects of the server.</p>
     * <p>This function is asynchronous.</p>
     *
     * @return a CompletableFuture with the list containing all projects of this server
     */
    public CompletableFuture<List<ServerEntity>> getProjects() {
        return getChildren(String.format(PROJECTS_URL, urls.get(PROJECTS_URL_KEY)));
    }

    /**
     * <p>Attempt to retrieve all orphaned datasets of the server.</p>
     * <p>This function is asynchronous.</p>
     *
     * @return a CompletableFuture with the list containing all orphaned datasets of this server
     */
    public CompletableFuture<List<ServerEntity>> getOrphanedDatasets() {
        return getChildren(String.format(ORPHANED_DATASETS_URL, urls.get(DATASETS_URL_KEY)));
    }

    /**
     * <p>Attempt to retrieve all datasets of a project.</p>
     * <p>This function is asynchronous.</p>
     *
     * @param project  the project whose datasets should be retrieved
     * @return a CompletableFuture with the list containing all datasets of the project
     */
    public CompletableFuture<List<ServerEntity>> getDatasets(Project project) {
        return getChildren(String.format(DATASETS_URL, urls.get(PROJECTS_URL_KEY), project.getId()));
    }

    /**
     * <p>Attempt to retrieve all images of a dataset.</p>
     * <p>This function is asynchronous.</p>
     *
     * @param dataset  the dataset whose images should be retrieved
     * @return a CompletableFuture with the list containing all images of the dataset
     */
    public CompletableFuture<List<ServerEntity>> getImages(Dataset dataset) {
        return getChildren(String.format(IMAGES_URL, urls.get(DATASETS_URL_KEY), dataset.getId()));
    }

    /**
     * <p>
     *     Populate all orphaned images of this server to the list specified in parameter.
     *     This function populates and doesn't return a list because the number of images can
     *     be large, so this operation can take tens of seconds.
     * </p>
     * <p>This function is asynchronous but the list update is done in the UI thread.</p>
     *
     * @param children  the list which should be populated by the orphaned images.
     */
    public void populateOrphanedImagesIntoList(List<RepositoryEntity> children) {
        areOrphanedImagesLoading.set(true);

        getOrphanedImagesURIs().thenAcceptAsync(uris -> {
            // The number of parallel requests is limited to 16
            // to avoid too many concurrent streams
            List<List<URI>> batches = Lists.partition(uris, 16);
            for (List<URI> batch: batches) {
                List<ServerEntity> serverEntities = batch.stream()
                        .map(this::requestImageInfo)
                        .map(CompletableFuture::join)
                        .flatMap(Optional::stream)
                        .map(jsonObject -> ServerEntity.createFromJsonElement(jsonObject, requestsHandler))
                        .flatMap(Optional::stream)
                        .toList();
                Platform.runLater(() -> {
                    children.addAll(serverEntities);
                    numberOfOrphanedImagesLoaded.set(numberOfOrphanedImagesLoaded.get() + batch.size());
                });
            }

            Platform.runLater(() -> areOrphanedImagesLoading.set(false));
        });
    }

    /**
     * <p>Indicates if the server can be browsed without being authenticated.</p>
     * <p>This function is asynchronous.</p>
     *
     * @return a CompletableFuture indicating if authentication can be skipped
     */
    public CompletableFuture<Boolean> canSkipAuthentication() {
        var uri = RequestsUtilities.createURI(urls.get(PROJECTS_URL_KEY));

        if (uri.isPresent()) {
            return Requests.isLinkReachable(uri.get());
        } else {
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * <p>Attempt to retrieve ROIs of an image.</p>
     * <p>This function is asynchronous.</p>
     *
     * @param id  the OMERO image ID
     * @return a CompletableFuture with the list of ROIs. If an error occurs, the list is empty
     */
    public CompletableFuture<List<PathObject>> getROIs(long id) {
        var uri = RequestsUtilities.createURI(String.format(ROIS_URL, host, id));

        if (uri.isPresent()) {
            var gson = new GsonBuilder().registerTypeAdapter(Shape.class, new Shape.GsonShapeDeserializer()).setLenient().create();

            return Requests.getPaginated(uri.get()).thenApply(jsonElements -> jsonElements.stream()
                    .map(datum -> datum.getAsJsonObject().getAsJsonArray("shapes").asList().stream()
                            .map(jsonElement -> {
                                try {
                                    return gson.fromJson(jsonElement, Shape.class);
                                } catch (JsonSyntaxException e) {
                                    logger.error("Error parsing shape", e);
                                    return null;
                                }
                            })
                            .filter(Objects::nonNull)
                            .map(Shape::createAnnotation)
                            .toList())
                    .flatMap(List::stream)
                    .toList());
        } else {
            return CompletableFuture.completedFuture(List.of());
        }
    }

    /**
     * <p>Attempt to retrieve the URIs of all orphaned images of the server.</p>
     * <p>This function is asynchronous.</p>
     *
     * @return a CompletableFuture with the list of URIs of all orphaned images. If an error occurs, the list is empty
     */
    public CompletableFuture<List<URI>> getOrphanedImagesURIs() {
        return requestsHandler.getOrphanedImagesIds().thenApply(ids -> ids.stream()
                .map(id -> RequestsUtilities.createURI(urls.get(IMAGES_URL_KEY) + id))
                .flatMap(Optional::stream)
                .toList()
        ).thenApply(uris -> {
            Platform.runLater(() -> numberOfOrphanedImages.set(uris.size()));
            return uris;
        });
    }

    private CompletableFuture<Map<String, String>> getURLs(String url) {
        var uri = RequestsUtilities.createURI(url);

        if (uri.isPresent()) {
            return Requests.getAndConvert(uri.get(), new TypeToken<Map<String, String>>() {}).thenApply(response -> response.orElse(Map.of()));
        } else {
            return CompletableFuture.completedFuture(Map.of());
        }
    }

    private CompletableFuture<Boolean> initialize() {
        var uri = RequestsUtilities.createURI(String.format(API_URL, host));

        if (uri.isPresent()) {
            return Requests.getAndConvert(uri.get(), OmeroAPI.class)
                    .thenCompose(omeroAPI -> {
                        if (omeroAPI.isPresent() && omeroAPI.get().getLatestVersionURL().isPresent()) {
                            return getURLs(omeroAPI.get().getLatestVersionURL().get());
                        } else {
                            return CompletableFuture.completedFuture(Map.of());
                        }
                    })
                    .thenApplyAsync(urls -> {
                        if (urls.isEmpty()) {
                            logger.error("Could not find API URL in " + uri.get());
                            return false;
                        } else {
                            if (setServerID(urls).join() && setToken(urls).join()) {
                                this.urls = urls;

                                return true;
                            } else {
                                return false;
                            }
                        }
                    });
        } else {
            return CompletableFuture.completedFuture(false);
        }
    }

    private CompletableFuture<Boolean> setServerID(Map<String, String> urls) {
        String url = SERVERS_URL_KEY;

        if (urls.containsKey(url)) {
            var uri = RequestsUtilities.createURI(urls.get(url));

            if (uri.isPresent()) {
                return Requests.getAndConvert(
                        uri.get(),
                        OmeroServerList.class
                ).thenApply(serverList -> {
                    if (serverList.isPresent() && serverList.get().getServerId().isPresent()) {
                        serverID = serverList.get().getServerId().get();
                        return true;
                    } else {
                        logger.error("Couldn't get id. The server response doesn't contain the required information.");
                        return false;
                    }
                });
            } else {
                return CompletableFuture.completedFuture(false);
            }
        } else {
            logger.error("Couldn't find the URL corresponding to " + url);
            return CompletableFuture.completedFuture(false);
        }
    }

    private CompletableFuture<Boolean> setToken(Map<String, String> urls) {
        String url = TOKEN_URL_KEY;

        if (urls.containsKey(url)) {
            var uri = RequestsUtilities.createURI(urls.get(url));

            if (uri.isPresent()) {
                return Requests.getAndConvert(
                        uri.get(),
                        new TypeToken<Map<String, String>>() {}
                ).thenApply(response -> {
                    boolean canGetToken = response.isPresent() && response.get().containsKey("data");

                    if (canGetToken) {
                        token = response.get().get("data");
                    } else {
                        logger.error("Couldn't get token. The server response doesn't contain the required information.");
                    }

                    return canGetToken;
                });
            } else {
                return CompletableFuture.completedFuture(false);
            }
        } else {
            logger.error("Couldn't find the URL corresponding to " + url);
            return CompletableFuture.completedFuture(false);
        }
    }

    private Optional<PasswordAuthentication> getPasswordAuthenticationFromArgs(String... args) {
        String username = null;
        char[] password = null;
        int i = 0;
        while (i < args.length-1) {
            String parameter = args[i++];
            if ("--username".equals(parameter) || "-u".equals(parameter)) {
                username = args[i++];
            }
            else if ("--password".equals(parameter) || "-p".equals(parameter)) {
                password = args[i++].toCharArray();
            }
        }
        if (username != null && password != null) {
            return Optional.of(new PasswordAuthentication(username, password));
        } else {
            return Optional.empty();
        }
    }

    private CompletableFuture<List<ServerEntity>> getChildren(String url) {
        var uri = RequestsUtilities.createURI(url);

        if (uri.isPresent()) {
            numberOfEntitiesLoading.set(numberOfEntitiesLoading.get() + 1);
            return Requests.getPaginated(uri.get()).thenApply(jsonElements -> {
                Platform.runLater(() -> numberOfEntitiesLoading.set(numberOfEntitiesLoading.get() - 1));

                return ServerEntity.createFromJsonElements(jsonElements, requestsHandler).toList();
            });
        } else {
            return CompletableFuture.completedFuture(List.of());
        }
    }

    private CompletableFuture<Optional<JsonObject>> requestImageInfo(URI uri) {
        return Requests.getAndConvert(uri, JsonObject.class).thenApply(response -> {
            if (response.isPresent()) {
                try  {
                    return Optional.ofNullable(response.get().getAsJsonObject("data"));
                } catch (Exception e) {
                    logger.error("Cannot read 'data' member in " + response, e);
                    return Optional.empty();
                }
            } else {
                return Optional.empty();
            }
        });
    }
}
