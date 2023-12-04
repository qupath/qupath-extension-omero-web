package qupath.ext.omero.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import qupath.ext.omero.OmeroServer;
import qupath.ext.omero.TestUtilities;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class TestWebClients extends OmeroServer {

    abstract static class GenericWebClientCreation {

        protected abstract WebClient createClient(String url, String... args) throws ExecutionException, InterruptedException;

        @Test
        void Check_Client_Creation_With_Public_User() throws ExecutionException, InterruptedException {
            int numberOfAttempts = 3;       // This test might sometimes fail because of the responsiveness of the OMERO server
            WebClient.Status expectedStatus = WebClient.Status.SUCCESS;
            WebClient client;

            int attempt = 0;
            do {
                client = createClient(OmeroServer.getWebServerURI());
            } while (!client.getStatus().equals(expectedStatus) && ++attempt < numberOfAttempts);
            WebClient.Status status = client.getStatus();

            Assertions.assertEquals(expectedStatus, status);

            WebClients.removeClient(client);
        }

        @Test
        void Check_Client_Creation_With_Root_User() throws ExecutionException, InterruptedException {
            WebClient client = createClient(
                    OmeroServer.getWebServerURI(),
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
        void Check_Client_Creation_With_Incorrect_Username() throws ExecutionException, InterruptedException {
            WebClient client = createClient(
                    OmeroServer.getWebServerURI(),
                    "-u",
                    "incorrect_username",
                    "-p",
                    OmeroServer.getRootPassword()
            );
            WebClient.Status expectedStatus = WebClient.Status.FAILED;

            WebClient.Status status = client.getStatus();

            Assertions.assertEquals(expectedStatus, status);

            WebClients.removeClient(client);
        }

        @Test
        void Check_Client_Creation_With_Incorrect_Password() throws ExecutionException, InterruptedException {
            WebClient client = createClient(
                    OmeroServer.getWebServerURI(),
                    "-u",
                    OmeroServer.getRootUsername(),
                    "-p",
                    "incorrect_password"
            );
            WebClient.Status expectedStatus = WebClient.Status.FAILED;

            WebClient.Status status = client.getStatus();

            Assertions.assertEquals(expectedStatus, status);

            WebClients.removeClient(client);
        }

        @Test
        void Check_Client_Creation_With_Invalid_URI() throws ExecutionException, InterruptedException {
            WebClient client = createClient(
                    "",
                    "-u",
                    OmeroServer.getRootUsername(),
                    "-p",
                    OmeroServer.getRootPassword()
            );
            WebClient.FailReason expectedFailReason = WebClient.FailReason.INVALID_URI_FORMAT;

            WebClient.FailReason failReason = client.getFailReason().orElse(null);

            Assertions.assertEquals(expectedFailReason, failReason);

            WebClients.removeClient(client);
        }

        @Test
        void Check_Client_List_After_Added() throws ExecutionException, InterruptedException {
            WebClient client = createClient(
                    OmeroServer.getWebServerURI(),
                    "-u",
                    OmeroServer.getRootUsername(),
                    "-p",
                    OmeroServer.getRootPassword()
            );
            List<WebClient> expectedClients = List.of(client);

            List<WebClient> clients = WebClients.getClients();

            TestUtilities.assertCollectionsEqualsWithoutOrder(expectedClients, clients);

            WebClients.removeClient(client);
        }

        @Test
        void Check_Client_List_After_Removed() throws ExecutionException, InterruptedException {
            WebClient client = createClient(
                    OmeroServer.getWebServerURI(),
                    "-u",
                    OmeroServer.getRootUsername(),
                    "-p",
                    OmeroServer.getRootPassword()
            );
            List<WebClient> expectedClients = List.of();

            WebClients.removeClient(client);
            List<WebClient> clients = WebClients.getClients();

            TestUtilities.assertCollectionsEqualsWithoutOrder(expectedClients, clients);
        }
    }

    @Nested
    class AsyncCreation extends GenericWebClientCreation {

        @Override
        protected WebClient createClient(String url, String... args) throws ExecutionException, InterruptedException {
            return WebClients.createClient(url, true, args).get();
        }
    }

    @Nested
    class SyncCreation extends GenericWebClientCreation {

        @Override
        protected WebClient createClient(String url, String... args) {
            return WebClients.createClientSync(url, true, args);
        }
    }
}
