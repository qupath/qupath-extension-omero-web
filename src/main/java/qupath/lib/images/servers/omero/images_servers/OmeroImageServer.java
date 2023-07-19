package qupath.lib.images.servers.omero.images_servers;

import qupath.lib.objects.*;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * <p>
 *     Interface that should be implemented by all {@link qupath.lib.images.servers.ImageServer image servers}
 *     of this extension.
 * </p>
 */
public interface OmeroImageServer {
    /**
     * <p>Send the given annotations to the OMERO server.</p>
     * <p>This function is asynchronous.</p>
     *
     * @param pathObjects  the list of annotations to send
     * @return a CompletableFuture indicating the success of the operation
     */
    CompletableFuture<Boolean> sendAnnotations(Collection<PathObject> pathObjects);
}
