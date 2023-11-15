package qupath.ext.omero.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import qupath.ext.omero.OmeroServer;
import qupath.ext.omero.TestUtilities;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class TestWebClients extends OmeroServer {

    @Test
    void Check_Client_Creation_With_Public_User() throws ExecutionException, InterruptedException {
        WebClient client = WebClients.createClient(OmeroServer.getServerURL()).get();
        WebClient.Status expectedStatus = WebClient.Status.SUCCESS;

        WebClient.Status status = client.getStatus();

        Assertions.assertEquals(expectedStatus, status);

        WebClients.removeClient(client);
    }

    @Test
    void Check_Client_Creation_With_Root_User() throws ExecutionException, InterruptedException {
        WebClient client = WebClients.createClient(
                OmeroServer.getServerURL(),
                "-u",
                OmeroServer.getRootUsername(),
                "-p",
                OmeroServer.getRootPassword()
        ).get();
        WebClient.Status expectedStatus = WebClient.Status.SUCCESS;

        WebClient.Status status = client.getStatus();

        Assertions.assertEquals(expectedStatus, status);

        WebClients.removeClient(client);
    }

    @Test
    void Check_Client_Creation_With_Incorrect_Username() throws ExecutionException, InterruptedException {
        WebClient client = WebClients.createClient(
                OmeroServer.getServerURL(),
                "-u",
                "incorrect_username",
                "-p",
                OmeroServer.getRootPassword()
        ).get();
        WebClient.Status expectedStatus = WebClient.Status.FAILED;

        WebClient.Status status = client.getStatus();

        Assertions.assertEquals(expectedStatus, status);

        WebClients.removeClient(client);
    }

    @Test
    void Check_Client_Creation_With_Incorrect_Password() throws ExecutionException, InterruptedException {
        WebClient client = WebClients.createClient(
                OmeroServer.getServerURL(),
                "-u",
                OmeroServer.getRootUsername(),
                "-p",
                "incorrect_password"
        ).get();
        WebClient.Status expectedStatus = WebClient.Status.FAILED;

        WebClient.Status status = client.getStatus();

        Assertions.assertEquals(expectedStatus, status);

        WebClients.removeClient(client);
    }

    @Test
    void Check_Client_Creation_Sync() {
        WebClient client = WebClients.createClientSync(
                OmeroServer.getServerURL(),
                "-u",
                OmeroServer.getRootUsername(),
                "-p",
                OmeroServer.getRootPassword()
        );
        WebClient.Status expectedStatus = WebClient.Status.SUCCESS;

        WebClient.Status status = client.getStatus();

        Assertions.assertEquals(expectedStatus, status);

        WebClients.removeClient(client);
    }

    @Test
    void Check_Client_List_After_Added() {
        WebClient client = WebClients.createClientSync(OmeroServer.getServerURL());
        List<WebClient> expectedClients = List.of(client);

        List<WebClient> clients = WebClients.getClients();

        TestUtilities.assertListEqualsWithoutOrder(expectedClients, clients);

        WebClients.removeClient(client);
    }

    @Test
    void Check_Client_List_After_Removed() {
        WebClient client = WebClients.createClientSync(OmeroServer.getServerURL());
        List<WebClient> expectedClients = List.of();

        WebClients.removeClient(client);
        List<WebClient> clients = WebClients.getClients();

        TestUtilities.assertListEqualsWithoutOrder(expectedClients, clients);
    }
}
