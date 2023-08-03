package qupath.lib.images.servers.omero.common.imagesservers.pixelsapis.ice;

import omero.gateway.Gateway;
import omero.gateway.LoginCredentials;
import omero.gateway.SecurityContext;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.facility.BrowseFacility;
import omero.gateway.model.ExperimenterData;
import omero.gateway.model.ImageData;
import omero.model.ExperimenterGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.images.servers.ImageChannel;
import qupath.lib.images.servers.TileRequest;
import qupath.lib.images.servers.omero.common.api.clients.WebClient;
import qupath.lib.images.servers.omero.common.imagesservers.pixelsapis.PixelAPI;

import java.awt.image.*;
import java.util.*;

/**
 * <p>Reads raw pixel values from an image.</p>
 * <p>
 *     When the corresponding {@link WebClient} is no longer used,
 *     remember to call {@link #closeApisOfClient(WebClient)} to free
 *     resources on the server side.
 * </p>
 * <p>
 *     This class uses the {@link IceLogger} class to forward log messages from
 *     the OMERO Java gateway to the logging framework used by the extension, and
 *     the {@link IceReaderWrapper} class to actually read the pixel values.
 * </p>
 */
public class IceAPI implements PixelAPI {
    private static final Logger logger = LoggerFactory.getLogger(IceAPI.class);
    private static final Map<WebClient, IceAPI> apis = new HashMap<>();
    private final Gateway gateway;
    private SecurityContext context;
    private List<ImageChannel> channels;
    private ImageData imageData;
    private IceReaderWrapper readerWrapper;

    private IceAPI(WebClient client) throws DSOutOfServiceException {
        gateway = new Gateway(new IceLogger());
        ExperimenterData user = gateway.connect(new LoginCredentials(
                client.getUsername().get(),
                client.getPassword().map(String::valueOf).orElse(null),
                client.getServerURI().getHost(),
                client.getPort()
        ));

        context = new SecurityContext(user.getGroupId());
    }

    public static Optional<IceAPI> create(
            WebClient client,
            long imageID,
            List<ImageChannel> channels
    ) {
        try {
            IceAPI iceAPI;
            if (apis.containsKey(client)) {
                iceAPI = apis.get(client);
            } else {
                iceAPI = new IceAPI(client);
                apis.put(client, iceAPI);
            }

            var imageData = iceAPI.getImage(imageID);
            if (imageData.isPresent()) {
                iceAPI.imageData = imageData.get();
                iceAPI.channels = channels;
                iceAPI.readerWrapper = null;

                return Optional.of(iceAPI);
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            logger.error("Error when creating ICE API", e);
            return Optional.empty();
        }
    }

    @Override
    public BufferedImage readTile(TileRequest tileRequest) {
        if (readerWrapper == null) {
            IceReaderWrapper.create(context, gateway, imageData, channels)
                    .ifPresent(iceReaderWrapper -> readerWrapper = iceReaderWrapper);
        }

        if (readerWrapper == null) {
            return null;
        } else {
            return readerWrapper.getImage(tileRequest).orElse(null);
        }
    }

    public static void closeApisOfClient(WebClient client) {
        if (apis.containsKey(client)) {
            apis.get(client).gateway.disconnect();
            apis.remove(client);
        }
    }

    private Optional<ImageData> getImage(long imageID) {
        try {
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
        } catch (Exception e) {
            logger.error("Couldn't retrieve groups of current user", e);
            return Optional.empty();
        }
    }
}
