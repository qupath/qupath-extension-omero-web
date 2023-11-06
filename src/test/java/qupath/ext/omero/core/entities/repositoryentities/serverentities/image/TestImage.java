package qupath.ext.omero.core.entities.repositoryentities.serverentities.image;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import qupath.ext.omero.TestUtilities;
import qupath.ext.omero.core.OmeroServer;
import qupath.ext.omero.core.WebClient;
import qupath.ext.omero.core.WebClients;
import qupath.ext.omero.core.entities.repositoryentities.RepositoryEntity;
import qupath.ext.omero.core.entities.repositoryentities.serverentities.Dataset;
import qupath.ext.omero.core.entities.repositoryentities.serverentities.Project;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class TestImage extends OmeroServer {

    private static WebClient client;
    private static Image image;
    @BeforeAll
    static void createClient() throws ExecutionException, InterruptedException {
        client = OmeroServer.createValidClient();

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
    void Check_Attributes() {
        int numberOfValues = OmeroServer.getImageNumberOfAttributes();
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
    void Check_Number_Of_Attributes() {
        int expectedNumberOfAttributes = OmeroServer.getImageNumberOfAttributes();

        int numberOfAttributes = image.getNumberOfAttributes();

        Assertions.assertEquals(expectedNumberOfAttributes, numberOfAttributes);
    }

    @Test
    void Check_Supported() {
        boolean isSupported = image.isSupported().get();

        Assertions.assertTrue(isSupported);
    }

    @Test
    void Check_Not_Uint8() {
        boolean isUint8 = image.isUint8();

        Assertions.assertFalse(isUint8);
    }

    @Test
    void Check_Has_3_channels() {
        boolean has3Channels = image.has3Channels();

        Assertions.assertFalse(has3Channels);
    }
}
