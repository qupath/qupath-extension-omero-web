package qupath.lib.images.servers.omero.core.apis;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.images.servers.omero.core.WebUtilities;
import qupath.lib.images.servers.omero.core.RequestSender;
import qupath.lib.images.servers.omero.core.entities.shapes.Shape;
import qupath.lib.io.GsonTools;
import qupath.lib.objects.*;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>API to communicate with an <a href="https://www.openmicroscopy.org/omero/iviewer/">OMERO.iviewer</a>.</p>
 * <p>It is simply used to send ROIs to an OMERO server.</p>
 */
class IViewerApi {

    private static final Logger logger = LoggerFactory.getLogger(IViewerApi.class);
    private static final String ROIS_URL = "%s/iviewer/persist_rois/";
    private static final String ROIS_BODY = """
        {
            "imageId":%d,
            "rois": {
                "count":%d,
                "empty_rois":{%s},
                "new_and_deleted":[],
                "deleted":{},
                "new":[%s],
                "modified":[]
            }
        }
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

    @Override
    public String toString() {
        return String.format("IViewer API of %s", host);
    }

    /**
     * <p>Attempt to write and delete ROIs to the server.</p>
     * <p>This function is asynchronous.</p>
     *
     * @param id  the OMERO image id
     * @param pathObjectsToAdd  the list of ROIs to add. Detections from this list will be skipped.
     * @param shapesToRemove the list of ROIs to remove
     * @param token  the OMERO <a href="https://docs.openmicroscopy.org/omero/5.6.0/developers/json-api.html#get-csrf-token">CSRF token</a>
     * @return a CompletableFuture indicating the success of the operation
     */
    public CompletableFuture<Boolean> writeROIs(long id, Collection<PathObject> pathObjectsToAdd, Collection<Shape> shapesToRemove, String token) {
        var uri = WebUtilities.createURI(String.format(ROIS_URL, host));

        if (uri.isPresent()) {
            var detections = pathObjectsToAdd.stream().filter(PathObject::isDetection).toList();
            if (!detections.isEmpty()) {
                logger.warn(detections.size() + " detections have been detected and won't be sent.");
            }

            List<String> roisToAdd = pathObjectsToString(pathObjectsToAdd.stream().filter(e -> !e.isDetection()).toList());
            String roisToRemove = shapesToRemove.stream()
                    .map(shape ->String.format("%s\":[%s]", shape.getOldId().split(":")[0], shape.getOldId()))
                    .collect(Collectors.joining(","));

            return RequestSender.post(
                    uri.get(),
                    String.format(
                            ROIS_BODY,
                            id,
                            roisToAdd.size() + shapesToRemove.size(),
                            roisToRemove,
                            String.join(", ", roisToAdd)
                    ),
                    String.format(ROIS_REFERER_URL, host, id),
                    token
            ).thenApply(response -> {
                if (response.isPresent()) {
                    if (response.get().toLowerCase().contains("error")) {
                        logger.error("Error when sending ROIs: " + response.get());
                        return false;
                    } else {
                        return true;
                    }
                } else {
                    return false;
                }
            });
        } else {
            return CompletableFuture.completedFuture(false);
        }
    }
    private List<String> pathObjectsToString(Collection<PathObject> pathObjects) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(TMACoreObject.class, new Shape.GsonShapeSerializer())
                .registerTypeAdapter(PathAnnotationObject.class, new Shape.GsonShapeSerializer())
                .registerTypeAdapter(PathDetectionObject.class, new Shape.GsonShapeSerializer())
                .serializeSpecialFloatingPointValues()
                .setLenient()
                .create();

        return pathObjects.stream()
                .map(pathObject -> {
                    if (pathObject instanceof PathCellObject) {
                        var detectionObject = PathObjects.createDetectionObject(pathObject.getROI());
                        detectionObject.setPathClass(pathObject.getPathClass());
                        detectionObject.setColor(pathObject.getColor());
                        detectionObject.setName(pathObject.getName());
                        pathObject = detectionObject;
                    }
                    return gson.toJson(pathObject);
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
