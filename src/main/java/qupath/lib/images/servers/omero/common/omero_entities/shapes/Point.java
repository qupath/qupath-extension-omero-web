package qupath.lib.images.servers.omero.common.omero_entities.shapes;

import com.google.gson.annotations.SerializedName;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

/**
 * A point.
 */
class Point extends Shape {
    @SerializedName(value = "X", alternate = "x") private double x;
    @SerializedName(value = "Y", alternate = "y") private double y;

    /**
     * Creates a point.
     *
     * @param x  the x-coordinate of the point
     * @param y  the y-coordinate of the point
     */
    Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    @Override
    ROI createROI() {
        return ROIs.createPointsROI(x, y, getPlane());
    }
}
