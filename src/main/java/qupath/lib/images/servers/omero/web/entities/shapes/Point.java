package qupath.lib.images.servers.omero.web.entities.shapes;

import com.google.gson.annotations.SerializedName;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

/**
 * A point.
 */
class Point extends Shape {

    @SerializedName(value = "X", alternate = "x") private final double x;
    @SerializedName(value = "Y", alternate = "y") private final double y;

    /**
     * Creates a point.
     *
     * @param x  the x-coordinate of the point
     * @param y  the y-coordinate of the point
     */
    public Point(double x, double y) {
        this.x = x;
        this.y = y;
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
