package qupath.lib.images.servers.omero.core.apis;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.images.servers.omero.core.entities.search.SearchQuery;
import qupath.lib.images.servers.omero.core.entities.search.SearchResult;
import qupath.lib.images.servers.omero.core.WebUtilities;
import qupath.lib.images.servers.omero.core.RequestSender;
import qupath.lib.images.servers.omero.core.entities.annotations.AnnotationGroup;
import qupath.lib.images.servers.omero.core.entities.repositoryentities.serverentities.ServerEntity;

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
    public void close() throws Exception {
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
     * Returns a link of the OMERO.web client pointing to a server entity (e.g. an image, a dataset).
     *
     * @param serverEntity  the entity to have a link to
     * @return a URL pointing to the server entity
     */
    public String getItemURI(ServerEntity serverEntity) {
        return String.format(ITEM_URL,
                host,
                serverEntity.getType(),
                serverEntity.getId()
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
            return RequestSender.isLinkReachable(pingUri);
        }
    }

    /**
     * <p>Attempt to get the image IDs of all orphaned images of the server.</p>
     * <p>This function is asynchronous.</p>
     *
     * @return a CompletableFuture with a list containing the ID of all orphaned images,
     * or an empty list if an error occurred
     */
    public CompletableFuture<List<Integer>> getOrphanedImagesIds() {
        var uri = WebUtilities.createURI(String.format(ORPHANED_IMAGES_URL, host));

        if (uri.isPresent()) {
            return RequestSender.requestAndConvertToJsonList(uri.get(), "images").thenApply(elements ->
                    elements.stream()
                            .map(jsonElement -> {
                                try {
                                    return Integer.parseInt(jsonElement.getAsJsonObject().get("id").toString());
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
     *     Attempt to retrieve OMERO annotations of an OMERO entity (e.g. an image, dataset).
     *     An OMERO annotation is <b>not</b> similar to a QuPath annotation, it refers to some metadata
     *     attached to an entity.
     * </p>
     * <p>This function is asynchronous.</p>
     *
     * @param serverEntity  the entity whose annotation should be retrieved
     * @return a CompletableFuture with the annotation, or an empty Optional if an error occurred
     */
    public CompletableFuture<Optional<AnnotationGroup>> getAnnotations(ServerEntity serverEntity) {
        var uri = WebUtilities.createURI(String.format(READ_ANNOTATION_URL, host, serverEntity.getType(), serverEntity.getId()));

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
                searchQuery.owner().getId(),
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
     * @return a CompletableFuture with the icon, of an empty Optional if an error occured
     */
    public CompletableFuture<Optional<BufferedImage>> getImageIcon() {
        return ApiUtilities.getImage(String.format(IMAGE_ICON_URL, host));
    }
}
