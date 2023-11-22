package qupath.ext.omero.core.pixelapis.mspixelbuffer;

import loci.formats.gui.AWTImageTools;
import qupath.ext.omero.core.RequestSender;
import qupath.ext.omero.core.WebClient;
import qupath.ext.omero.core.WebUtilities;
import qupath.ext.omero.core.pixelapis.PixelAPIReader;
import qupath.lib.color.ColorModelFactory;
import qupath.lib.images.servers.ImageChannel;
import qupath.lib.images.servers.PixelType;
import qupath.lib.images.servers.TileRequest;

import java.awt.image.*;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

/**
 * Read pixel values using the <a href="https://github.com/glencoesoftware/omero-ms-pixel-buffer">OMERO Pixel Data Microservice</a>.
 */
class MsPixelBufferReader implements PixelAPIReader {

    private static final String TILE_URI = "%s/tile/%d/%d/%d/%d?x=%d&y=%d&w=%d&h=%d&format=tif&resolution=%d";
    private final WebClient client;
    private final String host;
    private final long imageID;
    private final PixelType pixelType;
    private final int numberOfChannels;
    private final ColorModel colorModel;
    private final int numberOfLevels;

    /**
     * Create a new MsPixelBuffer reader.
     *
     * @param client  the WebClient owning the image to open
     * @param host  the URI from which this microservice is available
     * @param imageID  the ID of the image to open
     * @param pixelType  the pixel type of the image to open
     * @param channels  the channels of the image to open
     * @param numberOfLevels  the number of resolution levels of the image to open
     */
    public MsPixelBufferReader(
            WebClient client,
            String host,
            long imageID,
            PixelType pixelType,
            List<ImageChannel> channels,
            int numberOfLevels
    ) {
        this.client = client;
        this.host = host;
        this.imageID = imageID;
        this.pixelType = pixelType;
        this.numberOfChannels = channels.size();
        this.colorModel = ColorModelFactory.createColorModel(pixelType, channels);
        this.numberOfLevels = numberOfLevels;
    }

    @Override
    public BufferedImage readTile(TileRequest tileRequest) throws IOException {
        // OMERO expects resolutions to be specified in reverse order
        int level = numberOfLevels - tileRequest.getLevel() - 1;

        List<BufferedImage> images = IntStream.range(0, numberOfChannels)
                .mapToObj(i -> readTile(
                        imageID,
                        i,
                        level,
                        tileRequest
                ))
                .map(CompletableFuture::join)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        if (images.size() != numberOfChannels) {
            throw new IOException("Could not retrieve all pixels for all channels");
        } else {
            if (numberOfChannels == 1) {
                return images.get(0);
            } else {
                DataBuffer dataBuffer = getDataBuffer(images.stream()
                        .map(AWTImageTools::getPixels)
                        .toList()
                );

                return new BufferedImage(
                        colorModel,
                        WritableRaster.createWritableRaster(
                                new BandedSampleModel(
                                        dataBuffer.getDataType(),
                                        tileRequest.getTileWidth(),
                                        tileRequest.getTileHeight(),
                                        numberOfChannels
                                ),
                                dataBuffer,
                                null
                        ),
                        false,
                        null
                );
            }
        }
    }

    @Override
    public String getName() {
        return MsPixelBufferAPI.NAME;
    }

    @Override
    public void close() {}

    @Override
    public String toString() {
        return String.format(
                "Ms pixel buffer reader for image %d of %s",
                imageID,
                client.getApisHandler().getWebServerURI()
        );
    }

    private CompletableFuture<Optional<BufferedImage>> readTile(long imageID, int channel, int level, TileRequest tileRequest) {
        return WebUtilities.createURI(String.format(TILE_URI,
                host,
                imageID,
                tileRequest.getZ(),
                channel,
                tileRequest.getT(),
                tileRequest.getTileX(),
                tileRequest.getTileY(),
                tileRequest.getTileWidth(),
                tileRequest.getTileHeight(),
                level
        ))
                .map(RequestSender::getImage)
                .orElse(CompletableFuture.completedFuture(Optional.empty()));
    }

    private DataBuffer getDataBuffer(List<Object> pixels) {
        return switch (pixelType) {
            case UINT8 -> {
                byte[][] bytes = new byte[pixels.size()][];
                for (int i = 0; i < pixels.size(); i++) {
                    bytes[i] = ((byte[][]) pixels.get(i))[0];
                }
                yield new DataBufferByte(bytes, bytes[0].length);
            }
            case UINT16, INT16 -> {
                short[][] shortArray = new short[pixels.size()][];
                for (int i = 0; i < pixels.size(); i++) {
                    shortArray[i] = ((short[][]) pixels.get(i))[0];
                }
                yield pixelType.equals(PixelType.UINT16) ?
                        new DataBufferUShort(shortArray, shortArray[0].length) :
                        new DataBufferShort(shortArray, shortArray[0].length);
            }
            case INT32 -> {
                int[][] intArray = new int[pixels.size()][];
                for (int i = 0; i < pixels.size(); i++) {
                    intArray[i] = ((int[][]) pixels.get(i))[0];
                }
                yield new DataBufferInt(intArray, intArray[0].length);
            }
            case FLOAT32 -> {
                float[][] floatArray = new float[pixels.size()][];
                for (int c = 0; c < pixels.size(); c++) {
                    floatArray[c] = ((float[][]) pixels.get(c))[0];
                }
                yield  new DataBufferFloat(floatArray, floatArray[0].length);
            }
            case FLOAT64 -> {
                double[][] doubleArray = new double[pixels.size()][];
                for (int c = 0; c < pixels.size(); c++) {
                    doubleArray[c] = ((double[][]) pixels.get(c))[0];
                }
                yield  new DataBufferDouble(doubleArray, doubleArray[0].length);
            }
            default -> throw new UnsupportedOperationException("Unsupported pixel type " + pixelType);
        };
    }
}
