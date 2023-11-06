package qupath.ext.omero.core.apis;

import javafx.collections.ObservableList;
import org.junit.jupiter.api.*;
import qupath.ext.omero.TestUtilities;
import qupath.ext.omero.core.OmeroServer;
import qupath.ext.omero.core.WebClient;
import qupath.ext.omero.core.WebClients;
import qupath.ext.omero.core.entities.annotations.AnnotationGroup;
import qupath.ext.omero.core.entities.permissions.Group;
import qupath.ext.omero.core.entities.permissions.Owner;
import qupath.ext.omero.core.entities.repositoryentities.OrphanedFolder;
import qupath.ext.omero.core.entities.repositoryentities.RepositoryEntity;
import qupath.ext.omero.core.entities.repositoryentities.Server;
import qupath.ext.omero.core.entities.repositoryentities.serverentities.Dataset;
import qupath.ext.omero.core.entities.repositoryentities.serverentities.Project;
import qupath.ext.omero.core.entities.repositoryentities.serverentities.ServerEntity;
import qupath.ext.omero.core.entities.repositoryentities.serverentities.image.Image;
import qupath.ext.omero.core.entities.search.SearchQuery;
import qupath.ext.omero.core.entities.search.SearchResult;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class TestApisHandler extends OmeroServer {

    private static WebClient client;
    private static ApisHandler apisHandler;

    @BeforeAll
    static void createClient() throws ExecutionException, InterruptedException {
        client = OmeroServer.createValidClient();
        apisHandler = client.getApisHandler();
    }

    @AfterAll
    static void removeClient() {
        WebClients.removeClient(client);
    }

    @Test
    void Check_Host() {
        URI expectedHost = URI.create(OmeroServer.getServerURL());

        URI host = apisHandler.getHost();

        Assertions.assertEquals(expectedHost, host);
    }

    @Test
    void Check_Port() {
        int expectedPort = OmeroServer.getPort();

        int port = apisHandler.getPort();

        Assertions.assertEquals(expectedPort, port);
    }

    @Test
    void Check_Image_URI_Of_Dataset() throws ExecutionException, InterruptedException {
        long datasetID = OmeroServer.getDataset().getId();
        List<URI> expectedURIs = List.of(OmeroServer.getImageURI());

        List<URI> uris = apisHandler.getImagesURIOfDataset(datasetID).get();

        TestUtilities.assertListEqualsWithoutOrder(expectedURIs, uris);
    }

    @Test
    void Check_Image_URI_Of_Invalid_Dataset() throws ExecutionException, InterruptedException {
        long datasetID = -1;
        List<URI> expectedURIs = List.of();

        List<URI> uris = apisHandler.getImagesURIOfDataset(datasetID).get();

        TestUtilities.assertListEqualsWithoutOrder(expectedURIs, uris);
    }

    @Test
    void Check_Image_URI_Of_Project() throws ExecutionException, InterruptedException {
        long projectID = OmeroServer.getProject().getId();
        List<URI> expectedURIs = List.of(OmeroServer.getImageURI());

        List<URI> uris = apisHandler.getImagesURIOfProject(projectID).get();

        TestUtilities.assertListEqualsWithoutOrder(expectedURIs, uris);
    }

    @Test
    void Check_Image_URI_Of_Invalid_Project() throws ExecutionException, InterruptedException {
        long projectID = -1;
        List<URI> expectedURIs = List.of();

        List<URI> uris = apisHandler.getImagesURIOfProject(projectID).get();

        TestUtilities.assertListEqualsWithoutOrder(expectedURIs, uris);
    }

    @Test
    void Check_Image_URI() {
        Image image = OmeroServer.getImage();
        String expectedURI = OmeroServer.getImageURI().toString();

        String uri = apisHandler.getItemURI(image);

        Assertions.assertEquals(expectedURI, uri);
    }

    @Test
    void Check_Dataset_URI() {
        Dataset dataset = OmeroServer.getDataset();
        String expectedURI = OmeroServer.getDatasetURI();

        String uri = apisHandler.getItemURI(dataset);

        Assertions.assertEquals(expectedURI, uri);
    }

    @Test
    void Check_Project_URI() {
        Project project = OmeroServer.getProject();
        String expectedURI = OmeroServer.getProjectURI();

        String uri = apisHandler.getItemURI(project);

        Assertions.assertEquals(expectedURI, uri);
    }

    @Test
    void Check_Invalid_Entity_URI() {
        ServerEntity serverEntity = new ServerEntityImplementation();

        Assertions.assertThrows(IllegalArgumentException.class, () ->
                apisHandler.getItemURI(serverEntity)
        );
    }

    @Test
    void Check_Ping() throws ExecutionException, InterruptedException {
        boolean pingSucceeded = apisHandler.ping().get();

        Assertions.assertTrue(pingSucceeded);
    }

    @Test
    void Check_Orphaned_Images_Id() throws ExecutionException, InterruptedException {
        List<Long> expectedIds = List.of(OmeroServer.getOrphanedImage().getId());

        List<Long> ids = apisHandler.getOrphanedImagesIds().get();

        TestUtilities.assertListEqualsWithoutOrder(expectedIds, ids);
    }

    @Test
    void Check_Groups() throws ExecutionException, InterruptedException {
        List<Group> expectedGroups = OmeroServer.getGroups();

        List<Group> groups = apisHandler.getGroups().get();

        TestUtilities.assertListEqualsWithoutOrder(expectedGroups, groups);
    }

    @Test
    void Check_Owners() throws ExecutionException, InterruptedException {
        List<Owner> expectedOwners = OmeroServer.getOwners();

        List<Owner> owners = apisHandler.getOwners().get();

        TestUtilities.assertListEqualsWithoutOrder(expectedOwners, owners);
    }

    @Test
    void Check_Projects() throws ExecutionException, InterruptedException {
        List<Project> expectedProjects = List.of(OmeroServer.getProject());

        List<Project> projects = apisHandler.getProjects().get();

        TestUtilities.assertListEqualsWithoutOrder(expectedProjects, projects);
    }

    @Test
    void Check_Orphaned_Datasets() throws ExecutionException, InterruptedException {
        List<Dataset> expectedOrphanedDatasets = List.of(OmeroServer.getOrphanedDataset());

        List<Dataset> orphanedDatasets = apisHandler.getOrphanedDatasets().get();

        TestUtilities.assertListEqualsWithoutOrder(expectedOrphanedDatasets, orphanedDatasets);
    }

    @Test
    void Check_Datasets() throws ExecutionException, InterruptedException {
        long projectID = OmeroServer.getProject().getId();
        List<Dataset> expectedDatasets = List.of(OmeroServer.getDataset());

        List<Dataset> datasets = apisHandler.getDatasets(projectID).get();

        TestUtilities.assertListEqualsWithoutOrder(expectedDatasets, datasets);
    }

    @Test
    void Check_Datasets_Of_Invalid_Project() throws ExecutionException, InterruptedException {
        long invalidProjectID = -1;
        List<Dataset> expectedDatasets = List.of();

        List<Dataset> datasets = apisHandler.getDatasets(invalidProjectID).get();

        TestUtilities.assertListEqualsWithoutOrder(expectedDatasets, datasets);
    }

    @Test
    void Check_Images() throws ExecutionException, InterruptedException {
        long datasetID = OmeroServer.getDataset().getId();
        List<Image> expectedImages = List.of(OmeroServer.getImage());

        List<Image> images = apisHandler.getImages(datasetID).get();

        TestUtilities.assertListEqualsWithoutOrder(expectedImages, images);
    }

    @Test
    void Check_Images_Of_Invalid_Dataset() throws ExecutionException, InterruptedException {
        long invalidDatasetID = -1;
        List<Image> expectedImages = List.of();

        List<Image> images = apisHandler.getImages(invalidDatasetID).get();

        TestUtilities.assertListEqualsWithoutOrder(expectedImages, images);
    }

    @Test
    void Check_Image() throws ExecutionException, InterruptedException {
        Image expectedImage = OmeroServer.getImage();
        long imageID = expectedImage.getId();

        Image image = apisHandler.getImage(imageID).get().orElse(null);

        Assertions.assertEquals(expectedImage, image);
    }

    @Test
    void Check_Image_With_Invalid_ID() throws ExecutionException, InterruptedException {
        long imageID = -1;

        Image image = apisHandler.getImage(imageID).get().orElse(null);

        Assertions.assertNull(image);
    }

    @Test
    void Check_Number_Of_Orphaned_Image() throws ExecutionException, InterruptedException {
        int expectedNumberOfOrphanedImages = 1;

        int numberOfOrphanedImages = apisHandler.getNumberOfOrphanedImages().get();

        Assertions.assertEquals(expectedNumberOfOrphanedImages, numberOfOrphanedImages);
    }

    @Test
    void Check_Loading_Orphaned_Images() throws InterruptedException {
        List<Image> images = new ArrayList<>();
        List<Image> expectedImages = List.of(OmeroServer.getOrphanedImage());

        apisHandler.populateOrphanedImagesIntoList(images);
        while (apisHandler.areOrphanedImagesLoading().get()) {
            TimeUnit.MILLISECONDS.sleep(50);
        }

        TestUtilities.assertListEqualsWithoutOrder(expectedImages, images);
    }

    @Test
    void Check_Number_Of_Loaded_Orphaned_Images() throws InterruptedException {
        List<Image> images = new ArrayList<>();
        int expectedNumberOfImages = 1;

        apisHandler.populateOrphanedImagesIntoList(images);
        while (apisHandler.areOrphanedImagesLoading().get()) {
            TimeUnit.MILLISECONDS.sleep(50);
        }
        int numberOfImages = apisHandler.getNumberOfOrphanedImagesLoaded().get();

        Assertions.assertEquals(expectedNumberOfImages, numberOfImages);
    }

     @Test
    void Check_Annotations() throws ExecutionException, InterruptedException {
        AnnotationGroup expectedAnnotationGroup = OmeroServer.getDatasetAnnotationGroup();

        AnnotationGroup annotationGroup = apisHandler.getAnnotations(OmeroServer.getDataset()).get().orElse(null);

        Assertions.assertEquals(expectedAnnotationGroup, annotationGroup);
    }

    @Test
    void Check_Search() throws ExecutionException, InterruptedException {
        SearchQuery searchQuery = new SearchQuery(
                "dataset",
                false,
                false,
                true,
                true,
                true,
                true,
                true,
                true,
                Group.getAllGroupsGroup(),
                Owner.getAllMembersOwner()
        );
        List<SearchResult> expectedResults = OmeroServer.getSearchResultsOnDataset();

        List<SearchResult> searchResults = apisHandler.getSearchResults(searchQuery).get();

        TestUtilities.assertListEqualsWithoutOrder(expectedResults, searchResults);
    }

    @Test
    void Check_Dataset_Icon() throws ExecutionException, InterruptedException {
        Class<? extends RepositoryEntity> type = Dataset.class;

        BufferedImage icon = apisHandler.getOmeroIcon(type).get().orElse(null);

        Assertions.assertNotNull(icon);
    }

    @Test
    void Check_Image_Icon() throws ExecutionException, InterruptedException {
        Class<? extends RepositoryEntity> type = Image.class;

        BufferedImage icon = apisHandler.getOmeroIcon(type).get().orElse(null);

        Assertions.assertNotNull(icon);
    }

    @Test
    void Check_Orphaned_Folder_Icon() throws ExecutionException, InterruptedException {
        Class<? extends RepositoryEntity> type = OrphanedFolder.class;

        BufferedImage icon = apisHandler.getOmeroIcon(type).get().orElse(null);

        Assertions.assertNotNull(icon);
    }

    @Test
    void Check_Project_Icon() throws ExecutionException, InterruptedException {
        Class<? extends RepositoryEntity> type = Project.class;

        BufferedImage icon = apisHandler.getOmeroIcon(type).get().orElse(null);

        Assertions.assertNotNull(icon);
    }

    @Test
    void Check_Server_Icon() throws ExecutionException, InterruptedException {
        Class<? extends RepositoryEntity> type = Server.class;

        BufferedImage icon = apisHandler.getOmeroIcon(type).get().orElse(null);

        Assertions.assertNull(icon);    // there is no icon for the server
    }

    @Test
    void Check_Image_Thumbnail() throws ExecutionException, InterruptedException {
        long imageId = OmeroServer.getImage().getId();

        BufferedImage image = apisHandler.getThumbnail(imageId).get().orElse(null);

        Assertions.assertNotNull(image);
    }

    @Test
    void Check_Image_Thumbnail_With_Specific_Size() throws ExecutionException, InterruptedException {
        long imageId = OmeroServer.getImage().getId();
        int size = 30;

        BufferedImage image = apisHandler.getThumbnail(imageId, size).get().orElse(null);

        Assertions.assertNotNull(image);
        Assertions.assertEquals(size, Math.max(image.getWidth(), image.getHeight()));
    }

    @Test
    void Check_Image_Thumbnail_With_Invalid_Image_ID() throws ExecutionException, InterruptedException {
        long imageId = -1;

        BufferedImage image = apisHandler.getThumbnail(imageId).get().orElse(null);

        Assertions.assertNull(image);
    }

    private static class ServerEntityImplementation extends ServerEntity {

        @Override
        public int getNumberOfChildren() {
            return 0;
        }

        @Override
        public ObservableList<? extends RepositoryEntity> getChildren() {
            return null;
        }

        @Override
        public boolean isPopulatingChildren() {
            return false;
        }

        @Override
        public String getAttributeName(int informationIndex) {
            return null;
        }

        @Override
        public String getAttributeValue(int informationIndex) {
            return null;
        }

        @Override
        public int getNumberOfAttributes() {
            return 0;
        }
    }
}


