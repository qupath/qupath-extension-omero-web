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

    public String getItemURI(ServerEntity serverEntity) {
        return webclientApi.getItemURI(serverEntity);
    }

    public ReadOnlyBooleanProperty getOrphanedImagesLoading() {
        return jsonApi.getOrphanedImagesLoading();
    }

    public ReadOnlyIntegerProperty getNumberOfEntitiesLoading() {
        return jsonApi.getNumberOfEntitiesLoading();
    }

    public ReadOnlyIntegerProperty getNumberOfOrphanedImagesLoaded() {
        return jsonApi.getNumberOfOrphanedImagesLoaded();
    }

    public ReadOnlyIntegerProperty getNumberOfOrphanedImages() {
        return jsonApi.getNumberOfOrphanedImages();
    }

    public ReadOnlyIntegerProperty getNumberOfThumbnailsLoading() {
        return webGatewayApi.getNumberOfThumbnailsLoading();
    }

    public CompletableFuture<LoginResponse> login(String... args) {
        return jsonApi.login(args);
    }

    public void logout() {
        webclientApi.logout(jsonApi.getToken());
    }

    public CompletableFuture<Boolean> ping() {
        return webclientApi.ping();
    }

    public CompletableFuture<List<Integer>> getOrphanedImagesIds() {
        return webclientApi.getOrphanedImagesIds();
    }

    public CompletableFuture<List<URI>> getOrphanedImagesURIs() {
        return jsonApi.getOrphanedImagesURIs();
    }

    public CompletableFuture<List<ServerEntity>> getProjects() {
        return jsonApi.getProjects();
    }

    public CompletableFuture<List<ServerEntity>> getOrphanedDatasets() {
        return jsonApi.getOrphanedDatasets();
    }

    public CompletableFuture<List<ServerEntity>> getDatasets(Project project) {
        return jsonApi.getDatasets(project);
    }

    public CompletableFuture<List<ServerEntity>> getImages(Dataset dataset) {
        return jsonApi.getImages(dataset);
    }

    public void populateOrphanedImagesIntoList(List<RepositoryEntity> children) {
        jsonApi.populateOrphanedImagesIntoList(children);
    }

    public CompletableFuture<Optional<AnnotationGroup>> getAnnotations(ServerEntity serverEntity) {
        return webclientApi.getAnnotations(serverEntity);
    }

    public CompletableFuture<List<SearchResult>> getSearchResults(SearchQuery searchQuery) {
        return webclientApi.getSearchResults(searchQuery);
    }

    public CompletableFuture<Optional<BufferedImage>> getProjectIcon() {
        return webGatewayApi.getProjectIcon();
    }

    public CompletableFuture<Optional<BufferedImage>> getDatasetIcon() {
        return webGatewayApi.getDatasetIcon();
    }

    public CompletableFuture<Optional<BufferedImage>> getOrphanedFolderIcon() {
        return webGatewayApi.getOrphanedFolderIcon();
    }

    public CompletableFuture<Optional<BufferedImage>> getImageIcon() {
        return webclientApi.getImageIcon();
    }

    public CompletableFuture<Optional<BufferedImage>> getThumbnail(int id, int size) {
        return webGatewayApi.getThumbnail(id, size);
    }

    public CompletableFuture<Boolean> canSkipAuthentication() {
        return jsonApi.canSkipAuthentication();
    }

    public CompletableFuture<Optional<BufferedImage>> readOneDimensionalTile(Long id, TileRequest tileRequest, int preferredTileWidth, int preferredTileHeight, double quality, boolean allowSmoothInterpolation) {
        return webGatewayApi.readOneDimensionalTile(id, tileRequest, preferredTileWidth, preferredTileHeight, quality, allowSmoothInterpolation);
    }

    public CompletableFuture<Optional<BufferedImage>> readMultiDimensionalTile(Long id, TileRequest tileRequest, int preferredTileWidth, int preferredTileHeight, double quality) {
        return webGatewayApi.readMultiDimensionalTile(id, tileRequest, preferredTileWidth, preferredTileHeight, quality);
    }

    public CompletableFuture<Optional<ImageMetadataResponse>> getImageMetadata(long id) {
        return webGatewayApi.getImageMetadata(id);
    }

    public CompletableFuture<List<PathObject>> getROIs(long id) {
        return jsonApi.getROIs(id);
    }

    public CompletableFuture<Boolean> writeROIs(long id, Collection<PathObject> rois) {
        return iViewerApi.writeROIs(id, rois, jsonApi.getToken());
    }
}
