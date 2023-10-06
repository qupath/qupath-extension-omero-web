package qupath.lib.images.servers.omero.web.apis;

import javafx.beans.property.*;
import qupath.lib.images.servers.*;
import qupath.lib.images.servers.omero.web.WebClient;
import qupath.lib.images.servers.omero.web.WebUtilities;
import qupath.lib.images.servers.omero.web.entities.imagemetadata.ImageMetadataResponse;
import qupath.lib.images.servers.omero.web.entities.login.LoginResponse;
import qupath.lib.images.servers.omero.web.entities.permissions.Group;
import qupath.lib.images.servers.omero.web.entities.permissions.Owner;
import qupath.lib.images.servers.omero.web.entities.repositoryentities.serverentities.image.Image;
import qupath.lib.images.servers.omero.web.entities.search.SearchQuery;
import qupath.lib.images.servers.omero.web.entities.search.SearchResult;
import qupath.lib.images.servers.omero.web.entities.annotations.AnnotationGroup;
import qupath.lib.images.servers.omero.web.entities.repositoryentities.RepositoryEntity;
import qupath.lib.images.servers.omero.web.entities.repositoryentities.serverentities.ServerEntity;
import qupath.lib.objects.PathObject;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * <p>This class provides functions to perform operations with an OMERO server.</p>
 * <p>
 *     As different APIs are used to perform the operations, this class only
 *     redirect each web request to the appropriate API contained in this package.
 * </p>
 * <p>An instance of this class must be {@link #close() closed} once no longer used.</p>
 */
public class ApisHandler implements AutoCloseable {

    private final WebClient client;
    private final URI host;
    private final WebclientApi webclientApi;
    private final WebGatewayApi webGatewayApi;
    private final IViewerApi iViewerApi;
    private JsonApi jsonApi;

    private ApisHandler(WebClient client, URI host) {
        this.client = client;
        this.host = host;

        webclientApi = new WebclientApi(host);
        webGatewayApi = new WebGatewayApi(host);
        iViewerApi = new IViewerApi(host);
    }

    /**
     * <p>Attempt to create a request handler.</p>
     * <p>This function is asynchronous.</p>
     *
     * @param client  the corresponding web client
     * @param host  the base server URI (e.g. <a href="https://idr.openmicroscopy.org">https://idr.openmicroscopy.org</a>)
     * @return a CompletableFuture with the request handler, an empty Optional if an error occurred
     */
    public static CompletableFuture<Optional<ApisHandler>> create(WebClient client, URI host) {
        ApisHandler apisHandler = new ApisHandler(client, host);

        return JsonApi.create(apisHandler, host).thenApply(jsonApi -> {
            if (jsonApi.isPresent()) {
                apisHandler.jsonApi = jsonApi.get();
                apisHandler.webclientApi.setToken(jsonApi.get().getToken());
                return Optional.of(apisHandler);
            } else {
                return Optional.empty();
            }
        });
    }

    @Override
    public void close() throws Exception {
        webclientApi.close();
    }

    @Override
    public String toString() {
        return String.format("APIs handler of %s", host);
    }

    /**
     * @return the web client using this APIs handler
     */
    public WebClient getClient() {
        return client;
    }

    /**
     * @return the base server URI (e.g. <a href="https://idr.openmicroscopy.org">https://idr.openmicroscopy.org</a>)
     */
    public URI getHost() {
        return host;
    }

    /**
     * @return the server port of this session
     */
    public int getPort() {
        return jsonApi.getPort();
    }

    /**
     * <p>Returns a list of image URIs contained in the dataset identified by the provided ID.</p>
     * <p>This function is asynchronous.</p>
     *
     * @param datasetID  the ID of the dataset the returned images must belong to
     * @return a list of URIs of images contained in the dataset
     */
    public CompletableFuture<List<URI>> getImagesURIOfDataset(long datasetID) {
        return getImages(datasetID).thenApply(images -> images.stream()
                .map(this::getItemURI)
                .map(WebUtilities::createURI)
                .flatMap(Optional::stream)
                .toList()
        );
    }

    /**
     * <p>Returns a list of image URIs contained in the project identified by the provided ID.</p>
     * <p>This function is asynchronous.</p>
     *
     * @param projectID  the ID of the project the returned images must belong to
     * @return a list of URIs of images contained in the project
     */
    public CompletableFuture<List<URI>> getImagesURIOfProject(long projectID) {
        return getDatasets(projectID).thenApplyAsync(datasets -> datasets.stream()
                .map(dataset -> getImagesURIOfDataset(dataset.getId()))
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .toList());
    }

    /**
     * See {@link WebclientApi#getItemURI(ServerEntity)}.
     */
    public String getItemURI(ServerEntity serverEntity) {
        return webclientApi.getItemURI(serverEntity);
    }

    /**
     * See {@link JsonApi#getNumberOfEntitiesLoading}.
     */
    public ReadOnlyIntegerProperty getNumberOfEntitiesLoading() {
        return jsonApi.getNumberOfEntitiesLoading();
    }

    /**
     * See {@link JsonApi#getOrphanedImagesLoading()}.
     */
    public ReadOnlyBooleanProperty getOrphanedImagesLoading() {
        return jsonApi.getOrphanedImagesLoading();
    }

    /**
     * See {@link JsonApi#getNumberOfOrphanedImagesLoaded()}.
     */
    public ReadOnlyIntegerProperty getNumberOfOrphanedImagesLoaded() {
        return jsonApi.getNumberOfOrphanedImagesLoaded();
    }

    /**
     * See {@link JsonApi#getNumberOfOrphanedImages()}.
     */
    public ReadOnlyIntegerProperty getNumberOfOrphanedImages() {
        return jsonApi.getNumberOfOrphanedImages();
    }

    /**
     * See {@link WebGatewayApi#getNumberOfThumbnailsLoading()}.
     */
    public ReadOnlyIntegerProperty getNumberOfThumbnailsLoading() {
        return webGatewayApi.getNumberOfThumbnailsLoading();
    }

    /**
     * See {@link JsonApi#login(String...)}.
     */
    public CompletableFuture<LoginResponse> login(String... args) {
        return jsonApi.login(args);
    }

    /**
     * See {@link WebclientApi#ping()}.
     */
    public CompletableFuture<Boolean> ping() {
        return webclientApi.ping();
    }

    /**
     * See {@link WebclientApi#getOrphanedImagesIds()}.
     */
    public CompletableFuture<List<Integer>> getOrphanedImagesIds() {
        return webclientApi.getOrphanedImagesIds();
    }

    /**
     * See {@link JsonApi#getOrphanedImagesURIs()}.
     */
    public CompletableFuture<List<URI>> getOrphanedImagesURIs() {
        return jsonApi.getOrphanedImagesURIs();
    }

    /**
     * See {@link JsonApi#getGroups()} ()}.
     */
    public CompletableFuture<List<Group>> getGroups() {
        return jsonApi.getGroups();
    }

    /**
     * See {@link JsonApi#getOwners()} ()} ()}.
     */
    public CompletableFuture<List<Owner>> getOwners() {
        return jsonApi.getOwners();
    }

    /**
     * See {@link JsonApi#getProjects()}.
     */
    public CompletableFuture<List<ServerEntity>> getProjects() {
        return jsonApi.getProjects();
    }

    /**
     * See {@link JsonApi#getOrphanedDatasets()}.
     */
    public CompletableFuture<List<ServerEntity>> getOrphanedDatasets() {
        return jsonApi.getOrphanedDatasets();
    }

    /**
     * See {@link JsonApi#getDatasets(long)}.
     */
    public CompletableFuture<List<ServerEntity>> getDatasets(long projectID) {
        return jsonApi.getDatasets(projectID);
    }

    /**
     * See {@link JsonApi#getImages(long)}.
     */
    public CompletableFuture<List<ServerEntity>> getImages(long datasetID) {
        return jsonApi.getImages(datasetID);
    }

    /**
     * See {@link JsonApi#getImage(long)}.
     */
    public CompletableFuture<Optional<Image>> getImage(long imageID) {
        return jsonApi.getImage(imageID);
    }

    /**
     * See {@link JsonApi#populateOrphanedImagesIntoList(List)}.
     */
    public void populateOrphanedImagesIntoList(List<RepositoryEntity> children) {
        jsonApi.populateOrphanedImagesIntoList(children);
    }

    /**
     * See {@link WebclientApi#getAnnotations(ServerEntity)}.
     */
    public CompletableFuture<Optional<AnnotationGroup>> getAnnotations(ServerEntity serverEntity) {
        return webclientApi.getAnnotations(serverEntity);
    }

    /**
     * See {@link WebclientApi#getSearchResults(SearchQuery)}.
     */
    public CompletableFuture<List<SearchResult>> getSearchResults(SearchQuery searchQuery) {
        return webclientApi.getSearchResults(searchQuery);
    }

    /**
     * See {@link WebGatewayApi#getProjectIcon()}.
     */
    public CompletableFuture<Optional<BufferedImage>> getProjectIcon() {
        return webGatewayApi.getProjectIcon();
    }

    /**
     * See {@link WebGatewayApi#getDatasetIcon()}.
     */
    public CompletableFuture<Optional<BufferedImage>> getDatasetIcon() {
        return webGatewayApi.getDatasetIcon();
    }

    /**
     * See {@link WebGatewayApi#getOrphanedFolderIcon()}.
     */
    public CompletableFuture<Optional<BufferedImage>> getOrphanedFolderIcon() {
        return webGatewayApi.getOrphanedFolderIcon();
    }

    /**
     * See {@link WebclientApi#getImageIcon()}.
     */
    public CompletableFuture<Optional<BufferedImage>> getImageIcon() {
        return webclientApi.getImageIcon();
    }

    /**
     * See {@link WebGatewayApi#getThumbnail(long, int)}.
     */
    public CompletableFuture<Optional<BufferedImage>> getThumbnail(long id, int size) {
        return webGatewayApi.getThumbnail(id, size);
    }

    /**
     * See {@link JsonApi#canSkipAuthentication()}.
     */
    public CompletableFuture<Boolean> canSkipAuthentication() {
        return jsonApi.canSkipAuthentication();
    }

    /**
     * See {@link WebGatewayApi#readSingleResolutionTile(Long, TileRequest, int, int, double, boolean)}.
     */
    public CompletableFuture<Optional<BufferedImage>> readSingleResolutionTile(
            Long id,
            TileRequest tileRequest,
            int preferredTileWidth,
            int preferredTileHeight,
            double quality,
            boolean allowSmoothInterpolation
    ) {
        return webGatewayApi.readSingleResolutionTile(id, tileRequest, preferredTileWidth, preferredTileHeight, quality, allowSmoothInterpolation);
    }

    /**
     * See {@link WebGatewayApi#readMultiResolutionTile(Long, TileRequest, int, int, double)}.
     */
    public CompletableFuture<Optional<BufferedImage>> readMultiResolutionTile(
            Long id,
            TileRequest tileRequest,
            int preferredTileWidth,
            int preferredTileHeight,
            double quality
    ) {
        return webGatewayApi.readMultiResolutionTile(id, tileRequest, preferredTileWidth, preferredTileHeight, quality);
    }

    /**
     * See {@link WebGatewayApi#getImageMetadata(long)}.
     */
    public CompletableFuture<Optional<ImageMetadataResponse>> getImageMetadata(long id) {
        return webGatewayApi.getImageMetadata(id);
    }

    /**
     * See {@link JsonApi#getROIs(long)}.
     */
    public CompletableFuture<List<PathObject>> getROIs(long id) {
        return jsonApi.getROIs(id);
    }

    /**
     * See {@link IViewerApi#writeROIs(long, Collection, String)}.
     */
    public CompletableFuture<Boolean> writeROIs(long id, Collection<PathObject> rois) {
        return iViewerApi.writeROIs(id, rois, jsonApi.getToken());
    }
}
