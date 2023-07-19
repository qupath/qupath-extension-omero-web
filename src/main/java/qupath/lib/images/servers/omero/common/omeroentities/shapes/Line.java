package qupath.lib.images.servers.omero.common.omeroentities.shapes;

import com.google.gson.annotations.SerializedName;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

/**
 * A line.
 */
class Line extends Shape {
    @SerializedName(value = "X1", alternate = "x1") private double x1;
    @SerializedName(value = "Y1", alternate = "y1") private double y1;
    @SerializedName(value = "X2", alternate = "x2") private double x2;
    @SerializedName(value = "Y2", alternate = "y2") private double y2;

    /**
     * Creates a line.
     *
     * @param x1  the x-coordinate of the start point of the line
     * @param y1  the y-coordinate of the start point of the line
     * @param x2  the x-coordinate of the end point of the line
     * @param y2  the y-coordinate of the end point of the line
     */
    Line(double x1, double y1, double x2, double y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    @Override
    ROI createROI() {
        return ROIs.createLineROI(x1, y1, x2, y2, getPlane());
    }
}
