package qupath.ext.omero.core.pixelapis.mspixelbuffer;

import javafx.beans.property.*;
import javafx.beans.value.ObservableBooleanValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.omero.core.ClientsPreferencesManager;
import qupath.ext.omero.core.RequestSender;
import qupath.ext.omero.core.WebClient;
import qupath.ext.omero.core.WebUtilities;
import qupath.ext.omero.core.pixelapis.PixelAPI;
import qupath.ext.omero.core.pixelapis.PixelAPIReader;
import qupath.lib.images.servers.ImageServerMetadata;
import qupath.lib.images.servers.PixelType;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

/**
 * <p>
 *     This API uses the <a href="https://github.com/glencoesoftware/omero-ms-pixel-buffer">OMERO Pixel Data Microservice</a>
 *     to access pixel values of an image. Any image can be used, and pixel values are accurate.
 *     However, the server needs to have this microservice installed.
 * </p>
 */
public class MsPixelBufferAPI implements PixelAPI {

    static final String NAME = "Pixel Buffer Microservice";
    private static final int DEFAULT_PORT = 8082;
    private static final String PORT_PARAMETER = "--msPixelBufferPort";
    private static final Logger logger = LoggerFactory.getLogger(MsPixelBufferAPI.class);
    private final WebClient client;
    private final BooleanProperty isAvailable = new SimpleBooleanProperty(false);
    private final IntegerProperty port;
    private String host;

    /**
     * Creates a new MsPixelBufferAPI.
     *
     * @param client  the WebClient owning this API
     */
    public MsPixelBufferAPI(WebClient client) {
        this.client = client;
        port = new SimpleIntegerProperty(
                ClientsPreferencesManager.getMsPixelBufferPort(client.getApisHandler().getWebServerURI().toString()).orElse(DEFAULT_PORT)
        );

        setHost();
        setAvailable(true);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String[] getArgs() {
        return new String[] {PORT_PARAMETER, String.valueOf(port.get())};
    }

    @Override
    public void setParametersFromArgs(String... args) {
        for (int i=0; i<args.length-1; ++i) {
            if (args[i].equals(PORT_PARAMETER)) {
                try {
                    setPort(Integer.parseInt(args[i+1]), true);
                } catch (NumberFormatException e) {
                    logger.warn(String.format("Can't convert %s to integer", args[i+1]), e);
                }
            }
        }
    }

    @Override
    public ObservableBooleanValue isAvailable() {
        return isAvailable;
    }

    @Override
    public boolean canAccessRawPixels() {
        return true;
    }

    @Override
    public boolean canReadImage(PixelType pixelType) {
        return !pixelType.equals(PixelType.INT8) && !pixelType.equals(PixelType.UINT32);
    }

    @Override
    public boolean canReadImage(int numberOfChannels) {
        return true;
    }

    @Override
    public PixelAPIReader createReader(long id, ImageServerMetadata metadata, boolean allowSmoothInterpolation, int nResolutions) {
        if (!isAvailable().get()) {
            throw new IllegalStateException("This API is not available and cannot be used");
        }
        if (!canReadImage(metadata.getPixelType(), metadata.getSizeC())) {
            throw new IllegalArgumentException("The provided image cannot be read by this API");
        }

        return new MsPixelBufferReader(
                client,
                host,
                id,
                metadata.getPixelType(),
                metadata.getChannels(),
                metadata.nLevels()
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof MsPixelBufferAPI msPixelBufferAPI))
            return false;
        return msPixelBufferAPI.client.equals(client);
    }

    @Override
    public int hashCode() {
        return client.hashCode();
    }

    @Override
    public String toString() {
        return String.format("Ms pixel buffer API of %s", client.getApisHandler().getWebServerURI());
    }

    /**
     * @return the port used by this microservice on the OMERO server.
     * This property may be updated from any thread
     */
    public ReadOnlyIntegerProperty getPort() {
        return port;
    }

    /**
     * Set the port used by this microservice on the OMERO server.
     * This may change the availability of this pixel API.
     *
     * @param port the new port this microservice uses on the OMERO server
     * @param checkAvailabilityNow  whether to directly check if the new port changes the
     *                              availability of this pixel API. If false, the check will
     *                              be performed in the background (recommended to avoid blocking
     *                              the calling thread)
     */
    public void setPort(int port, boolean checkAvailabilityNow) {
        this.port.set(port);

        ClientsPreferencesManager.setMsPixelBufferPort(
                client.getApisHandler().getWebServerURI().toString(),
                port
        );

        setHost();
        setAvailable(!checkAvailabilityNow);
    }

    private void setHost() {
        Optional<URI> uri = changePortOfURI(client.getApisHandler().getWebServerURI(), port.get());
        if (uri.isPresent()) {
            host = uri.get().toString();
        } else {
            host = client.getApisHandler().getWebServerURI().toString();
        }
    }

    private void setAvailable(boolean performInBackground) {
        Optional<URI> uri = WebUtilities.createURI(host + "/tile");

        if (uri.isPresent()) {
            if (performInBackground) {
                RequestSender.isLinkReachableWithOptions(uri.get()).thenAccept(isAvailable::set);
            } else {
                isAvailable.set(RequestSender.isLinkReachableWithOptions(uri.get()).join());
            }
        } else {
            isAvailable.set(false);
        }
    }

    private static Optional<URI> changePortOfURI(URI uri, int port) {
        try {
            return Optional.of(new URI(
                    uri.getScheme(),
                    uri.getUserInfo(),
                    uri.getHost(),
                    port,
                    uri.getPath(),
                    uri.getQuery(),
                    uri.getFragment()
            ));
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
    }
}
