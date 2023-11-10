package qupath.ext.omero.core.entities.shapes;

import com.google.gson.annotations.SerializedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

import java.util.Objects;

/**
 * A text placed at some point.
 * This is not supported by the extension, which will creates a point instead.
 */
public class Label extends Shape {

    private static final Logger logger = LoggerFactory.getLogger(Shape.class);
    public static final String TYPE = TYPE_URL + "Label";
    @SerializedName(value = "X", alternate = "x") private double x;
    @SerializedName(value = "Y", alternate = "y") private double y;

    protected Label(String type) {
        super(type);
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

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof Label label))
            return false;
        return label.x == this.x && label.y == this.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}
