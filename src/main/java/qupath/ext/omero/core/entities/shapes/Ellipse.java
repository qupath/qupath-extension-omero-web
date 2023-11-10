package qupath.ext.omero.core.entities.shapes;

import com.google.gson.annotations.SerializedName;
import qupath.lib.objects.PathObject;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

import java.util.Objects;

/**
 * An ellipse.
 */
public class Ellipse extends Shape {

    public static final String TYPE = TYPE_URL + "Ellipse";
    @SerializedName(value = "X", alternate = "x") private final double x;
    @SerializedName(value = "Y", alternate = "y") private final double y;
    @SerializedName(value = "RadiusX", alternate = "radiusX") private final double radiusX;
    @SerializedName(value = "RadiusY", alternate = "radiusY") private final double radiusY;

    /**
     * Creates an ellipse.
     *
     * @param x  x-coordinate of the center of the ellipse
     * @param y  y-coordinate of the center of the ellipse
     * @param radiusX  radius along the x-axis
     * @param radiusY  radius along the y-axis
     */
    public Ellipse(double x, double y, double radiusX, double radiusY) {
        super(TYPE);

        this.x = x;
        this.y = y;
        this.radiusX = radiusX;
        this.radiusY = radiusY;
    }

    /**
     * Creates an ellipse corresponding to a path object.
     *
     * @param pathObject  the path object corresponding to this shape
     */
    public Ellipse(PathObject pathObject) {
        this(
                pathObject.getROI().getCentroidX(),
                pathObject.getROI().getCentroidY(),
                pathObject.getROI().getBoundsWidth()/2,
                pathObject.getROI().getBoundsHeight()/2
        );

        linkWithPathObject(pathObject);
    }

    @Override
    public ROI createROI() {
        return ROIs.createEllipseROI(x-radiusX, y-radiusY, radiusX*2, radiusY*2, getPlane());
    }

    @Override
    public String toString() {
        return String.format("Ellipse located centered at {x: %f, y: %f} of radius {x: %f, y: %f}", x, y, radiusX, radiusY);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof Ellipse ellipse))
            return false;
        return ellipse.x == this.x && ellipse.y == this.y && ellipse.radiusX == this.radiusX && ellipse.radiusY == this.radiusY;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, radiusX, radiusY);
    }
}
