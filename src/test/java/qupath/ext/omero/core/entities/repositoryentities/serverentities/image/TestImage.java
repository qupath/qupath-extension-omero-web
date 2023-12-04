package qupath.ext.omero.core.entities.repositoryentities.serverentities.image;

import org.junit.jupiter.api.*;
import qupath.ext.omero.TestUtilities;
import qupath.ext.omero.OmeroServer;
import qupath.ext.omero.core.WebClient;
import qupath.ext.omero.core.WebClients;
import qupath.ext.omero.core.entities.repositoryentities.OrphanedFolder;
import qupath.ext.omero.core.entities.repositoryentities.RepositoryEntity;
import qupath.ext.omero.core.entities.repositoryentities.serverentities.Dataset;
import qupath.ext.omero.core.entities.repositoryentities.serverentities.Project;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class TestImage extends OmeroServer {

    abstract static class GenericImage {

        protected static WebClient client;
        protected static Image image;

        @AfterAll
        static void removeClient() {
            WebClients.removeClient(client);
        }

        @Test
        void Check_Has_Children() {
            boolean expectedChildren = false;

            boolean hasChildren = image.hasChildren();

            Assertions.assertEquals(expectedChildren, hasChildren);
        }

        @Test
        void Check_Children() throws InterruptedException {
            List<? extends RepositoryEntity> expectedChildren = List.of();

            List<? extends RepositoryEntity> children = image.getChildren();
            while (image.isPopulatingChildren()) {
                TimeUnit.MILLISECONDS.sleep(50);
            }

            TestUtilities.assertCollectionsEqualsWithoutOrder(expectedChildren, children);
        }

        @Test
        abstract void Check_Attributes();

        @Test
        abstract void Check_Supported();

        @Test
        abstract void Check_Unsupported_Reasons();
    }

    @Nested
    class RGBImage extends GenericImage {

        @BeforeAll
        static void createClient() throws ExecutionException, InterruptedException {
            client = OmeroServer.createUnauthenticatedClient();

            while (client.getServer().isPopulatingChildren()) {
                TimeUnit.MILLISECONDS.sleep(50);
            }
            Project project = client.getServer().getChildren().stream()
                    .filter(child -> child instanceof Project)
                    .map(p -> (Project) p)
                    .findAny()
                    .orElse(null);
            assert project != null;

            List<? extends RepositoryEntity> projectChildren = project.getChildren();
            while (project.isPopulatingChildren()) {
                TimeUnit.MILLISECONDS.sleep(50);
            }

            Dataset dataset = projectChildren.stream()
                    .filter(child -> child instanceof Dataset)
                    .map(d -> (Dataset) d)
                    .findAny()
                    .orElse(null);
            assert dataset != null;

            List<? extends RepositoryEntity> datasetChildren = dataset.getChildren();
            while (dataset.isPopulatingChildren()) {
                TimeUnit.MILLISECONDS.sleep(50);
            }

            image = datasetChildren.stream()
                    .filter(child -> child instanceof Image)
                    .map(d -> (Image) d)
                    .filter(image -> image.equals(OmeroServer.getRGBImage()))
                    .findAny()
                    .orElse(null);
        }

        @Test
        @Override
        void Check_Attributes() {
            int numberOfValues = image.getNumberOfAttributes();
            String[] expectedAttributeValues = new String[numberOfValues];
            for (int i=0; i<numberOfValues; ++i) {
                expectedAttributeValues[i] = OmeroServer.getRGBImageAttributeValue(i);
            }

            String[] attributesValues = new String[numberOfValues];
            for (int i=0; i<numberOfValues; ++i) {
                attributesValues[i] = image.getAttributeValue(i);
            }

            Assertions.assertArrayEquals(expectedAttributeValues, attributesValues);
        }

        @Test
        @Override
        void Check_Supported() {
            boolean isSupported = image.isSupported().get();

            Assertions.assertTrue(isSupported);
        }

        @Test
        @Override
        void Check_Unsupported_Reasons() {
            Set<Image.UNSUPPORTED_REASON> expectedReasons = Set.of();

            Set<Image.UNSUPPORTED_REASON> reasons = image.getUnsupportedReasons();

            TestUtilities.assertCollectionsEqualsWithoutOrder(expectedReasons, reasons);
        }
    }

    @Nested
    class ComplexImage extends GenericImage {

        @BeforeAll
        static void createClient() throws ExecutionException, InterruptedException {
            client = OmeroServer.createUnauthenticatedClient();

            while (client.getServer().isPopulatingChildren()) {
                TimeUnit.MILLISECONDS.sleep(50);
            }

            OrphanedFolder orphanedFolder = client.getServer().getChildren().stream()
                    .filter(child -> child instanceof OrphanedFolder)
                    .map(p -> (OrphanedFolder) p)
                    .findAny()
                    .orElse(null);
            assert orphanedFolder != null;

            List<? extends RepositoryEntity> orphanedFolderChildren = orphanedFolder.getChildren();
            while (orphanedFolder.isPopulatingChildren()) {
                TimeUnit.MILLISECONDS.sleep(50);
            }

            image = orphanedFolderChildren.stream()
                    .filter(child -> child instanceof Image)
                    .map(d -> (Image) d)
                    .findAny()
                    .orElse(null);
        }

        @Test
        @Override
        void Check_Attributes() {
            int numberOfValues = image.getNumberOfAttributes();
            String[] expectedAttributeValues = new String[numberOfValues];
            for (int i=0; i<numberOfValues; ++i) {
                expectedAttributeValues[i] = OmeroServer.getComplexImageAttributeValue(i);
            }

            String[] attributesValues = new String[numberOfValues];
            for (int i=0; i<numberOfValues; ++i) {
                attributesValues[i] = image.getAttributeValue(i);
            }

            Assertions.assertArrayEquals(expectedAttributeValues, attributesValues);
        }

        @Test
        @Override
        void Check_Supported() {
            boolean isSupported = image.isSupported().get();

            Assertions.assertTrue(isSupported);
        }

        @Test
        @Override
        void Check_Unsupported_Reasons() {
            Set<Image.UNSUPPORTED_REASON> expectedReasons = Set.of();

            Set<Image.UNSUPPORTED_REASON> reasons = image.getUnsupportedReasons();

            TestUtilities.assertCollectionsEqualsWithoutOrder(expectedReasons, reasons);
        }
    }
}
