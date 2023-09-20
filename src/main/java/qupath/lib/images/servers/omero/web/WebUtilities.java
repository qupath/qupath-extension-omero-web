package qupath.lib.images.servers.omero.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.images.servers.omero.web.apis.ApisHandler;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * Utility methods for handling web requests.
 */
public class WebUtilities {

    private static final Logger logger = LoggerFactory.getLogger(WebUtilities.class);
    private static final Pattern webclientImagePattern = Pattern.compile("/webclient/\\?show=image-(\\d+)");
    private static final Pattern webclientImagePatternAlternate = Pattern.compile("/webclient/img_detail/(\\d+)");
    private static final Pattern webgatewayImagePattern = Pattern.compile("/webgateway/img_detail/(\\d+)");
    private static final Pattern iviewerImagePattern = Pattern.compile("/iviewer/\\?images=(\\d+)");
    private static final Pattern datasetPattern = Pattern.compile("/webclient/\\?show=dataset-(\\d+)");
    private static final Pattern projectPattern = Pattern.compile("/webclient/\\?show=project-(\\d+)");
    private static final List<Pattern> allPatterns = List.of(
            webclientImagePattern,
            webclientImagePatternAlternate,
            webgatewayImagePattern,
            iviewerImagePattern,
            datasetPattern,
            projectPattern
    );
    private static final List<Pattern> imagePatterns = List.of(
            webclientImagePattern,
            webclientImagePatternAlternate,
            webgatewayImagePattern,
            iviewerImagePattern
    );


    private WebUtilities() {
        throw new AssertionError("This class is not instantiable.");
    }

    /**
     * Attempt to create a URI from the provided text. If the attempt fails,
     * the reason is logged.
     *
     * @param url  the text the URI should be created from. It can be null
     * @return a URI if it was successfully created, or an empty Optional otherwise
     */
    public static Optional<URI> createURI(String url) {
        try {
            return Optional.of(new URI(url));
        } catch (Exception e) {
            logger.error("Couldn't create URI " + url, e);
            return Optional.empty();
        }
    }

    /**
     * Parse the OMERO entity ID from a URI.
     *
     * @param uri  the URI that is supposed to contain the ID. It can be URL encoded
     * @return the entity ID, or an empty Optional if it was not found
     */
    public static OptionalInt parseEntityId(URI uri) {
        for (Pattern pattern : allPatterns) {
            var matcher = pattern.matcher(decodeURI(uri));

            if (matcher.find()) {
                try {
                    return OptionalInt.of(Integer.parseInt(matcher.group(1)));
                } catch (Exception ignored) {}
            }
        }
        return OptionalInt.empty();
    }

    /**
     * Returns the host part of a complex URI. For example,
     * {@code https://www.my-server.com/show=image-462} returns {@code https://www.my-server.com}
     *
     * @return the host part of the URI, or an empty Optional if it could not be determined
     */
    public static Optional<URI> getServerURI(URI uri) {
        return createURI(uri.getScheme() + "://" + uri.getAuthority());
    }

    /**
     * <p>Attempt to retrieve the image URIs indicated by the provided entity URI.</p>
     * <ul>
     *     <li>If the entity is a dataset, the URIs of the children of this dataset (which are images) are returned.</li>
     *     <li>If the entity is a project, the URIs of each children of the datasets of this project are returned.</li>
     *     <li>If the entity is an image, the input URI is returned.</li>
     *     <li>Else, nothing is returned.</li>
     * </ul>
     * <p>This function is asynchronous.</p>
     *
     * @param entityURI  the URI of the entity whose images should be retrieved. It can be URL encoded
     * @param apisHandler  the request handler corresponding to the current server
     * @return a CompletableFuture with the list described above
     */
    public static CompletableFuture<List<URI>> getImagesURIFromEntityURI(URI entityURI, ApisHandler apisHandler) {
        String entityURL = decodeURI(entityURI);

        if (datasetPattern.matcher(entityURL).find()) {
            var datasetID = parseEntityId(entityURI);

            if (datasetID.isPresent()) {
                return apisHandler.getImagesURIOfDataset(datasetID.getAsInt());
            }
        } else if (projectPattern.matcher(entityURL).find()) {
            var projectID = parseEntityId(entityURI);

            if (projectID.isPresent()) {
                return apisHandler.getImagesURIOfProject(projectID.getAsInt());
            }
        } else if (imagePatterns.stream().anyMatch(pattern -> pattern.matcher(entityURL).find())) {
            return CompletableFuture.completedFuture(List.of(entityURI));
        }

        return CompletableFuture.completedFuture(List.of());
    }

    private static String decodeURI(URI uri) {
        return URLDecoder.decode(uri.toString(), StandardCharsets.UTF_8);
    }
}
