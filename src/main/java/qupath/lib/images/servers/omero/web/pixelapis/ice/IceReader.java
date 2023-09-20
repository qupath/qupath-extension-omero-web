package qupath.lib.images.servers.omero.web.pixelapis.ice;

import omero.ServerError;
import omero.api.RawPixelsStorePrx;
import omero.gateway.Gateway;
import omero.gateway.LoginCredentials;
import omero.gateway.SecurityContext;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.facility.BrowseFacility;
import omero.gateway.model.ExperimenterData;
import omero.gateway.model.ImageData;
import omero.gateway.model.PixelsData;
import omero.model.ExperimenterGroup;
import qupath.lib.color.ColorModelFactory;
import qupath.lib.images.servers.ImageChannel;
import qupath.lib.images.servers.PixelType;
import qupath.lib.images.servers.TileRequest;
import qupath.lib.images.servers.bioformats.OMEPixelParser;
import qupath.lib.images.servers.omero.web.WebClient;
import qupath.lib.images.servers.omero.web.pixelapis.PixelAPIReader;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Read pixel values using the <a href="https://omero.readthedocs.io/en/v5.6.7/developers/Java.html">OMERO gateway</a>.
 */
class IceReader implements PixelAPIReader {

    private final Gateway gateway;
    private SecurityContext context;
    private RawPixelsStorePrx reader;
    private int numberOfResolutionLevels;
    private int nChannels;
    private int effectiveNChannels;
    private PixelType pixelType;
    private ColorModel colorModel;


    /**
     * Creates a new Ice reader.
     *
     * @param client  the WebClient owning the image to open
     * @param imageID  the ID of the image to open
     * @param channels  the channels of the image to open
     * @throws IOException  when the reader creation fails
     */
    public IceReader(WebClient client, long imageID, List<ImageChannel> channels) throws IOException {
        try {
            gateway = new Gateway(new IceLogger());
            ExperimenterData user = gateway.connect(new LoginCredentials(
                    client.getUsername().get(),
                    client.getPassword().map(String::valueOf).orElse(null),
                    client.getServerURI().getHost(),
                    client.getPort()
            ));

            context = new SecurityContext(user.getGroupId());

            setImage(imageID, channels);

        } catch (DSOutOfServiceException | ExecutionException | ServerError e) {
            throw new IOException(e);
        }
    }

    @Override
    public BufferedImage readTile(TileRequest tileRequest) throws IOException {
        byte[][] bytes = new byte[effectiveNChannels][];

        synchronized (reader) {
            try {
                reader.setResolutionLevel(numberOfResolutionLevels - 1 - tileRequest.getLevel());
            } catch (ServerError e) {
                throw new IOException(e);
            }

            for (int channel = 0; channel < effectiveNChannels; channel++) {
                try {
                    bytes[channel] = reader.getTile(
                            tileRequest.getZ(),
                            channel,
                            tileRequest.getT(),
                            tileRequest.getTileX(),
                            tileRequest.getTileY(),
                            tileRequest.getTileWidth(),
                            tileRequest.getTileHeight()
                    );
                } catch (ServerError e) {
                    throw new IOException(e);
                }
            }
        }

        OMEPixelParser omePixelParser = new OMEPixelParser.Builder()
                .isInterleaved(false)
                .pixelType(pixelType)
                .byteOrder(ByteOrder.BIG_ENDIAN)
                .normalizeFloats(false)
                .effectiveNChannels(effectiveNChannels)
                .build();

        return omePixelParser.parse(bytes, tileRequest.getTileWidth(), tileRequest.getTileHeight(), nChannels, colorModel);
    }

    @Override
    public String getName() {
        return IceAPI.NAME;
    }

    @Override
    public void close() {
        gateway.disconnect();
    }

    @Override
    public String toString() {
        return String.format("Ice reader for %s", context.getServerInformation());
    }

    private void setImage(long imageID, List<ImageChannel> channels) throws IOException, ServerError, DSOutOfServiceException, ExecutionException {
        var imageData = getImage(imageID);
        if (imageData.isPresent()) {
            PixelsData pixelsData = imageData.get().getDefaultPixels();

            reader = gateway.getPixelsStore(context);
            reader.setPixelsId(pixelsData.getId(), false);
            numberOfResolutionLevels = reader.getResolutionLevels();
            nChannels = channels.size();
            effectiveNChannels = pixelsData.getSizeC();
            pixelType = switch (pixelsData.getPixelType()) {
                case PixelsData.INT8_TYPE -> PixelType.INT8;
                case PixelsData.UINT8_TYPE -> PixelType.UINT8;
                case PixelsData.INT16_TYPE -> PixelType.INT16;
                case PixelsData.UINT16_TYPE -> PixelType.UINT16;
                case PixelsData.UINT32_TYPE -> PixelType.UINT32;
                case PixelsData.INT32_TYPE -> PixelType.INT32;
                case PixelsData.FLOAT_TYPE -> PixelType.FLOAT32;
                case PixelsData.DOUBLE_TYPE -> PixelType.FLOAT64;
                default -> throw new IllegalArgumentException("Unsupported pixel type " + pixelsData.getPixelType());
            };
            colorModel = ColorModelFactory.createColorModel(pixelType, channels);
        } else {
            throw new IOException("Couldn't find requested image of ID " + imageID);
        }
    }

    private Optional<ImageData> getImage(long imageID) throws ExecutionException, DSOutOfServiceException, ServerError {
        BrowseFacility browser = gateway.getFacility(BrowseFacility.class);
        try {
            return Optional.of(browser.getImage(context, imageID));
        } catch (Exception ignored) {}

        List<ExperimenterGroup> groups = gateway.getAdminService(context).containedGroups(gateway.getLoggedInUser().asExperimenter().getId().getValue());
        for(ExperimenterGroup group: groups) {
            context = new SecurityContext(group.getId().getValue());

            try {
                return Optional.of(browser.getImage(context, imageID));
            } catch (Exception ignored) {}
        }
        return Optional.empty();
    }
}
