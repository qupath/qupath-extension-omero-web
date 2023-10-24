package qupath.lib.images.servers.omero.core.entities.shapes;

import com.google.gson.annotations.SerializedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.objects.PathObject;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

/**
 * A text placed at some point.
 * This is not supported by the extension, which will creates a point instead.
 */
class Label extends Shape {

    private static final Logger logger = LoggerFactory.getLogger(Shape.class);
    public static final String TYPE = TYPE_URL + "Label";
    @SerializedName(value = "X", alternate = "x") private double x;
    @SerializedName(value = "Y", alternate = "y") private double y;

    public Label(PathObject pathObject) {
        super(pathObject);
    }

    @Override
    public ROI createROI() {
        logger.warn("Creating point (requested label shape is unsupported)");
        return ROIs.createPointsROI(x, y, getPlane());
    }

    @Override
    public String toString() {
        return String.format("Label located at {x: %f, y: %f}", x, y);
    }
}
