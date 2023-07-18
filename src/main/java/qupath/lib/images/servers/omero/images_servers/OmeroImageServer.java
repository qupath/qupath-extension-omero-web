package qupath.lib.images.servers.omero.images_servers;

import qupath.lib.objects.*;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public interface OmeroImageServer {
    CompletableFuture<Boolean> sendAnnotations(Collection<PathObject> pathObjects);
}
