package qupath.ext.omero.core.pixelapis.web;

import org.junit.jupiter.api.*;
import qupath.ext.omero.OmeroServer;
import qupath.ext.omero.core.WebClient;
import qupath.ext.omero.core.WebClients;
import qupath.ext.omero.core.pixelapis.PixelAPIReader;
import qupath.ext.omero.core.pixelapis.mspixelbuffer.MsPixelBufferAPI;
import qupath.ext.omero.imagesserver.OmeroImageServer;
import qupath.lib.analysis.stats.Histogram;
import qupath.lib.common.ColorTools;
import qupath.lib.images.servers.ImageServerMetadata;
import qupath.lib.images.servers.TileRequest;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

public class TestWebReader extends OmeroServer {

    abstract static class GenericImage {

        protected static WebClient client;
        protected static TileRequest tileRequest;
        protected static PixelAPIReader reader;

        @AfterAll
        static void removeClient() throws Exception {
            reader.close();
            WebClients.removeClient(client);
        }

        @Test
        void Check_Image_Can_Be_Read() throws IOException {
            BufferedImage image = reader.readTile(tileRequest);

            Assertions.assertNotNull(image);
        }

        @Test
        abstract void Check_Image_Histogram() throws IOException;
    }

    @Nested
    class RgbImage extends GenericImage {

        @BeforeAll
        static void createClient() throws ExecutionException, InterruptedException {
            client = OmeroServer.createUnauthenticatedClient();

            ImageServerMetadata metadata;
            int nResolutions;
            try (OmeroImageServer imageServer = OmeroServer.createImageServer(OmeroServer.getRGBImageURI())) {
                tileRequest = imageServer.getTileRequestManager().getTileRequest(0, 0, 0, 0, 0);

                metadata = imageServer.getMetadata();
                nResolutions = imageServer.nResolutions();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            reader = client.getPixelAPI(WebAPI.class).createReader(
                    OmeroServer.getRGBImage().getId(),
                    metadata,
                    true,
                    nResolutions
            );
        }

        @Test
        @Override
        void Check_Image_Histogram() throws IOException {
            double expectedMean = OmeroServer.getUInt8ImageRedChannelMean();
            double expectedStdDev = OmeroServer.getUInt8ImageRedChannelStdDev();

            BufferedImage image = reader.readTile(tileRequest);

            Histogram histogram = new Histogram(
                    Arrays.stream(image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth()))
                            .map(ColorTools::red)
                            .toArray(),
                    256,
                    Double.NaN,
                    Double.NaN
            );
            Assertions.assertEquals(expectedMean, histogram.getMeanValue(), 100);
            Assertions.assertEquals(expectedStdDev, histogram.getStdDev(), 100);
        }
    }

    @Nested
    class UInt8Image extends GenericImage {

        @BeforeAll
        static void createClient() throws ExecutionException, InterruptedException {
            client = OmeroServer.createUnauthenticatedClient();

            ImageServerMetadata metadata;
            int nResolutions;
            try (OmeroImageServer imageServer = OmeroServer.createImageServer(OmeroServer.getUInt8ImageURI())) {
                tileRequest = imageServer.getTileRequestManager().getTileRequest(0, 0, 0, 0, 0);

                metadata = imageServer.getMetadata();
                nResolutions = imageServer.nResolutions();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            reader = client.getPixelAPI(MsPixelBufferAPI.class).createReader(
                    OmeroServer.getUInt8Image().getId(),
                    metadata,
                    true,
                    nResolutions
            );
        }

        @Test
        @Override
        void Check_Image_Histogram() throws IOException {
            double expectedMean = OmeroServer.getUInt8ImageRedChannelMean();
            double expectedStdDev = OmeroServer.getUInt8ImageRedChannelStdDev();

            BufferedImage image = reader.readTile(tileRequest);

            Histogram histogram = new Histogram(
                    Arrays.stream(image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth()))
                            .map(ColorTools::red)
                            .toArray(),
                    256,
                    Double.NaN,
                    Double.NaN
            );

            Assertions.assertEquals(expectedMean, histogram.getMeanValue(), 0.001);
            Assertions.assertEquals(expectedStdDev, histogram.getStdDev(), 0.001);
        }
    }
}
