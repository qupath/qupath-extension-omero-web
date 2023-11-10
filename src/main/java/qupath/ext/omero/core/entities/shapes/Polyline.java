package qupath.ext.omero.core.entities.shapes;

import com.google.gson.annotations.SerializedName;
import qupath.lib.geom.Point2;
import qupath.lib.objects.PathObject;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

import java.util.List;
import java.util.Objects;

/**
 * A set of lines.
 */
public class Polyline extends Shape {

    public static final String TYPE = TYPE_URL + "Polyline";
    @SerializedName(value = "Points", alternate = "points") private final String pointString;

    /**
     * Creates a polyline.
     *
     * @param points  a list of points describing the polyline
     */
    public Polyline(List<Point2> points) {
        super(TYPE);

        this.pointString = pointsToString(points);
    }

    /**
     * Creates a polyline corresponding to a path object.
     *
     * @param pathObject  the path object corresponding to this shape
     */
    public Polyline(PathObject pathObject) {
        this(pathObject.getROI().getAllPoints());

        linkWithPathObject(pathObject);
    }

    @Override
    public ROI createROI() {
        return ROIs.createPolylineROI(parseStringPoints(pointString == null ? "" : pointString), getPlane());
    }

    @Override
    public String toString() {
        return String.format("Polyline of points %s", pointString);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof Polyline polyline))
            return false;
        return parseStringPoints(polyline.pointString == null ? "" : polyline.pointString)
                .equals(parseStringPoints(pointString == null ? "" : pointString));
    }

    @Override
    public int hashCode() {
        return Objects.hash(pointString);
    }
}
