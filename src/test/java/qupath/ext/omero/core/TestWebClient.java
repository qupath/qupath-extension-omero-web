package qupath.ext.omero.core;

import org.junit.jupiter.api.*;
import qupath.ext.omero.OmeroServer;

import java.net.URI;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class TestWebClient extends OmeroServer {

    private static WebClient client;

    @BeforeAll
    static void createClient() throws ExecutionException, InterruptedException {
        client = OmeroServer.createValidClient();
    }

    @AfterAll
    static void removeClient() {
        WebClients.removeClient(client);
    }

    @Test
    void Check_Client_Authentication() {
        boolean isAuthenticated = client.getAuthenticated().get();

        Assertions.assertTrue(isAuthenticated);
    }

    @Test
    void Check_Client_Username() {
        String expectedUsername = OmeroServer.getUsername();

        String username = client.getUsername().get();

        Assertions.assertEquals(expectedUsername, username);
    }

    @Test
    void Check_Opened_Images_When_One_Image_Added() {
        int expectedSize = client.getOpenedImagesURIs().size() + 1;

        client.addOpenedImage(URI.create(OmeroServer.getServerURL()));
        Set<URI> openedImagesURIs = client.getOpenedImagesURIs();

        Assertions.assertEquals(expectedSize, openedImagesURIs.size());
    }

    @Test
    void Check_Client_Password() {
        char[] expectedPassword = OmeroServer.getPassword().toCharArray();

        char[] password = client.getPassword().orElse(new char[0]);

        Assertions.assertArrayEquals(expectedPassword, password);
    }

    @Test
    void Check_Client_Can_Be_Closed() {
        boolean canBeClosed = client.canBeClosed();

        Assertions.assertTrue(canBeClosed);
    }
}
