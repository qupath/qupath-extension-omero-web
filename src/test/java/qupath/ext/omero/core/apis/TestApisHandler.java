package qupath.ext.omero.core.apis;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.collections.ObservableList;
import org.junit.jupiter.api.*;
import qupath.ext.omero.TestUtilities;
import qupath.ext.omero.OmeroServer;
import qupath.ext.omero.core.WebClient;
import qupath.ext.omero.core.WebClients;
import qupath.ext.omero.core.entities.annotations.AnnotationGroup;
import qupath.ext.omero.core.entities.imagemetadata.ImageMetadataResponse;
import qupath.ext.omero.core.entities.permissions.Group;
import qupath.ext.omero.core.entities.permissions.Owner;
import qupath.ext.omero.core.entities.repositoryentities.OrphanedFolder;
import qupath.ext.omero.core.entities.repositoryentities.RepositoryEntity;
import qupath.ext.omero.core.entities.repositoryentities.Server;
import qupath.ext.omero.core.entities.repositoryentities.serverentities.*;
import qupath.ext.omero.core.entities.repositoryentities.serverentities.image.Image;
import qupath.ext.omero.core.entities.search.SearchQuery;
import qupath.ext.omero.core.entities.search.SearchResult;
import qupath.ext.omero.core.entities.shapes.Line;
import qupath.ext.omero.core.entities.shapes.Rectangle;
import qupath.ext.omero.core.entities.shapes.Shape;
import qupath.lib.images.servers.PixelType;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class TestApisHandler extends OmeroServer {

    abstract static class GenericClient {

        protected static WebClient client;
        protected static ApisHandler apisHandler;

        @AfterAll
        static void removeClient() {
            WebClients.removeClient(client);
        }

        @Test
        void Check_UInt8_Pixel_Type() {
            PixelType expectedPixelType = PixelType.UINT8;

            PixelType pixelType = ApisHandler.getPixelType("uint8").orElse(null);

            Assertions.assertEquals(expectedPixelType, pixelType);
        }

        @Test
        void Check_Int8_Pixel_Type() {
            PixelType expectedPixelType = PixelType.INT8;

            PixelType pixelType = ApisHandler.getPixelType("int8").orElse(null);

            Assertions.assertEquals(expectedPixelType, pixelType);
        }

        @Test
        void Check_UInt16_Pixel_Type() {
            PixelType expectedPixelType = PixelType.UINT16;

            PixelType pixelType = ApisHandler.getPixelType("uint16").orElse(null);

            Assertions.assertEquals(expectedPixelType, pixelType);
        }

        @Test
        void Check_Int16_Pixel_Type() {
            PixelType expectedPixelType = PixelType.INT16;

            PixelType pixelType = ApisHandler.getPixelType("int16").orElse(null);

            Assertions.assertEquals(expectedPixelType, pixelType);
        }

        @Test
        void Check_UInt32_Pixel_Type() {
            PixelType expectedPixelType = PixelType.UINT32;

            PixelType pixelType = ApisHandler.getPixelType("uint32").orElse(null);

            Assertions.assertEquals(expectedPixelType, pixelType);
        }

        @Test
        void Check_Int32_Pixel_Type() {
            PixelType expectedPixelType = PixelType.INT32;

            PixelType pixelType = ApisHandler.getPixelType("int32").orElse(null);

            Assertions.assertEquals(expectedPixelType, pixelType);
        }

        @Test
        void Check_Float_Pixel_Type() {
            PixelType expectedPixelType = PixelType.FLOAT32;

            PixelType pixelType = ApisHandler.getPixelType("float").orElse(null);

            Assertions.assertEquals(expectedPixelType, pixelType);
        }

        @Test
        void Check_Double_Pixel_Type() {
            PixelType expectedPixelType = PixelType.FLOAT64;

            PixelType pixelType = ApisHandler.getPixelType("double").orElse(null);

            Assertions.assertEquals(expectedPixelType, pixelType);
        }

        @Test
        void Check_Invalid_Pixel_Type() {
            Optional<PixelType> pixelType = ApisHandler.getPixelType("invalid");

            Assertions.assertTrue(pixelType.isEmpty());
        }

        @Test
        void Check_Web_Server_URI() {
            URI expectedWebServerURI = URI.create(OmeroServer.getWebServerURI());

            URI webServerURI = apisHandler.getWebServerURI();

            Assertions.assertEquals(expectedWebServerURI, webServerURI);
        }

        @Test
        void Check_Server_URI() {
            String expectedServerURI = OmeroServer.getServerURI();

            String serverURI = apisHandler.getServerURI();

            Assertions.assertEquals(expectedServerURI, serverURI);
        }

        @Test
        void Check_Server_Port() {
            int expectedServerPort = OmeroServer.getServerPort();

            int serverPort = apisHandler.getServerPort();

            Assertions.assertEquals(expectedServerPort, serverPort);
        }

        @Test
        void Check_Images_URI_Of_Dataset() throws ExecutionException, InterruptedException {
            long datasetID = OmeroServer.getDataset().getId();
            List<URI> expectedURIs = OmeroServer.getImagesUriInDataset();

            List<URI> uris = apisHandler.getImagesURIOfDataset(datasetID).get();

            TestUtilities.assertCollectionsEqualsWithoutOrder(expectedURIs, uris);
        }

        @Test
        void Check_Image_URI_Of_Invalid_Dataset() throws ExecutionException, InterruptedException {
            long datasetID = -1;
            List<URI> expectedURIs = List.of();

            List<URI> uris = apisHandler.getImagesURIOfDataset(datasetID).get();

            TestUtilities.assertCollectionsEqualsWithoutOrder(expectedURIs, uris);
        }

        @Test
        void Check_Image_URI_Of_Project() throws ExecutionException, InterruptedException {
            long projectID = OmeroServer.getProject().getId();
            List<URI> expectedURIs = OmeroServer.getImagesUriInDataset();

            List<URI> uris = apisHandler.getImagesURIOfProject(projectID).get();

            TestUtilities.assertCollectionsEqualsWithoutOrder(expectedURIs, uris);
        }

        @Test
        void Check_Image_URI_Of_Invalid_Project() throws ExecutionException, InterruptedException {
            long projectID = -1;
            List<URI> expectedURIs = List.of();

            List<URI> uris = apisHandler.getImagesURIOfProject(projectID).get();

            TestUtilities.assertCollectionsEqualsWithoutOrder(expectedURIs, uris);
        }

        @Test
        void Check_Image_URI() {
            Image image = OmeroServer.getComplexImage();
            String expectedURI = OmeroServer.getComplexImageURI().toString();

            String uri = apisHandler.getItemURI(image);

            Assertions.assertEquals(expectedURI, uri);
        }

        @Test
        void Check_Dataset_URI() {
            Dataset dataset = OmeroServer.getDataset();
            String expectedURI = OmeroServer.getDatasetURI().toString();

            String uri = apisHandler.getItemURI(dataset);

            Assertions.assertEquals(expectedURI, uri);
        }

        @Test
        void Check_Project_URI() {
            Project project = OmeroServer.getProject();
            String expectedURI = OmeroServer.getProjectURI().toString();

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

            TestUtilities.assertCollectionsEqualsWithoutOrder(expectedIds, ids);
        }

        @Test
        void Check_Groups() throws ExecutionException, InterruptedException {
            List<Group> expectedGroups = OmeroServer.getGroups();

            List<Group> groups = apisHandler.getGroups().get();

            TestUtilities.assertCollectionsEqualsWithoutOrder(expectedGroups, groups);
        }

        @Test
        void Check_Owners() throws ExecutionException, InterruptedException {
            List<Owner> expectedOwners = OmeroServer.getOwners();

            List<Owner> owners = apisHandler.getOwners().get();

            TestUtilities.assertCollectionsEqualsWithoutOrder(expectedOwners, owners);
        }

        @Test
        void Check_Projects() throws ExecutionException, InterruptedException {
            List<Project> expectedProjects = List.of(OmeroServer.getProject());

            List<Project> projects = apisHandler.getProjects().get();

            TestUtilities.assertCollectionsEqualsWithoutOrder(expectedProjects, projects);
        }

        @Test
        void Check_Orphaned_Datasets() throws ExecutionException, InterruptedException {
            List<Dataset> expectedOrphanedDatasets = List.of(OmeroServer.getOrphanedDataset());

            List<Dataset> orphanedDatasets = apisHandler.getOrphanedDatasets().get();

            TestUtilities.assertCollectionsEqualsWithoutOrder(expectedOrphanedDatasets, orphanedDatasets);
        }

        @Test
        void Check_Datasets() throws ExecutionException, InterruptedException {
            long projectID = OmeroServer.getProject().getId();
            List<Dataset> expectedDatasets = List.of(OmeroServer.getDataset());

            List<Dataset> datasets = apisHandler.getDatasets(projectID).get();

            TestUtilities.assertCollectionsEqualsWithoutOrder(expectedDatasets, datasets);
        }

        @Test
        void Check_Datasets_Of_Invalid_Project() throws ExecutionException, InterruptedException {
            long invalidProjectID = -1;
            List<Dataset> expectedDatasets = List.of();

            List<Dataset> datasets = apisHandler.getDatasets(invalidProjectID).get();

            TestUtilities.assertCollectionsEqualsWithoutOrder(expectedDatasets, datasets);
        }

        @Test
        void Check_Images() throws ExecutionException, InterruptedException {
            long datasetID = OmeroServer.getDataset().getId();
            List<Image> expectedImages = OmeroServer.getImagesInDataset();

            List<Image> images = apisHandler.getImages(datasetID).get();

            TestUtilities.assertCollectionsEqualsWithoutOrder(expectedImages, images);
        }

        @Test
        void Check_Images_Of_Invalid_Dataset() throws ExecutionException, InterruptedException {
            long invalidDatasetID = -1;
            List<Image> expectedImages = List.of();

            List<Image> images = apisHandler.getImages(invalidDatasetID).get();

            TestUtilities.assertCollectionsEqualsWithoutOrder(expectedImages, images);
        }

        @Test
        void Check_Image() throws ExecutionException, InterruptedException {
            Image expectedImage = OmeroServer.getComplexImage();
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

            TestUtilities.assertCollectionsEqualsWithoutOrder(expectedImages, images);
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
        void Check_Screens() throws ExecutionException, InterruptedException {
            List<Screen> expectedScreens = List.of(OmeroServer.getScreen());

            List<Screen> screens = apisHandler.getScreens().get();

            TestUtilities.assertCollectionsEqualsWithoutOrder(expectedScreens, screens);
        }

        @Test
        void Check_Orphaned_Plates() throws ExecutionException, InterruptedException {
            List<Plate> expectedOrphanedPlates = List.of(OmeroServer.getOrphanedPlate());

            List<Plate> orphanedPlates = apisHandler.getOrphanedPlates().get();

            TestUtilities.assertCollectionsEqualsWithoutOrder(expectedOrphanedPlates, orphanedPlates);
        }

        @Test
        void Check_Plates() throws ExecutionException, InterruptedException {
            long screenId = OmeroServer.getScreen().getId();
            List<Plate> expectedPlates = List.of(OmeroServer.getPlate());

            List<Plate> plates = apisHandler.getPlates(screenId).get();

            TestUtilities.assertCollectionsEqualsWithoutOrder(expectedPlates, plates);
        }

        @Test
        void Check_Plate_Acquisitions() throws ExecutionException, InterruptedException {
            long screenId = OmeroServer.getScreen().getId();
            List<PlateAcquisition> expectedPlateAcquisitions = List.of();

            List<PlateAcquisition> plateAcquisitions = apisHandler.getPlateAcquisitions(screenId).get();

            TestUtilities.assertCollectionsEqualsWithoutOrder(expectedPlateAcquisitions, plateAcquisitions);
        }

        @Test
        void Check_Wells() throws ExecutionException, InterruptedException {
            long plateId = OmeroServer.getPlate().getId();
            List<Well> expectedWells = OmeroServer.getWells();

            List<Well> wells = apisHandler.getWellsFromPlate(plateId).get();

            TestUtilities.assertCollectionsEqualsWithoutOrder(expectedWells, wells);
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

            TestUtilities.assertCollectionsEqualsWithoutOrder(expectedResults, searchResults);
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
            long imageId = OmeroServer.getComplexImage().getId();

            BufferedImage image = apisHandler.getThumbnail(imageId).get().orElse(null);

            Assertions.assertNotNull(image);
        }

        @Test
        void Check_Image_Thumbnail_With_Specific_Size() throws ExecutionException, InterruptedException {
            long imageId = OmeroServer.getComplexImage().getId();
            int size = 30;

            BufferedImage image = apisHandler.getThumbnail(imageId, size).get().orElse(null);

            Assertions.assertNotNull(image);
            Assertions.assertEquals(size, Math.max(image.getWidth(), image.getHeight()));
        }

        @Test
        void Check_Image_Thumbnail_With_Invalid_Image_ID() throws ExecutionException, InterruptedException {
            long invalidImageID = -1;

            BufferedImage image = apisHandler.getThumbnail(invalidImageID).get().orElse(null);

            Assertions.assertNull(image);
        }

        @Test
        void Check_Image_Metadata_Name() throws ExecutionException, InterruptedException {
            long imageId = OmeroServer.getComplexImage().getId();
            String expectedName = OmeroServer.getComplexImageName();

            ImageMetadataResponse metadata = apisHandler.getImageMetadata(imageId).get().orElse(null);

            Assertions.assertNotNull(metadata);
            Assertions.assertEquals(expectedName, metadata.getImageName());
        }

        @Test
        void Check_Image_Metadata_Pixel_Type() throws ExecutionException, InterruptedException {
            long imageId = OmeroServer.getComplexImage().getId();
            PixelType expectedPixelType = OmeroServer.getComplexImagePixelType();

            ImageMetadataResponse metadata = apisHandler.getImageMetadata(imageId).get().orElse(null);

            Assertions.assertNotNull(metadata);
            Assertions.assertEquals(expectedPixelType, metadata.getPixelType());
        }

        @Test
        void Check_Image_Metadata_Width() throws ExecutionException, InterruptedException {
            long imageId = OmeroServer.getComplexImage().getId();
            int expectedWidth = OmeroServer.getComplexImageWidth();

            ImageMetadataResponse metadata = apisHandler.getImageMetadata(imageId).get().orElse(null);

            Assertions.assertNotNull(metadata);
            Assertions.assertEquals(expectedWidth, metadata.getSizeX());
        }

        @Test
        void Check_Image_Metadata_Height() throws ExecutionException, InterruptedException {
            long imageId = OmeroServer.getComplexImage().getId();
            int expectedHeight = OmeroServer.getComplexImageHeight();

            ImageMetadataResponse metadata = apisHandler.getImageMetadata(imageId).get().orElse(null);

            Assertions.assertNotNull(metadata);
            Assertions.assertEquals(expectedHeight, metadata.getSizeY());
        }

        @Test
        void Check_Image_Metadata_Number_Of_Slices() throws ExecutionException, InterruptedException {
            long imageId = OmeroServer.getComplexImage().getId();
            int expectedNumberOfSlices = OmeroServer.getComplexImageNumberOfSlices();

            ImageMetadataResponse metadata = apisHandler.getImageMetadata(imageId).get().orElse(null);

            Assertions.assertNotNull(metadata);
            Assertions.assertEquals(expectedNumberOfSlices, metadata.getSizeZ());
        }

        @Test
        void Check_Image_Metadata_Number_Of_Channels() throws ExecutionException, InterruptedException {
            long imageId = OmeroServer.getComplexImage().getId();
            int expectedNumberOfChannels = OmeroServer.getComplexImageNumberOfChannels();

            ImageMetadataResponse metadata = apisHandler.getImageMetadata(imageId).get().orElse(null);

            Assertions.assertNotNull(metadata);
            Assertions.assertEquals(expectedNumberOfChannels, metadata.getChannels().size());
        }

        @Test
        void Check_Image_Metadata_Number_Of_Time_Points() throws ExecutionException, InterruptedException {
            long imageId = OmeroServer.getComplexImage().getId();
            int expectedNumberOfTimePoints = OmeroServer.getComplexImageNumberOfTimePoints();

            ImageMetadataResponse metadata = apisHandler.getImageMetadata(imageId).get().orElse(null);

            Assertions.assertNotNull(metadata);
            Assertions.assertEquals(expectedNumberOfTimePoints, metadata.getSizeT());
        }

        @Test
        void Check_Image_Metadata_Is_RGB() throws ExecutionException, InterruptedException {
            long imageId = OmeroServer.getComplexImage().getId();
            boolean expectedRGB = OmeroServer.isComplexImageRGB();

            ImageMetadataResponse metadata = apisHandler.getImageMetadata(imageId).get().orElse(null);

            Assertions.assertNotNull(metadata);
            Assertions.assertEquals(expectedRGB, metadata.isRGB());
        }

        @Test
        void Check_Image_Metadata_Pixel_Width() throws ExecutionException, InterruptedException {
            long imageId = OmeroServer.getComplexImage().getId();
            double expectedPixelWidth = OmeroServer.getComplexImagePixelWidthMicrons();

            ImageMetadataResponse metadata = apisHandler.getImageMetadata(imageId).get().orElse(null);

            Assertions.assertNotNull(metadata);
            Assertions.assertEquals(expectedPixelWidth, metadata.getPixelWidthMicrons().orElse(-1.));
        }

        @Test
        void Check_Image_Metadata_Pixel_Height() throws ExecutionException, InterruptedException {
            long imageId = OmeroServer.getComplexImage().getId();
            double expectedPixelHeight = OmeroServer.getComplexImagePixelHeightMicrons();

            ImageMetadataResponse metadata = apisHandler.getImageMetadata(imageId).get().orElse(null);

            Assertions.assertNotNull(metadata);
            Assertions.assertEquals(expectedPixelHeight, metadata.getPixelHeightMicrons().orElse(-1.));
        }

        @Test
        void Check_Image_Metadata_Pixel_Z_Spacing() throws ExecutionException, InterruptedException {
            long imageId = OmeroServer.getComplexImage().getId();
            double expectedPixelZSpacing = OmeroServer.getComplexImagePixelZSpacingMicrons();

            ImageMetadataResponse metadata = apisHandler.getImageMetadata(imageId).get().orElse(null);

            Assertions.assertNotNull(metadata);
            Assertions.assertEquals(expectedPixelZSpacing, metadata.getZSpacingMicrons().orElse(-1.));
        }

        @Test
        void Check_Image_Metadata_With_Invalid_Image_ID() throws ExecutionException, InterruptedException {
            long invalidImageID = -1;

            Optional<ImageMetadataResponse> metadata = apisHandler.getImageMetadata(invalidImageID).get();

            Assertions.assertTrue(metadata.isEmpty());
        }

        @Test
        void Check_Get_ROIs_With_Invalid_Image_ID() throws ExecutionException, InterruptedException {
            long invalidImageID = -1;
            List<Shape> expectedROIs = List.of();

            List<Shape> rois = apisHandler.getROIs(invalidImageID).get();

            TestUtilities.assertCollectionsEqualsWithoutOrder(expectedROIs, rois);
        }

        @Test
        abstract void Check_Write_ROIs() throws ExecutionException, InterruptedException;
    }

    @Nested
    class UnauthenticatedClient extends GenericClient {

        @BeforeAll
        static void createClient() throws ExecutionException, InterruptedException {
            client = OmeroServer.createUnauthenticatedClient();
            apisHandler = client.getApisHandler();
        }

        @Test
        @Override
        void Check_Write_ROIs() throws ExecutionException, InterruptedException {
            long imageId = OmeroServer.getComplexImage().getId();
            List<Shape> rois = List.of(new Rectangle(10, 10, 100, 100), new Line(20, 20, 50, 50));

            boolean success = apisHandler.writeROIs(imageId, rois, true).get();

            Assertions.assertFalse(success);    // unauthenticated user has read-only access
        }
    }

    @Nested
    class AuthenticatedClient extends GenericClient {

        @BeforeAll
        static void createClient() throws ExecutionException, InterruptedException {
            client = OmeroServer.createAuthenticatedClient();
            apisHandler = client.getApisHandler();
        }

        @Test
        @Override
        void Check_Write_ROIs() throws ExecutionException, InterruptedException {
            long imageId = OmeroServer.getComplexImage().getId();
            List<Shape> rois = List.of(new Rectangle(10, 10, 100, 100), new Line(20, 20, 50, 50));

            boolean success = apisHandler.writeROIs(imageId, rois, true).get();

            Assertions.assertTrue(success);
        }
    }

    private static class ServerEntityImplementation extends ServerEntity {

        @Override
        public boolean hasChildren() {
            return false;
        }

        @Override
        public ObservableList<? extends RepositoryEntity> getChildren() {
            return null;
        }

        @Override
        public ReadOnlyStringProperty getLabel() {
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


