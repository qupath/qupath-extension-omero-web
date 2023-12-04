package qupath.ext.omero.core.apis;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.omero.core.entities.annotations.AnnotationGroup;
import qupath.ext.omero.core.entities.repositoryentities.serverentities.*;
import qupath.ext.omero.core.entities.repositoryentities.serverentities.image.Image;
import qupath.ext.omero.core.entities.search.SearchQuery;
import qupath.ext.omero.core.entities.search.SearchResult;
import qupath.ext.omero.core.WebUtilities;
import qupath.ext.omero.core.RequestSender;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * <p>API to communicate with a OMERO.web server.</p>
 * <p>
 *     This API is mainly used to keep a connection alive, log out, perform a search
 *     and get OMERO annotations.
 * </p>
 * <p>An instance of this class must be {@link #close() closed} once no longer used.</p>
 */
class WebclientApi implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(WebclientApi.class);
    private static final String PING_URL = "%s/webclient/keepalive_ping/";
    private static final String ITEM_URL = "%s/webclient/?show=%s-%d";
    private static final String LOGOUT_URL = "%s/webclient/logout/";
    private static final String ORPHANED_IMAGES_URL = "%s/webclient/api/images/?orphaned=true";
    private static final String READ_ANNOTATION_URL = "%s/webclient/api/annotations/?%s=%d";
    private final static String SEARCH_URL = "%s/webclient/load_searching/form/" +
            "?query=%s&%s&%s&searchGroup=%s&ownedBy=%s" +
            "&useAcquisitionDate=false&startdateinput=&enddateinput=&_=%d";
    private static final String IMAGE_ICON_URL = "%s/static/webclient/image/image16.png";
    private static final String SCREEN_ICON_URL = "%s/static/webclient/image/folder_screen16.png";
    private static final String PLATE_ICON_URL = "%s/static/webclient/image/folder_plate16.png";
    private static final String PLATE_ACQUISITION_ICON_URL = "%s/static/webclient/image/run16.png";
    private static final Map<Class<? extends ServerEntity>, String> TYPE_TO_URI_LABEL = Map.of(
            Image.class, "image",
            Dataset.class, "dataset",
            Project.class, "project",
            Screen.class, "screen",
            Plate.class, "plate",
            PlateAcquisition.class, "run"
    );
    private final URI host;
    private final URI pingUri;
    private String token;

    /**
     * Creates a web client.
     *
     * @param host  the base server URI (e.g. <a href="https://idr.openmicroscopy.org">https://idr.openmicroscopy.org</a>)
     */
    public WebclientApi(URI host) {
        this.host = host;

        pingUri = WebUtilities.createURI(String.format(PING_URL, host)).orElse(null);
    }

    @Override
    public void close() {
        if (token != null) {
            WebUtilities.createURI(String.format(LOGOUT_URL, host)).ifPresent(value -> RequestSender.post(
                    value,
                    Map.of("csrfmiddlewaretoken", token),
                    value.toString(),
                    token
            ));
        }
    }

    @Override
    public String toString() {
        return String.format("Webclient API of %s", host);
    }

    /**
     * Set the <a href="https://docs.openmicroscopy.org/omero/5.6.0/developers/json-api.html#get-csrf-token">CSRF token</a>
     * used by this session. This is needed to properly close this API.
     *
     * @param token  the CSRF token of the session
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * Returns a link of the OMERO.web client pointing to a server entity.
     *
     * @param entity  the entity to have a link to.
     *                Must be an {@link Image}, {@link Dataset}, {@link Project},
     *                {@link Screen}, {@link Plate} or {@link PlateAcquisition}
     * @return a URL pointing to the server entity
     * @throws IllegalArgumentException when the provided entity is not an image, dataset, project,
     * screen, plate, or plate acquisition
     */
    public String getEntityURI(ServerEntity entity) {
        if (!TYPE_TO_URI_LABEL.containsKey(entity.getClass())) {
            throw new IllegalArgumentException(String.format(
                    "The provided item (%s) is not an image, dataset, project, screen, plate, or plate acquisition.",
                    entity
            ));
        }

        return String.format(ITEM_URL,
                host,
                TYPE_TO_URI_LABEL.get(entity.getClass()),
                entity.getId()
        );
    }

    /**
     * <p>
     *     Attempt to send a ping to the server. This is needed to keep the connection alive between the client
     *     and the server.
     * </p>
     * <p>This function is asynchronous.</p>
     *
     * @return a CompletableFuture indicating the success of the operation
     */
    public CompletableFuture<Boolean> ping() {
        if (pingUri == null) {
            return CompletableFuture.completedFuture(false);
        } else {
            return RequestSender.isLinkReachableWithGet(pingUri);
        }
    }

    /**
     * <p>Attempt to get the image IDs of all orphaned images of the server.</p>
     * <p>This function is asynchronous.</p>
     *
     * @return a CompletableFuture with a list containing the ID of all orphaned images,
     * or an empty list if an error occurred
     */
    public CompletableFuture<List<Long>> getOrphanedImagesIds() {
        var uri = WebUtilities.createURI(String.format(ORPHANED_IMAGES_URL, host));

        if (uri.isPresent()) {
            return RequestSender.getAndConvertToJsonList(uri.get(), "images").thenApply(elements ->
                    elements.stream()
                            .map(jsonElement -> {
                                try {
                                    return Long.parseLong(jsonElement.getAsJsonObject().get("id").toString());
                                } catch (Exception e) {
                                    logger.error("Could not parse " + jsonElement, e);
                                    return null;
                                }
                            })
                            .filter(Objects::nonNull)
                            .toList()
            );
        } else {
            return CompletableFuture.completedFuture(List.of());
        }
    }

    /**
     * <p>
     *     Attempt to retrieve OMERO annotations of an OMERO entity .
     *     An OMERO annotation is <b>not</b> similar to a QuPath annotation, it refers to some metadata
     *     attached to an entity.
     * </p>
     * <p>This function is asynchronous.</p>
     *
     * @param entity  the type of the entity whose annotation should be retrieved.
     *                Must be an {@link Image}, {@link Dataset}, {@link Project},
     *                {@link Screen}, {@link Plate}, or {@link PlateAcquisition}.
     * @return a CompletableFuture with the annotation, or an empty Optional if an error occurred
     * @throws IllegalArgumentException when the provided entity is not an image, dataset, project,
     * screen, plate, or plate acquisition
     */
    public CompletableFuture<Optional<AnnotationGroup>> getAnnotations(ServerEntity entity) {
        if (!TYPE_TO_URI_LABEL.containsKey(entity.getClass())) {
            throw new IllegalArgumentException(String.format(
                    "The provided item (%s) is not an image, dataset, project, screen, plate, or plate acquisition.",
                    entity
            ));
        }

        var uri = WebUtilities.createURI(String.format(
                READ_ANNOTATION_URL,
                host,
                TYPE_TO_URI_LABEL.get(entity.getClass()),
                entity.getId()
        ));

        if (uri.isPresent()) {
            return RequestSender.getAndConvert(uri.get(), JsonObject.class)
                    .thenApply(json -> json.map(AnnotationGroup::new));
        } else {
            return CompletableFuture.completedFuture(Optional.empty());
        }
    }

    /**
     * <p>Attempt to perform a search on the server.</p>
     * <p>This function is asynchronous.</p>
     *
     * @param searchQuery  the parameters used in the search
     * @return a CompletableFuture with a list of search results, or an empty list if an error occurred
     */
    public CompletableFuture<List<SearchResult>> getSearchResults(SearchQuery searchQuery) {
        StringBuilder fields = new StringBuilder();
        if (searchQuery.searchOnName()) {
            fields.append("&field=name");
        }
        if (searchQuery.searchOnDescription()) {
            fields.append("&field=description");
        }

        StringBuilder dataTypes = new StringBuilder();
        if (searchQuery.searchForImages()) {
            dataTypes.append("&datatype=images");
        }
        if (searchQuery.searchForDatasets()) {
            dataTypes.append("&datatype=datasets");
        }
        if (searchQuery.searchForProjects()) {
            dataTypes.append("&datatype=projects");
        }
        if (searchQuery.searchForWells()) {
            dataTypes.append("&datatype=wells");
        }
        if (searchQuery.searchForPlates()) {
            dataTypes.append("&datatype=plates");
        }
        if (searchQuery.searchForScreens()) {
            dataTypes.append("&datatype=screens");
        }

        var uri = WebUtilities.createURI(String.format(SEARCH_URL,
                host,
                searchQuery.query(),
                fields,
                dataTypes,
                searchQuery.group().getId(),
                searchQuery.owner().id(),
                System.currentTimeMillis()
        ));
        if (uri.isPresent()) {
            return RequestSender.get(uri.get()).thenApply(response ->
                    response.map(s -> SearchResult.createFromHTMLResponse(s, host)).orElseGet(List::of)
            );
        } else {
            return CompletableFuture.completedFuture(List.of());
        }
    }

    /**
     * <p>Attempt to retrieve the OMERO image icon.</p>
     * <p>This function is asynchronous.</p>
     *
     * @return a CompletableFuture with the icon, of an empty Optional if an error occurred
     */
    public CompletableFuture<Optional<BufferedImage>> getImageIcon() {
        return ApiUtilities.getImage(String.format(IMAGE_ICON_URL, host));
    }

    /**
     * <p>Attempt to retrieve the OMERO screen icon.</p>
     * <p>This function is asynchronous.</p>
     *
     * @return a CompletableFuture with the icon, of an empty Optional if an error occurred
     */
    public CompletableFuture<Optional<BufferedImage>> getScreenIcon() {
        return ApiUtilities.getImage(String.format(SCREEN_ICON_URL, host));
    }

    /**
     * <p>Attempt to retrieve the OMERO plate icon.</p>
     * <p>This function is asynchronous.</p>
     *
     * @return a CompletableFuture with the icon, of an empty Optional if an error occurred
     */
    public CompletableFuture<Optional<BufferedImage>> getPlateIcon() {
        return ApiUtilities.getImage(String.format(PLATE_ICON_URL, host));
    }

    /**
     * <p>Attempt to retrieve the OMERO plate acquisition icon.</p>
     * <p>This function is asynchronous.</p>
     *
     * @return a CompletableFuture with the icon, of an empty Optional if an error occurred
     */
    public CompletableFuture<Optional<BufferedImage>> getPlateAcquisitionIcon() {
        return ApiUtilities.getImage(String.format(PLATE_ACQUISITION_ICON_URL, host));
    }
}
