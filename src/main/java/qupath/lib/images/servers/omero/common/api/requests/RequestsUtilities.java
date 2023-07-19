package qupath.lib.images.servers.omero.common.api.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods for handling API requests.
 */
public class RequestsUtilities {
    private final static Logger logger = LoggerFactory.getLogger(RequestsUtilities.class);
    private final static Pattern showImagePattern = Pattern.compile("show=image-(\\d+)");

    private RequestsUtilities() {
        throw new RuntimeException("This class is not instantiable.");
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
     * Parse the OMERO image ID from a URI.
     *
     * @return the image ID, or empty Optional if it was not found
     */
    public static Optional<Long> parseImageId(URI uri) {
        String uriQuery = uri.getQuery();
        String id = null;

        if (uriQuery != null && uriQuery.startsWith("show=image-")) {
            Matcher matcher = showImagePattern.matcher(uriQuery);
            if (matcher.find())
                id = matcher.group(1);
        }
        if (id == null)
            id = uri.getFragment();

        try {
            return Optional.of(Long.parseLong(id));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
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
}
