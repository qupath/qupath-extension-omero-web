package qupath.lib.images.servers.omero.images_servers.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.ImageServerBuilder;
import qupath.lib.images.servers.omero.common.api.RequestsUtilities;
import qupath.lib.images.servers.omero.common.api.clients.WebClient;
import qupath.lib.images.servers.omero.common.api.clients.WebClients;
import qupath.lib.images.servers.omero.common.api.requests.Requests;
import qupath.lib.images.servers.omero.images_servers.OmeroImageServerBuilder;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class WebImageServerBuilder implements ImageServerBuilder<BufferedImage>, OmeroImageServerBuilder {
    final private static Logger logger = LoggerFactory.getLogger(WebImageServerBuilder.class);

    @Override
    public ImageServer<BufferedImage> buildServer(URI uri, String... args) {
        return getClientAndCheckURIReachable(uri, args).flatMap(webClient -> WebImageServer.create(uri, webClient, args)).orElse(null);
    }

    @Override
    public UriImageSupport<BufferedImage> checkImageSupport(URI uri, String... args) {
        var client = getClientAndCheckURIReachable(uri, args);

        if (client.isPresent()) {
            try (var server = buildServer(uri, args)) {
                return UriImageSupport.createInstance(this.getClass(), 4, List.of(server.getBuilder()));
            } catch (Exception e) {
                logger.warn("Unable to create OMERO server", e);
            }
        }

        return UriImageSupport.createInstance(this.getClass(), 0, List.of());
    }

    @Override
    public String getName() {
        return "OMERO web";
    }

    @Override
    public String getDescription() {
        return "Image server using the OMERO web API";
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
                    WebImageServer.class.getName().equals(className) ||
                    WebImageServer.class.getSimpleName().equals(className) ||
                    "omero-web".equalsIgnoreCase(className))
                return true;
        }
        return false;
    }

    private Optional<WebClient> getClientAndCheckURIReachable(URI uri, String... args) {
        var serverURI = RequestsUtilities.getServerURI(uri);

        if (serverURI.isPresent()) {
            try {
                var client = WebClients.createClient(serverURI.get().toString(), args).get();
                if (client.isPresent() && Requests.isLinkReachable(uri).get()) {
                    return client;
                } else {
                    return Optional.empty();
                }
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Client creation interrupted", e);
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }
}
