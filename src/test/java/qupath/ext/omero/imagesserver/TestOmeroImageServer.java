package qupath.ext.omero.imagesserver;

import org.junit.jupiter.api.*;
import qupath.ext.omero.OmeroServer;
import qupath.ext.omero.TestUtilities;
import qupath.ext.omero.core.WebClient;
import qupath.ext.omero.core.WebClients;
import qupath.lib.images.servers.PixelType;
import qupath.lib.images.servers.TileRequest;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.PathObjects;
import qupath.lib.roi.ROIs;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class TestOmeroImageServer extends OmeroServer {

    private static WebClient client;
    private static OmeroImageServer imageServer;

    @BeforeAll
    static void createImageServer() throws ExecutionException, InterruptedException {
        client = OmeroServer.createAuthenticatedClient();
        imageServer = OmeroServer.createImageServer(getRGBImageURI());
    }

    @AfterAll
    static void removeImageServer() throws Exception {
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
    void Check_Image_Thumbnail() throws IOException {
        BufferedImage thumbnail = imageServer.getDefaultThumbnail(0, 0);

        Assertions.assertNotNull(thumbnail);
    }

    @Test
    void Check_Image_Metadata_Width() {
        int expectedWidth = OmeroServer.getRGBImageWidth();

        int width = imageServer.getOriginalMetadata().getWidth();

        Assertions.assertEquals(expectedWidth, width);
    }

    @Test
    void Check_Image_Metadata_Height() {
        int expectedWidth = OmeroServer.getRGBImageHeight();

        int height = imageServer.getOriginalMetadata().getHeight();

        Assertions.assertEquals(expectedWidth, height);
    }

    @Test
    void Check_Image_Metadata_Pixel_Type() {
        PixelType expectedPixelType = OmeroServer.getRGBImagePixelType();

        PixelType pixelType = imageServer.getOriginalMetadata().getPixelType();

        Assertions.assertEquals(expectedPixelType, pixelType);
    }

    @Test
    void Check_Image_Metadata_Name() {
        String expectedName = OmeroServer.getRGBImageName();

        String name = imageServer.getOriginalMetadata().getName();

        Assertions.assertEquals(expectedName, name);
    }

    @Test
    void Check_Image_Metadata_Number_Slices() {
        int expectedNumberOfSlices = OmeroServer.getRGBImageNumberOfSlices();

        int numberOfSlices = imageServer.getOriginalMetadata().getSizeZ();

        Assertions.assertEquals(expectedNumberOfSlices, numberOfSlices);
    }

    @Test
    void Check_Image_Metadata_Number_Channels() {
        int expectedNumberOfChannels = OmeroServer.getRGBImageNumberOfChannels();

        int numberOfChannels = imageServer.getOriginalMetadata().getSizeC();

        Assertions.assertEquals(expectedNumberOfChannels, numberOfChannels);
    }

    @Test
    void Check_Image_Metadata_Number_Time_Points() {
        int expectedNumberOfTimePoints = OmeroServer.getRGBImageNumberOfTimePoints();

        int numberOfTimePoints = imageServer.getOriginalMetadata().getSizeT();

        Assertions.assertEquals(expectedNumberOfTimePoints, numberOfTimePoints);
    }

    @Test
    void Check_Image_Metadata_Pixel_Width() {
        double expectedPixelWidth = OmeroServer.getRGBImagePixelWidthMicrons();

        double pixelWidth = imageServer.getOriginalMetadata().getPixelWidthMicrons();

        Assertions.assertEquals(expectedPixelWidth, pixelWidth);
    }

    @Test
    void Check_Image_Metadata_Pixel_Height() {
        double expectedPixelHeight = OmeroServer.getRGBImagePixelHeightMicrons();

        double pixelHeight = imageServer.getOriginalMetadata().getPixelHeightMicrons();

        Assertions.assertEquals(expectedPixelHeight, pixelHeight);
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

        TestUtilities.assertCollectionsEqualsWithoutOrder(
                expectedPathObject.stream().map(PathObject::getID).toList(),
                pathObjects.stream().map(PathObject::getID).toList()
        );
    }

    @Test
    void Check_Id() {
        long expectedId = OmeroServer.getRGBImage().getId();

        long id = imageServer.getId();

        Assertions.assertEquals(expectedId, id);
    }
}
