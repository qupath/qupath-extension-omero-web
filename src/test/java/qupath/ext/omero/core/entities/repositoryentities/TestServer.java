package qupath.ext.omero.core.entities.repositoryentities;

import org.junit.jupiter.api.*;
import qupath.ext.omero.TestUtilities;
import qupath.ext.omero.OmeroServer;
import qupath.ext.omero.core.WebClient;
import qupath.ext.omero.core.WebClients;
import qupath.ext.omero.core.entities.permissions.Group;
import qupath.ext.omero.core.entities.permissions.Owner;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class TestServer extends OmeroServer {

    abstract static class GenericClient {

        protected static WebClient client;
        protected static Server server;

        @AfterAll
        static void removeClient() {
            WebClients.removeClient(client);
        }

        @Test
        void Check_Number_Of_Children() throws InterruptedException {
            int expectedNumberOfChildren = 3;    // project + orphaned dataset + orphaned folder
            while (server.isPopulatingChildren()) {
                TimeUnit.MILLISECONDS.sleep(50);
            }

            int numberOfChildren = server.getNumberOfChildren();

            Assertions.assertEquals(expectedNumberOfChildren, numberOfChildren);
        }

        @Test
        void Check_Children() throws InterruptedException, ExecutionException {
            List<? extends RepositoryEntity> expectedChildren = List.of(
                    OmeroServer.getOrphanedDataset(),
                    OmeroServer.getProject(),
                    OrphanedFolder.create(client.getApisHandler()).get()
            );
            while (server.isPopulatingChildren()) {
                TimeUnit.MILLISECONDS.sleep(50);
            }

            List<? extends RepositoryEntity> children = server.getChildren();

            TestUtilities.assertListEqualsWithoutOrder(expectedChildren, children);
        }

        @Test
        void Check_Is_Filtered() {
            boolean isFiltered = server.isFilteredByGroupOwnerName(null, null, null);

            Assertions.assertTrue(isFiltered);
        }

        @Test
        abstract void Check_Default_Group();

        @Test
        abstract void Check_Default_Owner();

        @Test
        void Check_Groups() {
            List<Group> expectedGroups = new ArrayList<>(OmeroServer.getGroups());
            expectedGroups.add(Group.getAllGroupsGroup());

            List<Group> groups = server.getGroups();

            TestUtilities.assertListEqualsWithoutOrder(expectedGroups, groups);
        }

        @Test
        void Check_Owners() {
            List<Owner> expectedOwners = new ArrayList<>(OmeroServer.getOwners());
            expectedOwners.add(Owner.getAllMembersOwner());

            List<Owner> owners = server.getOwners();

            TestUtilities.assertListEqualsWithoutOrder(expectedOwners, owners);
        }
    }

    @Nested
    class UnauthenticatedClient extends GenericClient {

        @BeforeAll
        static void createClient() throws ExecutionException, InterruptedException {
            client = OmeroServer.createUnauthenticatedClient();
            server = client.getServer();
        }

        @Test
        @Override
        void Check_Default_Group() {
            Optional<Group> defaultGroup = server.getDefaultGroup();

            Assertions.assertTrue(defaultGroup.isEmpty());
        }

        @Test
        @Override
        void Check_Default_Owner() {
            Optional<Owner> defaultOwner = server.getDefaultOwner();

            Assertions.assertTrue(defaultOwner.isEmpty());
        }
    }

    @Nested
    class AuthenticatedClient extends GenericClient {

        @BeforeAll
        static void createClient() throws ExecutionException, InterruptedException {
            client = OmeroServer.createAuthenticatedClient();
            server = client.getServer();
        }

        @Test
        @Override
        void Check_Default_Group() {
            Group expectedDefaultGroup = OmeroServer.getCurrentGroup();

            Group defaultGroup = server.getDefaultGroup().orElse(null);

            Assertions.assertEquals(expectedDefaultGroup, defaultGroup);
        }

        @Test
        @Override
        void Check_Default_Owner() {
            Owner expectedDefaultOwner = OmeroServer.getCurrentOwner();

            Owner defaultOwner = server.getDefaultOwner().orElse(null);

            Assertions.assertEquals(expectedDefaultOwner, defaultOwner);
        }
    }
}
