package qupath.lib.images.servers.omero.common.api.requests;

import javafx.beans.property.*;
import qupath.lib.images.servers.*;
import qupath.lib.images.servers.omero.common.api.requests.entities.imagemetadata.ImageMetadataResponse;
import qupath.lib.images.servers.omero.common.api.requests.entities.search.SearchQuery;
import qupath.lib.images.servers.omero.common.api.requests.apis.IViewerApi;
import qupath.lib.images.servers.omero.common.api.requests.apis.JsonApi;
import qupath.lib.images.servers.omero.common.api.requests.apis.WebGatewayApi;
import qupath.lib.images.servers.omero.common.api.requests.apis.WebclientApi;
import qupath.lib.images.servers.omero.common.api.requests.entities.login.LoginResponse;
import qupath.lib.images.servers.omero.common.api.requests.entities.search.SearchResult;
import qupath.lib.images.servers.omero.common.omeroentities.annotations.AnnotationGroup;
import qupath.lib.images.servers.omero.common.omeroentities.repositoryentities.RepositoryEntity;
import qupath.lib.images.servers.omero.common.omeroentities.repositoryentities.serverentities.Dataset;
import qupath.lib.images.servers.omero.common.omeroentities.repositoryentities.serverentities.Project;
import qupath.lib.images.servers.omero.common.omeroentities.repositoryentities.serverentities.ServerEntity;
import qupath.lib.objects.PathObject;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * <p>This class provides functions to perform operations with an OMERO server.</p>
 * <p>
 *     As different APIs are used to perform the operations, this class only
 *     redirect each web request to the appropriate API contained in
 *     {@link qupath.lib.images.servers.omero.common.api.requests.apis apis}.
 * </p>
 */
public class RequestsHandler {
    private final URI host;
    private final WebclientApi webclientApi;
    private final WebGatewayApi webGatewayApi;
    private final IViewerApi iViewerApi;
    private JsonApi jsonApi;

    private RequestsHandler(URI host) {
        this.host = host;

        webclientApi = new WebclientApi(host);
        webGatewayApi = new WebGatewayApi(host);
        iViewerApi = new IViewerApi(host);
    }

    /**
     * <p>Attempt to create a request handler.</p>
     * <p>This function is asynchronous.</p>
     *
     * @param host  the base server URI (e.g. <a href="https://idr.openmicroscopy.org">https://idr.openmicroscopy.org</a>)
     * @return a CompletableFuture with the request handler, an empty Optional if an error occurred
     */
    public static CompletableFuture<Optional<RequestsHandler>> create(URI host) {
        RequestsHandler requestsHandler = new RequestsHandler(host);

        return JsonApi.createJsonApi(requestsHandler, host).thenApply(jsonApi -> {
            if (jsonApi.isPresent()) {
                requestsHandler.jsonApi = jsonApi.get();
                return Optional.of(requestsHandler);
            } else {
                return Optional.empty();
            }
        });
    }

    /**
     * @return the base server URI (e.g. <a href="https://idr.openmicroscopy.org">https://idr.openmicroscopy.org</a>)
     */
    public URI getHost() {
        return host;
    }

    /**
     * See {@link qupath.lib.images.servers.omero.common.api.requests.apis.WebclientApi#getItemURI(ServerEntity)}.
     */
    public String getItemURI(ServerEntity serverEntity) {
        return webclientApi.getItemURI(serverEntity);
    }

    /**
     * See {@link qupath.lib.images.servers.omero.common.api.requests.apis.JsonApi#getNumberOfEntitiesLoading}.
     */
    public ReadOnlyIntegerProperty getNumberOfEntitiesLoading() {
        return jsonApi.getNumberOfEntitiesLoading();
    }

    /**
     * See {@link qupath.lib.images.servers.omero.common.api.requests.apis.JsonApi#getOrphanedImagesLoading()}.
     */
    public ReadOnlyBooleanProperty getOrphanedImagesLoading() {
        return jsonApi.getOrphanedImagesLoading();
    }

    /**
     * See {@link qupath.lib.images.servers.omero.common.api.requests.apis.JsonApi#getNumberOfOrphanedImagesLoaded()}.
     */
    public ReadOnlyIntegerProperty getNumberOfOrphanedImagesLoaded() {
        return jsonApi.getNumberOfOrphanedImagesLoaded();
    }

    /**
     * See {@link qupath.lib.images.servers.omero.common.api.requests.apis.JsonApi#getNumberOfOrphanedImages()}.
     */
    public ReadOnlyIntegerProperty getNumberOfOrphanedImages() {
        return jsonApi.getNumberOfOrphanedImages();
    }

    /**
     * See {@link qupath.lib.images.servers.omero.common.api.requests.apis.WebGatewayApi#getNumberOfThumbnailsLoading()}.
     */
    public ReadOnlyIntegerProperty getNumberOfThumbnailsLoading() {
        return webGatewayApi.getNumberOfThumbnailsLoading();
    }

    /**
     * See {@link qupath.lib.images.servers.omero.common.api.requests.apis.JsonApi#login(String...)}.
     */
    public CompletableFuture<LoginResponse> login(String... args) {
        return jsonApi.login(args);
    }

    /**
     * See {@link qupath.lib.images.servers.omero.common.api.requests.apis.WebclientApi#logout(String)}.
     */
    public void logout() {
        webclientApi.logout(jsonApi.getToken());
    }

    /**
     * See {@link qupath.lib.images.servers.omero.common.api.requests.apis.WebclientApi#ping()}.
     */
    public CompletableFuture<Boolean> ping() {
        return webclientApi.ping();
    }

    /**
     * See {@link qupath.lib.images.servers.omero.common.api.requests.apis.WebclientApi#getOrphanedImagesIds()}.
     */
    public CompletableFuture<List<Integer>> getOrphanedImagesIds() {
        return webclientApi.getOrphanedImagesIds();
    }

    /**
     * See {@link qupath.lib.images.servers.omero.common.api.requests.apis.JsonApi#getOrphanedImagesURIs()}.
     */
    public CompletableFuture<List<URI>> getOrphanedImagesURIs() {
        return jsonApi.getOrphanedImagesURIs();
    }

    /**
     * See {@link qupath.lib.images.servers.omero.common.api.requests.apis.JsonApi#getProjects()}.
     */
    public CompletableFuture<List<ServerEntity>> getProjects() {
        return jsonApi.getProjects();
    }

    /**
     * See {@link qupath.lib.images.servers.omero.common.api.requests.apis.JsonApi#getOrphanedDatasets()}.
     */
    public CompletableFuture<List<ServerEntity>> getOrphanedDatasets() {
        return jsonApi.getOrphanedDatasets();
    }

    /**
     * See {@link qupath.lib.images.servers.omero.common.api.requests.apis.JsonApi#getDatasets(Project)}.
     */
    public CompletableFuture<List<ServerEntity>> getDatasets(Project project) {
        return jsonApi.getDatasets(project);
    }

    /**
     * See {@link qupath.lib.images.servers.omero.common.api.requests.apis.JsonApi#getImages(Dataset)}.
     */
    public CompletableFuture<List<ServerEntity>> getImages(Dataset dataset) {
        return jsonApi.getImages(dataset);
    }

    /**
     * See {@link qupath.lib.images.servers.omero.common.api.requests.apis.JsonApi#populateOrphanedImagesIntoList(List)}.
     */
    public void populateOrphanedImagesIntoList(List<RepositoryEntity> children) {
        jsonApi.populateOrphanedImagesIntoList(children);
    }

    /**
     * See {@link qupath.lib.images.servers.omero.common.api.requests.apis.WebclientApi#getAnnotations(ServerEntity)}.
     */
    public CompletableFuture<Optional<AnnotationGroup>> getAnnotations(ServerEntity serverEntity) {
        return webclientApi.getAnnotations(serverEntity);
    }

    /**
     * See {@link qupath.lib.images.servers.omero.common.api.requests.apis.WebclientApi#getSearchResults(SearchQuery)}.
     */
    public CompletableFuture<List<SearchResult>> getSearchResults(SearchQuery searchQuery) {
        return webclientApi.getSearchResults(searchQuery);
    }

    /**
     * See {@link qupath.lib.images.servers.omero.common.api.requests.apis.WebGatewayApi#getProjectIcon()}.
     */
    public CompletableFuture<Optional<BufferedImage>> getProjectIcon() {
        return webGatewayApi.getProjectIcon();
    }

    /**
     * See {@link qupath.lib.images.servers.omero.common.api.requests.apis.WebGatewayApi#getDatasetIcon()}.
     */
    public CompletableFuture<Optional<BufferedImage>> getDatasetIcon() {
        return webGatewayApi.getDatasetIcon();
    }

    /**
     * See {@link qupath.lib.images.servers.omero.common.api.requests.apis.WebGatewayApi#getOrphanedFolderIcon()}.
     */
    public CompletableFuture<Optional<BufferedImage>> getOrphanedFolderIcon() {
        return webGatewayApi.getOrphanedFolderIcon();
    }

    /**
     * See {@link qupath.lib.images.servers.omero.common.api.requests.apis.WebclientApi#getImageIcon()}.
     */
    public CompletableFuture<Optional<BufferedImage>> getImageIcon() {
        return webclientApi.getImageIcon();
    }

    /**
     * See {@link qupath.lib.images.servers.omero.common.api.requests.apis.WebGatewayApi#getThumbnail(int, int)}.
     */
    public CompletableFuture<Optional<BufferedImage>> getThumbnail(int id, int size) {
        return webGatewayApi.getThumbnail(id, size);
    }

    /**
     * See {@link qupath.lib.images.servers.omero.common.api.requests.apis.JsonApi#canSkipAuthentication()}.
     */
    public CompletableFuture<Boolean> canSkipAuthentication() {
        return jsonApi.canSkipAuthentication();
    }

    /**
     * See {@link qupath.lib.images.servers.omero.common.api.requests.apis.WebGatewayApi#readOneDimensionalTile(Long, TileRequest, int, int, double, boolean)}.
     */
    public CompletableFuture<Optional<BufferedImage>> readOneDimensionalTile(
            Long id,
            TileRequest tileRequest,
            int preferredTileWidth,
            int preferredTileHeight,
            double quality,
            boolean allowSmoothInterpolation
    ) {
        return webGatewayApi.readOneDimensionalTile(id, tileRequest, preferredTileWidth, preferredTileHeight, quality, allowSmoothInterpolation);
    }

    /**
     * See {@link qupath.lib.images.servers.omero.common.api.requests.apis.WebGatewayApi#readMultiDimensionalTile(Long, TileRequest, int, int, double)}.
     */
    public CompletableFuture<Optional<BufferedImage>> readMultiDimensionalTile(
            Long id,
            TileRequest tileRequest,
            int preferredTileWidth,
            int preferredTileHeight,
            double quality
    ) {
        return webGatewayApi.readMultiDimensionalTile(id, tileRequest, preferredTileWidth, preferredTileHeight, quality);
    }

    /**
     * See {@link qupath.lib.images.servers.omero.common.api.requests.apis.WebGatewayApi#getImageMetadata(long)}.
     */
    public CompletableFuture<Optional<ImageMetadataResponse>> getImageMetadata(long id) {
        return webGatewayApi.getImageMetadata(id);
    }

    /**
     * See {@link qupath.lib.images.servers.omero.common.api.requests.apis.JsonApi#getROIs(long)}.
     */
    public CompletableFuture<List<PathObject>> getROIs(long id) {
        return jsonApi.getROIs(id);
    }

    /**
     * See {@link qupath.lib.images.servers.omero.common.api.requests.apis.IViewerApi#writeROIs(long, Collection, String)}.
     */
    public CompletableFuture<Boolean> writeROIs(long id, Collection<PathObject> rois) {
        return iViewerApi.writeROIs(id, rois, jsonApi.getToken());
    }
}
