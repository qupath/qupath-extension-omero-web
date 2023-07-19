package qupath.lib.images.servers.omero.common.api.requests.apis;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.images.servers.omero.common.api.requests.RequestsUtilities;
import qupath.lib.images.servers.omero.common.api.requests.Requests;
import qupath.lib.images.servers.omero.common.omeroentities.shapes.Shape;
import qupath.lib.io.GsonTools;
import qupath.lib.objects.*;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * <p>API to communicate with an <a href="https://www.openmicroscopy.org/omero/iviewer/">OMERO.iviewer</a>.</p>
 * <p>It is simply used to send ROIs to an OMERO server.</p>
 */
public class IViewerApi {
    private static final Logger logger = LoggerFactory.getLogger(IViewerApi.class);
    private static final String ROIS_URL = "%s/iviewer/persist_rois/";
    private static final String ROIS_BODY = """
        {"imageId":%d,
        "rois":{"count":%d,
        "empty_rois":{},
        "new_and_deleted":[],
        "deleted":{},
        "new":[%s],"modified":[]}}
        """;
    private static final String ROIS_REFERER_URL = "%s/iviewer/?images=%d";
    private final URI host;

    /**
     * Creates an iviewer client.
     *
     * @param host  the base server URI (e.g. <a href="https://idr.openmicroscopy.org">https://idr.openmicroscopy.org</a>)
     */
    public IViewerApi(URI host) {
        this.host = host;
    }

    /**
     * <p>Attempt to send ROIs to the server.</p>
     * <p>This function is asynchronous.</p>
     *
     * @param id  the OMERO image id
     * @param pathObjects  the list of ROIs
     * @param token  the OMERO <a href="https://docs.openmicroscopy.org/omero/5.6.0/developers/json-api.html#get-csrf-token">CSRF token</a>
     * @return a CompletableFuture indicating the success of the operation
     */
    public CompletableFuture<Boolean> writeROIs(long id, Collection<PathObject> pathObjects, String token) {
        var uri = RequestsUtilities.createURI(String.format(ROIS_URL, host));
        List<String> rois = pathObjectsToString(pathObjects);

        if (uri.isPresent()) {
            return Requests.post(
                    uri.get(),
                    String.format(ROIS_BODY, id, rois.size(), String.join(", ", rois)),
                    String.format(ROIS_REFERER_URL, host, id),
                    token
            ).thenApply(response -> response.isPresent() && !response.get().toLowerCase().contains("error"));
        } else {
            return CompletableFuture.completedFuture(false);
        }
    }
    private List<String> pathObjectsToString(Collection<PathObject> pathObjects) {
        Gson gsonTMAs  = new GsonBuilder().registerTypeAdapter(TMACoreObject.class, new Shape.GsonShapeSerializer()).serializeSpecialFloatingPointValues().setLenient().create();
        Gson gsonAnnotation = new GsonBuilder().registerTypeAdapter(PathAnnotationObject.class, new Shape.GsonShapeSerializer()).setLenient().create();
        Gson gsonDetection  = new GsonBuilder().registerTypeAdapter(PathDetectionObject.class, new Shape.GsonShapeSerializer()).serializeSpecialFloatingPointValues().setLenient().create();

        return pathObjects.stream()
                .map(pathObject -> {
                    if (pathObject.isTMACore()) {
                        return gsonTMAs.toJson(pathObject);
                    } else if (pathObject.isAnnotation()) {
                        return gsonAnnotation.toJson(pathObject);
                    } else if (pathObject.isDetection()) {
                        if (pathObject instanceof PathCellObject) {
                            var detectionObject = PathObjects.createDetectionObject(pathObject.getROI());
                            detectionObject.setPathClass(pathObject.getPathClass());
                            detectionObject.setColor(pathObject.getColor());
                            detectionObject.setName(pathObject.getName());
                            pathObject = detectionObject;
                        }
                        return gsonDetection.toJson(pathObject);
                    } else {
                        logger.error(String.format("Type not supported: %s", pathObject.getClass()));
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .flatMap(json -> {
                    // See if resulting JSON is a list (e.g. Points/MultiPolygon)
                    try {
                        return Arrays.stream(GsonTools.getInstance().fromJson(json, JsonElement[].class)).map(JsonElement::toString);
                    } catch (Exception ex) {
                        return Stream.of(json);
                    }
                })
                .toList();
    }
}
