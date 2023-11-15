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
        void Check_Number_Of_Children() {
            int expectedNumberOfChildren = 0;

            int numberOfChildren = image.getNumberOfChildren();

            Assertions.assertEquals(expectedNumberOfChildren, numberOfChildren);
        }

        @Test
        void Check_Children() throws InterruptedException {
            List<? extends RepositoryEntity> expectedChildren = List.of();

            List<? extends RepositoryEntity> children = image.getChildren();
            while (image.isPopulatingChildren()) {
                TimeUnit.MILLISECONDS.sleep(50);
            }

            TestUtilities.assertListEqualsWithoutOrder(expectedChildren, children);
        }

        @Test
        abstract void Check_Attributes();

        @Test
        abstract void Check_Supported();

        @Test
        abstract void Check_Uint8();

        @Test
        abstract void Check_Has_3_channels();
    }

    @Nested
    class SimpleRGBImage extends GenericImage {

        @BeforeAll
        static void createClient() throws ExecutionException, InterruptedException {
            client = OmeroServer.createUnauthenticatedClient();

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
                expectedAttributeValues[i] = OmeroServer.getOrphanedImageAttributeValue(i);
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
        void Check_Uint8() {
            boolean isUint8 = image.isUint8();

            Assertions.assertTrue(isUint8);
        }

        @Test
        @Override
        void Check_Has_3_channels() {
            boolean has3Channels = image.has3Channels();

            Assertions.assertTrue(has3Channels);
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
                    .findAny()
                    .orElse(null);
        }

        @Test
        @Override
        void Check_Attributes() {
            int numberOfValues = image.getNumberOfAttributes();
            String[] expectedAttributeValues = new String[numberOfValues];
            for (int i=0; i<numberOfValues; ++i) {
                expectedAttributeValues[i] = OmeroServer.getImageAttributeValue(i);
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

            Assertions.assertFalse(isSupported);
        }

        @Test
        @Override
        void Check_Uint8() {
            boolean isUint8 = image.isUint8();

            Assertions.assertFalse(isUint8);
        }

        @Test
        @Override
        void Check_Has_3_channels() {
            boolean has3Channels = image.has3Channels();

            Assertions.assertFalse(has3Channels);
        }
    }
}
