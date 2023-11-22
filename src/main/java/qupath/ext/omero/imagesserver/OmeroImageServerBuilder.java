package qupath.ext.omero.imagesserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.ImageServerBuilder;
import qupath.ext.omero.core.WebClient;
import qupath.ext.omero.core.WebClients;
import qupath.ext.omero.core.RequestSender;
import qupath.ext.omero.core.WebUtilities;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * <p>{@link qupath.lib.images.servers.ImageServerBuilder Image server builder} of this extension.</p>
 * <p>It creates an {@link OmeroImageServer}.</p>
 */
public class OmeroImageServerBuilder implements ImageServerBuilder<BufferedImage> {

    private static final Logger logger = LoggerFactory.getLogger(OmeroImageServerBuilder.class);
    private static final float SUPPORT_LEVEL = 4;

    @Override
    public ImageServer<BufferedImage> buildServer(URI uri, String... args) {
        return getClientAndCheckURIReachable(uri, args).flatMap(webClient -> OmeroImageServer.create(uri, webClient, args)).orElse(null);
    }

    @Override
    public ImageServerBuilder.UriImageSupport<BufferedImage> checkImageSupport(URI entityURI, String... args) {
        List<URI> imagesURIs = getImagesURIFromEntityURI(entityURI, args);

        float supportLevel = imagesURIs.isEmpty() ? 0 : SUPPORT_LEVEL;

        return UriImageSupport.createInstance(
                this.getClass(),
                supportLevel,
                imagesURIs.stream()
                        .map(uri -> {
                            try (var server = buildServer(uri, args)) {
                                return server == null ? null : server.getBuilder();
                            } catch (Exception e) {
                                logger.warn("Unable to create OMERO server", e);
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .toList()
        );
    }

    @Override
    public String getName() {
        return "OMERO";
    }

    @Override
    public String getDescription() {
        return "Image server using OMERO";
    }

    @Override
    public Class<BufferedImage> getImageType() {
        return BufferedImage.class;
    }

    @Override
    public boolean matchClassName(String... classNames) {
        for (var className : classNames) {
            if (this.getClass().getName().equals(className) ||
                    this.getClass().getSimpleName().equals(className) ||
                    OmeroImageServer.class.getName().equals(className) ||
                    OmeroImageServer.class.getSimpleName().equals(className) ||
                    "omero-web".equalsIgnoreCase(className))
                return true;
        }
        return false;
    }

    private static Optional<WebClient> getClientAndCheckURIReachable(URI uri, String... args) {
        try {
            if (RequestSender.isLinkReachableWithGet(uri).get()) {
                WebClient client = WebClients.createClientSync(uri.toString(), true, args);
                if (client.getStatus().equals(WebClient.Status.SUCCESS)) {
                    return Optional.of(client);
                } else {
                    logger.error("Client creation failed");
                    return Optional.empty();
                }
            } else {
                return Optional.empty();
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Client creation interrupted", e);
            return Optional.empty();
        }
    }

    private static List<URI> getImagesURIFromEntityURI(URI entityURI, String... args) {
        var client = getClientAndCheckURIReachable(entityURI, args);

        if (client.isPresent()) {
            try {
                return WebUtilities.getImagesURIFromEntityURI(entityURI, client.get().getApisHandler()).get();
            } catch (Exception e) {
                logger.error("Error when checking image support", e);
            }
        }

        return List.of();
    }
}
