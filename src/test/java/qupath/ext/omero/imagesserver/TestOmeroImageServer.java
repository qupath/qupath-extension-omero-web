package qupath.ext.omero.imagesserver;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import qupath.ext.omero.OmeroServer;
import qupath.ext.omero.TestUtilities;
import qupath.ext.omero.core.WebClient;
import qupath.ext.omero.core.WebClients;
import qupath.ext.omero.core.pixelapis.web.WebAPI;
import qupath.lib.analysis.stats.Histogram;
import qupath.lib.common.ColorTools;
import qupath.lib.images.servers.ImageServerMetadata;
import qupath.lib.images.servers.PixelType;
import qupath.lib.images.servers.TileRequest;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.PathObjects;
import qupath.lib.roi.ROIs;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class TestOmeroImageServer extends OmeroServer {

    private static WebClient client;
    private static OmeroImageServer imageServer;

    @BeforeAll
    static void createClient() throws ExecutionException, InterruptedException {
        client = OmeroServer.createValidClient();

        client.getSelectedPixelAPI().set(client.getAvailablePixelAPIs().stream().filter(pixelAPI -> pixelAPI instanceof WebAPI).findAny().orElse(null));
        imageServer = (OmeroImageServer) new OmeroImageServerBuilder().buildServer(getOrphanedImageURI());
    }

    @AfterAll
    static void removeClient() throws Exception {
        imageServer.close();
        WebClients.removeClient(client);
    }

    @Test
    void Check_Image_Can_Be_Read() throws IOException {
        TileRequest tileRequest = imageServer.getTileRequestManager().getTileRequest(0, 0, 0, 0, 0);

        BufferedImage image = imageServer.readTile(tileRequest);

        Assertions.assertNotNull(image);
    }

    @Test
    void Check_Image_Histogram() throws IOException {
        TileRequest tileRequest = imageServer.getTileRequestManager().getTileRequest(0, 0, 0, 0, 0);
        double expectedMean = 24.538;
        double expectedStdDev = 48.053;

        BufferedImage image = imageServer.readTile(tileRequest);

        Histogram histogram = new Histogram(
                Arrays.stream(image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth()))
                        .map(ColorTools::red)
                        .toArray(),
                256,
                Double.NaN,
                Double.NaN
        );
        Assertions.assertEquals(expectedMean, histogram.getMeanValue(), 0.01);
        Assertions.assertEquals(expectedStdDev, histogram.getStdDev(), 1);  // high delta because of JPEG compression
    }

    @Test
    void Check_Image_Thumbnail() throws IOException {
        BufferedImage thumbnail = imageServer.getDefaultThumbnail(0, 0);

        Assertions.assertNotNull(thumbnail);
    }

    @Test
    void Check_Image_Metadata_Width() {
        int expectedWidth = OmeroServer.getOrphanedImageWidth();

        ImageServerMetadata metadata = imageServer.getOriginalMetadata();

        Assertions.assertEquals(expectedWidth, metadata.getWidth());
    }

    @Test
    void Check_Image_Metadata_Height() {
        int expectedWidth = OmeroServer.getOrphanedImageWidth();

        ImageServerMetadata metadata = imageServer.getOriginalMetadata();

        Assertions.assertEquals(expectedWidth, metadata.getHeight());
    }

    @Test
    void Check_Image_Metadata_Pixel_Type() {
        PixelType expectedPixelType = OmeroServer.getOrphanedImagePixelType();

        ImageServerMetadata metadata = imageServer.getOriginalMetadata();

        Assertions.assertEquals(expectedPixelType, metadata.getPixelType());
    }

    @Test
    void Check_Image_Metadata_Name() {
        String expectedName = OmeroServer.getOrphanedImageName();

        ImageServerMetadata metadata = imageServer.getOriginalMetadata();

        Assertions.assertEquals(expectedName, metadata.getName());
    }

    @Test
    void Check_Image_Metadata_Number_Slices() {
        int expectedNumberOfSlices = OmeroServer.getOrphanedImageNumberOfSlices();

        ImageServerMetadata metadata = imageServer.getOriginalMetadata();

        Assertions.assertEquals(expectedNumberOfSlices, metadata.getSizeZ());
    }

    @Test
    void Check_Image_Metadata_Number_Channels() {
        int expectedNumberOfChannels = OmeroServer.getOrphanedImageNumberOfChannels();

        ImageServerMetadata metadata = imageServer.getOriginalMetadata();

        Assertions.assertEquals(expectedNumberOfChannels, metadata.getSizeC());
    }

    @Test
    void Check_Image_Metadata_Number_Time_Points() {
        int expectedNumberOfTimePoints = OmeroServer.getOrphanedImageNumberOfTimePoints();

        ImageServerMetadata metadata = imageServer.getOriginalMetadata();

        Assertions.assertEquals(expectedNumberOfTimePoints, metadata.getSizeT());
    }

    @Test
    void Check_Path_Objects_Written() {
        List<PathObject> pathObject = List.of(
                PathObjects.createAnnotationObject(ROIs.createRectangleROI(10, 10, 100, 100, null)),
                PathObjects.createAnnotationObject(ROIs.createLineROI(20, 20, 50, 50, null))
        );

        boolean success = imageServer.sendPathObjects(pathObject, true);

        Assertions.assertTrue(success);
    }

    @Test
    void Check_Path_Objects_Read() {
        List<PathObject> expectedPathObject = List.of(
                PathObjects.createAnnotationObject(ROIs.createRectangleROI(10, 10, 100, 100, null)),
                PathObjects.createAnnotationObject(ROIs.createLineROI(20, 20, 50, 50, null))
        );
        imageServer.sendPathObjects(expectedPathObject, true);

        Collection<PathObject> pathObjects = imageServer.readPathObjects();

        TestUtilities.assertListEqualsWithoutOrder(
                expectedPathObject.stream().map(PathObject::getID).toList(),
                pathObjects.stream().map(PathObject::getID).toList()
        );
    }

    @Test
    void Check_Id() {
        long id = imageServer.getId();

        Assertions.assertEquals(getOrphanedImage().getId(), id);
    }
}
