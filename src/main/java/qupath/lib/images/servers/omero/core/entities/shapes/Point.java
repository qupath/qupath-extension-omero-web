package qupath.lib.images.servers.omero.core.entities.shapes;

import com.google.gson.annotations.SerializedName;
import qupath.lib.objects.PathObject;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

/**
 * A point.
 */
class Point extends Shape {

    public static final String TYPE = TYPE_URL + "Point";
    @SerializedName(value = "X", alternate = "x") private final double x;
    @SerializedName(value = "Y", alternate = "y") private final double y;

    /**
     * Creates a point.

     * @param pathObject  the path object corresponding to this shape
     * @param x  the x-coordinate of the point
     * @param y  the y-coordinate of the point
     */
    public Point(PathObject pathObject, double x, double y) {
        super(pathObject);

        this.x = x;
        this.y = y;
        this.type = TYPE;
    }

    @Override
    public ROI createROI() {
        return ROIs.createPointsROI(x, y, getPlane());
    }

    @Override
    public String toString() {
        return String.format("Point located at {x: %f, y: %f}", x, y);
    }
}
