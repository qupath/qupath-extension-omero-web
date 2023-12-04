package qupath.ext.omero.core.pixelapis.mspixelbuffer;

import org.junit.jupiter.api.*;
import qupath.ext.omero.OmeroServer;
import qupath.ext.omero.core.WebClient;
import qupath.ext.omero.core.WebClients;
import qupath.ext.omero.core.pixelapis.PixelAPIReader;
import qupath.ext.omero.imagesserver.OmeroImageServer;
import qupath.lib.analysis.stats.Histogram;
import qupath.lib.common.ColorTools;
import qupath.lib.images.servers.ImageServerMetadata;
import qupath.lib.images.servers.TileRequest;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

public class TestMsPixelBufferReader extends OmeroServer {

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
            try (OmeroImageServer imageServer = OmeroServer.createImageServer(getRGBImageURI())) {
                tileRequest = imageServer.getTileRequestManager().getTileRequest(0, 0, 0, 0, 0);

                metadata = imageServer.getMetadata();
                nResolutions = imageServer.nResolutions();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            reader = client.getPixelAPI(MsPixelBufferAPI.class).createReader(
                    OmeroServer.getRGBImage().getId(),
                    metadata,
                    true,
                    nResolutions
            );
        }

        @Test
        @Override
        void Check_Image_Histogram() throws IOException {
            double expectedMean = OmeroServer.getRGBImageRedChannelMean();
            double expectedStdDev = OmeroServer.getRGBImageRedChannelStdDev();

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

    @Nested
    class UInt16Image extends GenericImage {

        @BeforeAll
        static void createClient() throws ExecutionException, InterruptedException {
            client = OmeroServer.createUnauthenticatedClient();

            ImageServerMetadata metadata;
            int nResolutions;
            try (OmeroImageServer imageServer = OmeroServer.createImageServer(OmeroServer.getUInt16ImageURI())) {
                tileRequest = imageServer.getTileRequestManager().getTileRequest(0, 0, 0, 0, 0);

                metadata = imageServer.getMetadata();
                nResolutions = imageServer.nResolutions();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            reader = client.getPixelAPI(MsPixelBufferAPI.class).createReader(
                    OmeroServer.getUInt16Image().getId(),
                    metadata,
                    true,
                    nResolutions
            );
        }

        @Test
        @Override
        void Check_Image_Histogram() throws IOException {
            double expectedMean = OmeroServer.getUInt16ImageRedChannelMean();
            double expectedStdDev = OmeroServer.getUInt16ImageRedChannelStdDev();

            BufferedImage image = reader.readTile(tileRequest);

            Raster raster = image.getData();
            int[] redValues = new int[image.getWidth()*image.getHeight()];
            for (int y=0; y<image.getHeight(); y++) {
                for (int x=0; x<image.getWidth(); x++) {
                    redValues[x + image.getWidth()*y] = raster.getSample(x, y, 0);
                }
            }
            Histogram histogram = new Histogram(
                    redValues,
                    256,
                    Double.NaN,
                    Double.NaN
            );

            Assertions.assertEquals(expectedMean, histogram.getMeanValue(), 0.001);
            Assertions.assertEquals(expectedStdDev, histogram.getStdDev(), 0.001);
        }
    }

    @Nested
    class Int16Image extends GenericImage {

        @BeforeAll
        static void createClient() throws ExecutionException, InterruptedException {
            client = OmeroServer.createUnauthenticatedClient();

            ImageServerMetadata metadata;
            int nResolutions;
            try (OmeroImageServer imageServer = OmeroServer.createImageServer(OmeroServer.getInt16ImageURI())) {
                tileRequest = imageServer.getTileRequestManager().getTileRequest(0, 0, 0, 0, 0);

                metadata = imageServer.getMetadata();
                nResolutions = imageServer.nResolutions();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            reader = client.getPixelAPI(MsPixelBufferAPI.class).createReader(
                    OmeroServer.getInt16Image().getId(),
                    metadata,
                    true,
                    nResolutions
            );
        }

        @Test
        @Override
        void Check_Image_Histogram() throws IOException {
            double expectedMean = OmeroServer.getInt16ImageRedChannelMean();
            double expectedStdDev = OmeroServer.getInt16ImageRedChannelStdDev();

            BufferedImage image = reader.readTile(tileRequest);

            Raster raster = image.getData();
            int[] redValues = new int[image.getWidth()*image.getHeight()];
            for (int y=0; y<image.getHeight(); y++) {
                for (int x=0; x<image.getWidth(); x++) {
                    redValues[x + image.getWidth()*y] = raster.getSample(x, y, 0);
                }
            }
            Histogram histogram = new Histogram(
                    redValues,
                    256,
                    Double.NaN,
                    Double.NaN
            );

            Assertions.assertEquals(expectedMean, histogram.getMeanValue(), 0.001);
            Assertions.assertEquals(expectedStdDev, histogram.getStdDev(), 0.001);
        }
    }

    @Nested
    class Int32Image extends GenericImage {

        @BeforeAll
        static void createClient() throws ExecutionException, InterruptedException {
            client = OmeroServer.createUnauthenticatedClient();

            ImageServerMetadata metadata;
            int nResolutions;
            try (OmeroImageServer imageServer = OmeroServer.createImageServer(OmeroServer.getInt32ImageURI())) {
                tileRequest = imageServer.getTileRequestManager().getTileRequest(0, 0, 0, 0, 0);

                metadata = imageServer.getMetadata();
                nResolutions = imageServer.nResolutions();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            reader = client.getPixelAPI(MsPixelBufferAPI.class).createReader(
                    OmeroServer.getInt32Image().getId(),
                    metadata,
                    true,
                    nResolutions
            );
        }

        @Test
        @Override
        void Check_Image_Histogram() throws IOException {
            double expectedMean = OmeroServer.getInt32ImageRedChannelMean();
            double expectedStdDev = OmeroServer.getInt32ImageRedChannelStdDev();

            BufferedImage image = reader.readTile(tileRequest);

            Raster raster = image.getData();
            int[] redValues = new int[image.getWidth()*image.getHeight()];
            for (int y=0; y<image.getHeight(); y++) {
                for (int x=0; x<image.getWidth(); x++) {
                    redValues[x + image.getWidth()*y] = raster.getSample(x, y, 0);
                }
            }
            Histogram histogram = new Histogram(
                    redValues,
                    256,
                    Double.NaN,
                    Double.NaN
            );

            Assertions.assertEquals(expectedMean, histogram.getMeanValue(), 0.001);
            Assertions.assertEquals(expectedStdDev, histogram.getStdDev(), 0.001);
        }
    }

    @Nested
    class Float32Image extends GenericImage {

        @BeforeAll
        static void createClient() throws ExecutionException, InterruptedException {
            client = OmeroServer.createUnauthenticatedClient();

            ImageServerMetadata metadata;
            int nResolutions;
            try (OmeroImageServer imageServer = OmeroServer.createImageServer(OmeroServer.getFloat32ImageURI())) {
                tileRequest = imageServer.getTileRequestManager().getTileRequest(0, 0, 0, 0, 0);

                metadata = imageServer.getMetadata();
                nResolutions = imageServer.nResolutions();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            reader = client.getPixelAPI(MsPixelBufferAPI.class).createReader(
                    OmeroServer.getFloat32Image().getId(),
                    metadata,
                    true,
                    nResolutions
            );
        }

        @Test
        @Override
        void Check_Image_Histogram() throws IOException {
            double expectedMean = OmeroServer.getFloat32ImageRedChannelMean();
            double expectedStdDev = OmeroServer.getFloat32ImageRedChannelStdDev();

            BufferedImage image = reader.readTile(tileRequest);

            Raster raster = image.getData();
            float[] redValues = new float[image.getWidth()*image.getHeight()];
            for (int y=0; y<image.getHeight(); y++) {
                for (int x=0; x<image.getWidth(); x++) {
                    redValues[x + image.getWidth()*y] = raster.getSampleFloat(x, y, 0);
                }
            }
            Histogram histogram = new Histogram(
                    redValues,
                    256,
                    Double.NaN,
                    Double.NaN
            );

            Assertions.assertEquals(expectedMean, histogram.getMeanValue(), 0.001);
            Assertions.assertEquals(expectedStdDev, histogram.getStdDev(), 0.001);
        }
    }

    @Nested
    class Float64Image extends GenericImage {

        @BeforeAll
        static void createClient() throws ExecutionException, InterruptedException {
            client = OmeroServer.createUnauthenticatedClient();

            ImageServerMetadata metadata;
            int nResolutions;
            try (OmeroImageServer imageServer = OmeroServer.createImageServer(OmeroServer.getFloat64ImageURI())) {
                tileRequest = imageServer.getTileRequestManager().getTileRequest(0, 0, 0, 0, 0);

                metadata = imageServer.getMetadata();
                nResolutions = imageServer.nResolutions();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            reader = client.getPixelAPI(MsPixelBufferAPI.class).createReader(
                    OmeroServer.getFloat64Image().getId(),
                    metadata,
                    true,
                    nResolutions
            );
        }

        @Test
        @Override
        void Check_Image_Histogram() throws IOException {
            double expectedMean = OmeroServer.getFloat64ImageRedChannelMean();
            double expectedStdDev = OmeroServer.getFloat64ImageRedChannelStdDev();

            BufferedImage image = reader.readTile(tileRequest);

            Raster raster = image.getData();
            double[] redValues = new double[image.getWidth()*image.getHeight()];
            for (int y=0; y<image.getHeight(); y++) {
                for (int x=0; x<image.getWidth(); x++) {
                    redValues[x + image.getWidth()*y] = raster.getSampleDouble(x, y, 0);
                }
            }
            Histogram histogram = new Histogram(
                    redValues,
                    256,
                    Double.NaN,
                    Double.NaN
            );

            Assertions.assertEquals(expectedMean, histogram.getMeanValue(), 0.001);
            Assertions.assertEquals(expectedStdDev, histogram.getStdDev(), 0.001);
        }
    }
}
