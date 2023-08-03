package qupath.lib.images.servers.omero.common.imagesservers.pixelsapis.ice;

import omero.ServerError;
import omero.api.RawPixelsStorePrx;
import omero.api.ResolutionDescription;
import omero.gateway.Gateway;
import omero.gateway.SecurityContext;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.model.ImageData;
import omero.gateway.model.PixelsData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.color.ColorModelFactory;
import qupath.lib.images.servers.ImageChannel;
import qupath.lib.images.servers.PixelType;
import qupath.lib.images.servers.TileRequest;

import java.awt.image.*;
import java.nio.*;
import java.util.List;
import java.util.Optional;

/**
 * A class that can fetch pixel values and convert them to an image.
 * It is thread-safe so can be called from different threads at the same time.
 */
class IceReaderWrapper {
    private static final Logger logger = LoggerFactory.getLogger(IceReaderWrapper.class);
    private final RawPixelsStorePrx reader;
    private final PixelsData pixelsData;
    private final ResolutionDescription[] levelDescriptions;
    private final int numberOfResolutionLevels;
    private final int numberOfChannels;
    private final ColorModel colorModel;

    private IceReaderWrapper(SecurityContext context, Gateway gateway, ImageData image, List<ImageChannel> channels) throws ServerError, DSOutOfServiceException {
        pixelsData = image.getDefaultPixels();

        reader = gateway.getPixelsStore(context);
        reader.setPixelsId(pixelsData.getId(), false);

        levelDescriptions = reader.getResolutionDescriptions();
        numberOfResolutionLevels = reader.getResolutionLevels();
        numberOfChannels = channels.size();

        colorModel = ColorModelFactory.createColorModel(
                switch (pixelsData.getPixelType()) {
                    case PixelsData.INT8_TYPE -> PixelType.INT8;
                    case PixelsData.UINT8_TYPE -> PixelType.UINT8;
                    case PixelsData.INT16_TYPE -> PixelType.INT16;
                    case PixelsData.UINT16_TYPE -> PixelType.UINT16;
                    case PixelsData.UINT32_TYPE -> PixelType.UINT32;
                    case PixelsData.INT32_TYPE -> PixelType.INT32;
                    case PixelsData.FLOAT_TYPE -> PixelType.FLOAT32;
                    case PixelsData.DOUBLE_TYPE -> PixelType.FLOAT64;
                    default -> throw new IllegalArgumentException("Unsupported pixel type " + pixelsData.getPixelType());
                    },
                channels
        );
    }

    /**
     * Creates a new reader.
     *
     * @param context  the security context of the user/group to access the image
     * @param gateway  the gateway with an active connexion to the omero server
     * @param image  the image to read pixels from
     * @param channels  the channels of the image
     * @return the new reader, or an empty Optional if the creation failed
     */
    public static Optional<IceReaderWrapper> create(SecurityContext context, Gateway gateway, ImageData image, List<ImageChannel> channels) {
        try {
            return Optional.of(new IceReaderWrapper(context, gateway, image, channels));
        } catch (Exception e) {
            logger.error("Error when creating ice reader wrapper", e);
            return Optional.empty();
        }
    }

    /**
     * Fetch pixels and create an image from it.
     * This function might take some time to complete.
     *
     * @param tileRequest  the part of the image to fetch
     * @return the image corresponding to the tile request, or an empty Optional
     * if an error occurred
     */
    public Optional<BufferedImage> getImage(TileRequest tileRequest) {
        try {
            return Optional.of(new BufferedImage(colorModel, getRaster(tileRequest), false, null));
        } catch (Exception e) {
            logger.error("Error when reading tile " + tileRequest);
            return Optional.empty();
        }
    }

    private WritableRaster getRaster(TileRequest tileRequest) {
        int tileWidth = recalculateTileWidth(tileRequest);
        int tileHeight = recalculateTileHeight(tileRequest);

        DataBuffer buffer = getBuffer(getBytes(tileRequest, tileWidth, tileHeight));

        return WritableRaster.createWritableRaster(
                getSampleModel(buffer, tileWidth, tileHeight),
                buffer,
                null
        );
    }

    /**
     * Recalculate tile width in case it exceeds the limits of the dataset
     */
    private int recalculateTileWidth(TileRequest tileRequest) {
        return Math.min(
                tileRequest.getTileX() + tileRequest.getTileWidth(),
                levelDescriptions[tileRequest.getLevel()].sizeX
        ) - tileRequest.getTileX();
    }

    /**
     * Recalculate tile height in case it exceeds the limits of the dataset
     */
    private int recalculateTileHeight(TileRequest tileRequest) {
        return Math.min(
                tileRequest.getTileY() + tileRequest.getTileHeight(),
                levelDescriptions[tileRequest.getLevel()].sizeY
        ) - tileRequest.getTileY();
    }

    private synchronized byte[][] getBytes(TileRequest tileRequest, int tileWidth, int tileHeight) {
        int numberOfChannels = pixelsData.getSizeC();
        byte[][] bytes = new byte[numberOfChannels][];

        try {
            reader.setResolutionLevel(numberOfResolutionLevels - 1 - tileRequest.getLevel());
        } catch (Exception e) {
            logger.error("Could not set resolution level", e);
        }

        for (int channel = 0; channel < numberOfChannels; channel++) {
            try {
                bytes[channel] = reader.getTile(
                        tileRequest.getZ(),
                        channel,
                        tileRequest.getT(),
                        tileRequest.getTileX(),
                        tileRequest.getTileY(),
                        tileWidth,
                        tileHeight
                );
            } catch (ServerError e) {
                logger.error("Could not read tile " + tileRequest, e);
            }
        }

        return bytes;
    }

    private DataBuffer getBuffer(byte[][] bytes) {
        return switch (pixelsData.getPixelType()) {
            case (PixelsData.UINT8_TYPE), (PixelsData.INT8_TYPE) -> new DataBufferByte(bytes, bytes[0].length);
            case (PixelsData.UINT16_TYPE), (PixelsData.INT16_TYPE) -> {
                short[][] array = new short[bytes.length][];
                for (int i = 0; i < bytes.length; i++) {
                    ShortBuffer buffer = ByteBuffer.wrap(bytes[i]).asShortBuffer();
                    array[i] = new short[buffer.limit()];
                    buffer.get(array[i]);
                }
                yield pixelsData.getPixelType().equals(PixelsData.UINT16_TYPE) ?
                        new DataBufferUShort(array, bytes[0].length / 2) :
                        new DataBufferShort(array, bytes[0].length / 2);
            }
            case (PixelsData.UINT32_TYPE), (PixelsData.INT32_TYPE) -> {
                int[][] array = new int[bytes.length][];
                for (int i = 0; i < bytes.length; i++) {
                    IntBuffer buffer = ByteBuffer.wrap(bytes[i]).asIntBuffer();
                    array[i] = new int[buffer.limit()];
                    buffer.get(array[i]);
                }
                yield new DataBufferInt(array, bytes[0].length / 4);
            }
            case (PixelsData.FLOAT_TYPE) -> {
                float[][] array = new float[bytes.length][];
                for (int i = 0; i < bytes.length; i++) {
                    FloatBuffer buffer = ByteBuffer.wrap(bytes[i]).asFloatBuffer();
                    array[i] = new float[buffer.limit()];
                    buffer.get(array[i]);
                }
                yield new DataBufferFloat(array, bytes[0].length / 4);
            }
            case (PixelsData.DOUBLE_TYPE) -> {
                double[][] array = new double[bytes.length][];
                for (int i = 0; i < bytes.length; i++) {
                    DoubleBuffer buffer = ByteBuffer.wrap(bytes[i]).asDoubleBuffer();
                    array[i] = new double[buffer.limit()];
                    buffer.get(array[i]);
                }
                yield new DataBufferDouble(array, bytes[0].length / 8);
            }
            default -> {
                logger.error("Unsupported pixel type " + pixelsData.getPixelType());
                yield new DataBufferByte(0);
            }
        };
    }

    private SampleModel getSampleModel(DataBuffer buffer, int tileWidth, int tileHeight) {
        if (pixelsData.getSizeC() == 1 && numberOfChannels > 1) {
            // Handle channels stored in the same plane
            int[] offsets = new int[numberOfChannels];
            for (int b = 0; b < numberOfChannels; b++) {
                offsets[b] = b * tileWidth * tileHeight;
            }
            return new ComponentSampleModel(buffer.getDataType(), tileWidth, tileHeight, 1, tileWidth, offsets);
        } else {
            return new BandedSampleModel(buffer.getDataType(), tileWidth, tileHeight, numberOfChannels);
        }
    }
}
