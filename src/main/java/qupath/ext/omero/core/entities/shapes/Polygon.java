package qupath.ext.omero.core.entities.shapes;

import com.google.gson.annotations.SerializedName;
import qupath.lib.geom.Point2;
import qupath.lib.objects.PathObject;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

import java.util.List;
import java.util.Objects;

/**
 * A polygon.
 */
public class Polygon extends Shape {

    public static final String TYPE = TYPE_URL + "Polygon";
    @SerializedName(value = "Points", alternate = "points") private final String pointString;

    /**
     * Creates a polygon.
     *
     * @param points  a list of points describing the polygon
     */
    public Polygon(List<Point2> points) {
        super(TYPE);

        this.pointString = pointsToString(points);
    }

    /**
     * Creates a polygon corresponding to a path object.
     *
     * @param pathObject  the path object corresponding to this shape
     * @param roi the roi describing the polygon
     */
    public Polygon(PathObject pathObject, ROI roi) {
        this(roi.getAllPoints());

        linkWithPathObject(pathObject);
    }

    @Override
    public ROI createROI() {
        return ROIs.createPolygonROI(parseStringPoints(pointString == null ? "" : pointString), getPlane());
    }

    @Override
    public String toString() {
        return String.format("Polygon of points %s", pointString);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof Polygon polygon))
            return false;
        return parseStringPoints(polygon.pointString == null ? "" : polygon.pointString)
                .equals(parseStringPoints(pointString == null ? "" : pointString));
    }

    @Override
    public int hashCode() {
        return Objects.hash(pointString);
    }
}
