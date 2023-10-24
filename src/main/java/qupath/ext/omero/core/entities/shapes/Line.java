package qupath.ext.omero.core.entities.shapes;

import com.google.gson.annotations.SerializedName;
import qupath.lib.objects.PathObject;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

/**
 * A line.
 */
class Line extends Shape {

    public static final String TYPE = TYPE_URL + "Line";
    @SerializedName(value = "X1", alternate = "x1") private final double x1;
    @SerializedName(value = "Y1", alternate = "y1") private final double y1;
    @SerializedName(value = "X2", alternate = "x2") private final double x2;
    @SerializedName(value = "Y2", alternate = "y2") private final double y2;

    /**
     * Creates a line.
     *
     * @param pathObject  the path object corresponding to this shape
     * @param x1  the x-coordinate of the start point of the line
     * @param y1  the y-coordinate of the start point of the line
     * @param x2  the x-coordinate of the end point of the line
     * @param y2  the y-coordinate of the end point of the line
     */
    public Line(PathObject pathObject, double x1, double y1, double x2, double y2) {
        super(pathObject);

        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.type = TYPE;
    }

    @Override
    public ROI createROI() {
        return ROIs.createLineROI(x1, y1, x2, y2, getPlane());
    }

    @Override
    public String toString() {
        return String.format("Line located between {x1: %f, y1: %f} and {x2: %f, y2: %f}", x1, y1, x2, y2);
    }
}
