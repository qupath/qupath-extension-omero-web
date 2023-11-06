package qupath.ext.omero.core.entities.repositoryentities.serverentities;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import qupath.ext.omero.TestUtilities;
import qupath.ext.omero.OmeroServer;
import qupath.ext.omero.core.WebClient;
import qupath.ext.omero.core.WebClients;
import qupath.ext.omero.core.entities.repositoryentities.RepositoryEntity;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class TestDataset extends OmeroServer {

    private static WebClient client;
    private static Dataset dataset;
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

        dataset = projectChildren.stream()
                .filter(child -> child instanceof Dataset)
                .map(d -> (Dataset) d)
                .findAny()
                .orElse(null);
    }

    @AfterAll
    static void removeClient() {
        WebClients.removeClient(client);
    }

    @Test
    void Check_Number_Of_Children() {
        int expectedNumberOfChildren = 1;

        int numberOfChildren = dataset.getNumberOfChildren();

        Assertions.assertEquals(expectedNumberOfChildren, numberOfChildren);
    }

    @Test
    void Check_Children() throws InterruptedException {
        List<? extends RepositoryEntity> expectedChildren = List.of(OmeroServer.getImage());

        List<? extends RepositoryEntity> children = dataset.getChildren();
        while (dataset.isPopulatingChildren()) {
            TimeUnit.MILLISECONDS.sleep(50);
        }

        TestUtilities.assertListEqualsWithoutOrder(expectedChildren, children);
    }

    @Test
    void Check_Attributes() {
        int numberOfValues = OmeroServer.getDatasetNumberOfAttributes();
        String[] expectedAttributeValues = new String[numberOfValues];
        for (int i=0; i<numberOfValues; ++i) {
            expectedAttributeValues[i] = OmeroServer.getDatasetAttributeValue(i);
        }

        String[] attributesValues = new String[numberOfValues];
        for (int i=0; i<numberOfValues; ++i) {
            attributesValues[i] = dataset.getAttributeValue(i);
        }

        Assertions.assertArrayEquals(expectedAttributeValues, attributesValues);
    }

    @Test
    void Check_Number_Of_Attributes() {
        int expectedNumberOfAttributes = OmeroServer.getDatasetNumberOfAttributes();

        int numberOfAttributes = dataset.getNumberOfAttributes();

        Assertions.assertEquals(expectedNumberOfAttributes, numberOfAttributes);
    }
}
