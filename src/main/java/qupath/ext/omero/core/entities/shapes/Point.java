package qupath.ext.omero.core.entities.shapes;

import com.google.gson.annotations.SerializedName;
import qupath.lib.objects.PathObject;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

import java.util.List;
import java.util.Objects;

/**
 * A point.
 */
public class Point extends Shape {

    public static final String TYPE = TYPE_URL + "Point";
    @SerializedName(value = "X", alternate = "x") private final double x;
    @SerializedName(value = "Y", alternate = "y") private final double y;

    /**
     * Create a point.

     * @param x  the x-coordinate of the point
     * @param y  the y-coordinate of the point
     */
    public Point(double x, double y) {
        super(TYPE);

        this.x = x;
        this.y = y;
    }

    /**
     * Create a list of points corresponding to a path object.
     *
     * @param pathObject  the path object corresponding to this shape
     * @return a list of points corresponding to the path object
     */
    public static List<Point> create(PathObject pathObject) {
        return pathObject.getROI().getAllPoints().stream()
                .map(point2 -> new Point(point2.getX(), point2.getY()))
                .peek(point -> point.linkWithPathObject(pathObject))
                .toList();
    }

    @Override
    public ROI createROI() {
        return ROIs.createPointsROI(x, y, getPlane());
    }

    @Override
    public String toString() {
        return String.format("Point located at {x: %f, y: %f}", x, y);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof Point point))
            return false;
        return point.x == this.x && point.y == this.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}
