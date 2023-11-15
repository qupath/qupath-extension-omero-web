package qupath.ext.omero;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.utility.DockerImageName;
import qupath.ext.omero.core.WebClient;
import qupath.ext.omero.core.WebClients;
import qupath.ext.omero.core.entities.annotations.AnnotationGroup;
import qupath.ext.omero.core.entities.permissions.Group;
import qupath.ext.omero.core.entities.permissions.Owner;
import qupath.ext.omero.core.entities.repositoryentities.serverentities.Dataset;
import qupath.ext.omero.core.entities.repositoryentities.serverentities.Project;
import qupath.ext.omero.core.entities.repositoryentities.serverentities.image.Image;
import qupath.ext.omero.core.entities.search.SearchResult;
import qupath.lib.images.servers.PixelType;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *     An abstract class that gives access to an OMERO server hosted
 *     on a local Docker container. Each subclass of this class will
 *     have access to the same OMERO server.
 * </p>
 * <p>
 *     All useful information of the OMERO server are returned by functions
 *     of this class.
 * </p>
 * <p>
 *     If Docker can't be found on the host machine, all tests are skipped.
 * </p>
 * <p>
 *     If a Docker container containing a working OMERO server is already
 *     running on the host machine, set the {@link #IS_LOCAL_OMERO_SERVER_RUNNING}
 *     variable to {@code true}. This will prevent this class to create a new container,
 *     gaining some time when running the tests.
 * </p>
 */
public abstract class OmeroServer {

    private static final boolean IS_LOCAL_OMERO_SERVER_RUNNING = false;
    private static final int CLIENT_CREATION_ATTEMPTS = 3;
    private static final String OMERO_PASSWORD = "password";
    private static final int OMERO_SERVER_PORT = 4064;
    private static final boolean dockerAvailable = DockerClientFactory.instance().isDockerAvailable();
    private static final GenericContainer<?> postgres;
    private static final GenericContainer<?> omeroServer;
    private static final GenericContainer<?> omeroWeb;
    private static final String analysisFileId;

    static {
        if (!dockerAvailable || IS_LOCAL_OMERO_SERVER_RUNNING) {
            postgres = null;
            omeroServer = null;
            omeroWeb = null;
            analysisFileId = "85";
        } else {
            postgres = new GenericContainer<>(DockerImageName.parse("postgres"))
                    .withNetwork(Network.SHARED)
                    .withNetworkAliases("postgres")
                    .withEnv("POSTGRES_PASSWORD", "postgres");
            postgres.start();

            omeroWeb = new GenericContainer<>(DockerImageName.parse("openmicroscopy/omero-web-standalone"))
                    .withNetwork(Network.SHARED)
                    .withEnv("OMEROHOST", "omero-server")
                    .withEnv("CONFIG_omero_web_public_enabled", "True")
                    .withEnv("CONFIG_omero_web_public_user", "public")
                    .withEnv("CONFIG_omero_web_public_password", "password")
                    .withEnv("CONFIG_omero_web_public_url__filter", "(.*?)")
                    .withExposedPorts(4080);
            omeroWeb.start();

            omeroServer = new GenericContainer<>(DockerImageName.parse("openmicroscopy/omero-server"))
                    .withNetwork(Network.SHARED)
                    .withNetworkAliases("omero-server")
                    .withEnv("CONFIG_omero_db_host", "postgres")
                    .withEnv("CONFIG_omero_db_user", "postgres")
                    .withEnv("CONFIG_omero_db_pass", "postgres")
                    .withEnv("CONFIG_omero_db_name", "postgres")
                    .withEnv("ROOTPASS", OMERO_PASSWORD)
                    .withExposedPorts(OMERO_SERVER_PORT)
                    .waitingFor(new AbstractWaitStrategy() {
                        @Override
                        protected void waitUntilReady() {
                            // Wait for the server to accept connections
                            while (true) {
                                try {
                                    WebClient client = createRootClient();
                                    WebClients.removeClient(client);
                                    return;
                                } catch (IllegalStateException | ExecutionException | InterruptedException ignored) {}

                                try {
                                    TimeUnit.SECONDS.sleep(1);
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    })
                    .withFileSystemBind(
                            Objects.requireNonNull(OmeroServer.class.getResource("analysis.csv")).getPath(),
                            "/analysis.csv"
                    )
                    .withFileSystemBind(
                            Objects.requireNonNull(OmeroServer.class.getResource("Cardio.tif")).getPath(),
                            "/Cardio.tif"
                    )
                    .withFileSystemBind(
                            Objects.requireNonNull(OmeroServer.class.getResource("mitosis.tif")).getPath(),
                            "/mitosis.tif"
                    )
                    .withFileSystemBind(
                            Objects.requireNonNull(OmeroServer.class.getResource("setupOmeroServer.sh")).getPath(),
                            "/setupOmeroServer.sh"
                    )
                    .dependsOn(postgres);
            omeroServer.start();

            try {
                omeroServer.execInContainer("chmod", "+x", "/setupOmeroServer.sh");

                Container.ExecResult result = omeroServer.execInContainer("/setupOmeroServer.sh");
                String[] logs = result.getStdout().split("\n");
                analysisFileId = logs[logs.length-1].split(":")[1];
            } catch (InterruptedException | IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @BeforeAll
    static void shouldRunTest() {
        Assumptions.assumeTrue(dockerAvailable, "Aborting test: no docker environment detected");
    }

    protected static String getServerURL() {
        return omeroWeb == null ? "http://localhost:4080" : "http://" + omeroWeb.getHost() + ":" + omeroWeb.getMappedPort(4080);
    }

    protected static WebClient createUnauthenticatedClient() throws ExecutionException, InterruptedException {
        return createValidClient();
    }

    protected static WebClient createAuthenticatedClient() throws ExecutionException, InterruptedException {
        return createValidClient(
                "-u",
                getUserUsername(),
                "-p",
                getUserPassword()
        );
    }

    protected static WebClient createRootClient() throws ExecutionException, InterruptedException {
        return createValidClient(
                "-u",
                getRootUsername(),
                "-p",
                getRootPassword()
        );
    }

    protected static String getServerHost() {
        return "omero-server";
    }

    protected static int getPort() {
        return OMERO_SERVER_PORT;
    }

    protected static String getRootUsername() {
        return "root";
    }

    protected static String getRootPassword() {
        return OMERO_PASSWORD;
    }

    protected static String getUserUsername() {
        return "public";
    }

    protected static String getUserPassword() {
        return "password";
    }

    protected static Project getProject() {
        return new Project(1);
    }

    protected static URI getProjectURI() {
        return URI.create(getServerURL() + "/webclient/?show=project-" + getProject().getId());
    }

    protected static String getProjectAttributeValue(int informationIndex) {
        return switch (informationIndex) {
            case 0 -> "project";
            case 1 -> String.valueOf(getProject().getId());
            case 2 -> "-";
            case 3 -> getCurrentOwner().getFullName();
            case 4 -> getCurrentGroup().getName();
            case 5 -> "1";
            default -> "";
        };
    }

    protected static Dataset getOrphanedDataset() {
        return new Dataset(2);
    }

    protected static Dataset getDataset() {
        return new Dataset(1);
    }

    protected static URI getDatasetURI() {
        return URI.create(getServerURL() + "/webclient/?show=dataset-" + getDataset().getId());
    }

    protected static AnnotationGroup getDatasetAnnotationGroup() {
        return new AnnotationGroup(JsonParser.parseString(String.format("""
                {
                    "annotations": [
                        {
                            "owner": {
                                "id": 2
                            },
                            "link": {
                                "owner": {
                                    "id": 2
                                }
                            },
                            "class": "CommentAnnotationI",
                            "textValue": "comment"
                        },
                        {
                            "owner": {
                                "id": 2
                            },
                            "link": {
                                "owner": {
                                    "id": 2
                                }
                            },
                            "class": "FileAnnotationI",
                            "file": {
                                "id": %s,
                                "name": "analysis.csv",
                                "size": 15,
                                "path": "//",
                                "mimetype": "text/csv"
                            }
                        }
                   ],
                   "experimenters": [
                        {
                            "id": 2,
                            "firstName": "public",
                            "lastName": "access"
                        }
                   ]
                }
                """, analysisFileId)).getAsJsonObject());
    }

    protected static String getDatasetAttributeValue(int informationIndex) {
        return switch (informationIndex) {
            case 0 -> "dataset";
            case 1 -> String.valueOf(getDataset().getId());
            case 2 -> "-";
            case 3 -> getCurrentOwner().getFullName();
            case 4 -> getCurrentGroup().getName();
            case 5 -> "1";
            default -> "";
        };
    }

    protected static List<SearchResult> getSearchResultsOnDataset() {
        return List.of(
                new SearchResult(
                        "dataset",
                        1,
                        "dataset",
                        getCurrentGroup().getName(),
                        "/webclient/?show=dataset-1",
                        null,
                        null
                ),
                new SearchResult(
                        "dataset",
                        2,
                        "orphaned_dataset",
                        getCurrentGroup().getName(),
                        "/webclient/?show=dataset-2",
                        null,
                        null
                )
        );
    }

    protected static Image getImage() {
        return new Image(1);
    }

    protected static URI getImageURI() {
        return URI.create(getServerURL() + "/webclient/?show=image-" + getImage().getId());
    }

    protected static String getImageName() {
        return "mitosis.tif";
    }

    protected static PixelType getImagePixelType() {
        return PixelType.UINT16;
    }

    protected static int getImageWidth() {
        return 171;
    }

    protected static int getImageHeight() {
        return 196;
    }

    protected static int getImageNumberOfSlices() {
        return 5;
    }

    protected static int getImageNumberOfChannels() {
        return 2;
    }

    protected static int getImageNumberOfTimePoints() {
        return 51;
    }

    protected static boolean isImageRGB() {
        return false;
    }

    protected static double getImagePixelWidthMicrons() {
        return 0.08850000022125;
    }

    protected static double getImagePixelHeightMicrons() {
        return 0.08850000022125;
    }

    protected static String getImageAttributeValue(int informationIndex) {
        return switch (informationIndex) {
            case 0 -> getImageName();
            case 1 -> String.valueOf(getImage().getId());
            case 2 -> getCurrentOwner().getFullName();
            case 3 -> getCurrentGroup().getName();
            case 4, 13 -> "-";
            case 5 -> getImageWidth() + " px";
            case 6 -> getImageHeight() + " px";
            case 7 -> "32.6 MB";
            case 8 -> String.valueOf(getImageNumberOfSlices());
            case 9 -> String.valueOf(getImageNumberOfChannels());
            case 10 -> String.valueOf(getImageNumberOfTimePoints());
            case 11 -> getImagePixelWidthMicrons() + " µm";
            case 12 -> getImagePixelHeightMicrons() + " µm";
            case 14 -> getImagePixelType().toString().toLowerCase();
            default -> "";
        };
    }

    protected static Image getOrphanedImage() {
        return new Image(2);
    }

    protected static URI getOrphanedImageURI() {
        return URI.create(getServerURL() + "/webclient/?show=image-" + getOrphanedImage().getId());
    }

    protected static String getOrphanedImageName() {
        return "Cardio.tif";
    }

    protected static PixelType getOrphanedImagePixelType() {
        return PixelType.UINT8;
    }

    protected static int getOrphanedImageWidth() {
        return 1000;
    }

    protected static int getOrphanedImageHeight() {
        return 1000;
    }

    protected static int getOrphanedImageNumberOfSlices() {
        return 1;
    }

    protected static int getOrphanedImageNumberOfChannels() {
        return 3;
    }

    protected static int getOrphanedImageNumberOfTimePoints() {
        return 1;
    }

    protected static String getOrphanedImageAttributeValue(int informationIndex) {
        return switch (informationIndex) {
            case 0 -> getOrphanedImageName();
            case 1 -> String.valueOf(getOrphanedImage().getId());
            case 2 -> getCurrentOwner().getFullName();
            case 3 -> getCurrentGroup().getName();
            case 4, 11, 12, 13 -> "-";
            case 5 -> getOrphanedImageWidth() + " px";
            case 6 -> getOrphanedImageHeight() + " px";
            case 7 -> "2.9 MB";
            case 8 -> String.valueOf(getOrphanedImageNumberOfSlices());
            case 9 -> String.valueOf(getOrphanedImageNumberOfChannels());
            case 10 -> String.valueOf(getOrphanedImageNumberOfTimePoints());
            case 14 -> getOrphanedImagePixelType().toString().toLowerCase();
            default -> "";
        };
    }

    protected static List<Group> getGroups() {
        return List.of(
                new Group(0, "system"),
                new Group(1, "user"),
                new Group(2, "guest"),
                new Group(3, "public-data")
        );
    }

    protected static List<Owner> getOwners() {
        return List.of(
                new Owner(0, "root", "", "root", "", "", "root"),
                new Owner(1, "Guest", "", "Account", "", "", "guest"),
                new Owner(2, "public", "", "access", "", "", "public")
        );
    }

    protected static Group getCurrentGroup() {
        return getGroups().get(3);
    }

    protected static Owner getCurrentOwner() {
        return getOwners().get(2);
    }

    private static WebClient createValidClient(String... args) throws ExecutionException, InterruptedException {
        WebClient webClient;
        int attempt = 0;

        do {
            webClient = WebClients.createClient(getServerURL(), args).get();
        } while (!webClient.getStatus().equals(WebClient.Status.SUCCESS) && ++attempt < CLIENT_CREATION_ATTEMPTS);

        if (webClient.getStatus().equals(WebClient.Status.SUCCESS)) {
            return webClient;
        } else {
            throw new IllegalStateException("Client creation failed");
        }
    }
}
