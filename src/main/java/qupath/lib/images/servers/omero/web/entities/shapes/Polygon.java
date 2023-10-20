package qupath.lib.images.servers.omero.web.entities.shapes;

import com.google.gson.annotations.SerializedName;
import qupath.lib.objects.PathObject;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

/**
 * A polygon.
 */
class Polygon extends Shape {

    public static final String TYPE = TYPE_URL + "Polygon";
    @SerializedName(value = "Points", alternate = "points") private final String pointString;

    /**
     * Creates a polygon.
     *
     * @param pathObject  the path object corresponding to this shape
     * @param pointString  a list of points as returned by the OMERO API
     *                     (see {@link Shape#parseStringPoints(String) parseStringPoints()})
     */
    public Polygon(PathObject pathObject, String pointString) {
        super(pathObject);

        this.pointString = pointString;
        this.type = TYPE;
    }

    @Override
    public ROI createROI() {
        return ROIs.createPolygonROI(parseStringPoints(pointString == null ? "" : pointString), getPlane());
    }

    @Override
    public String toString() {
        return String.format("Polygon of points %s", pointString);
    }
}
