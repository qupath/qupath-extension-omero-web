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

public class TestProject extends OmeroServer {

    private static WebClient client;
    private static Project project;
    @BeforeAll
    static void createClient() throws ExecutionException, InterruptedException {
        client = OmeroServer.createUnauthenticatedClient();

        while (client.getServer().isPopulatingChildren()) {
            TimeUnit.MILLISECONDS.sleep(50);
        }
        project = client.getServer().getChildren().stream()
                .filter(child -> child instanceof Project)
                .map(project -> (Project) project)
                .findAny()
                .orElse(null);
    }

    @AfterAll
    static void removeClient() {
        WebClients.removeClient(client);
    }

    @Test
    void Check_Has_Children() {
        boolean expectedChildren = true;

        boolean hasChildren = project.hasChildren();

        Assertions.assertEquals(expectedChildren, hasChildren);
    }

    @Test
    void Check_Children() throws InterruptedException {
        List<? extends RepositoryEntity> expectedChildren = List.of(OmeroServer.getDataset());

        List<? extends RepositoryEntity> children = project.getChildren();
        while (project.isPopulatingChildren()) {
            TimeUnit.MILLISECONDS.sleep(50);
        }

        TestUtilities.assertCollectionsEqualsWithoutOrder(expectedChildren, children);
    }

    @Test
    void Check_Attributes() {
        int numberOfValues = project.getNumberOfAttributes();
        String[] expectedAttributeValues = new String[numberOfValues];
        for (int i=0; i<numberOfValues; ++i) {
            expectedAttributeValues[i] = OmeroServer.getProjectAttributeValue(i);
        }

        String[] attributesValues = new String[numberOfValues];
        for (int i=0; i<numberOfValues; ++i) {
            attributesValues[i] = project.getAttributeValue(i);
        }

        Assertions.assertArrayEquals(expectedAttributeValues, attributesValues);
    }
}
