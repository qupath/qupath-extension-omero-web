package qupath.lib.images.servers.omero.core.entities.shapes;

import com.google.gson.annotations.SerializedName;
import qupath.lib.objects.PathObject;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

/**
 * A set of lines.
 */
class Polyline extends Shape {

    public static final String TYPE = TYPE_URL + "Polyline";
    @SerializedName(value = "Points", alternate = "points") private final String pointString;

    /**
     * Creates a polyline.
     *
     * @param pathObject  the path object corresponding to this shape
     * @param pointString  a list of points as returned by the OMERO API
     *                     (see {@link Shape#parseStringPoints(String) parseStringPoints()})
     */
    public Polyline(PathObject pathObject, String pointString) {
        super(pathObject);

        this.pointString = pointString;
        this.type = TYPE;
    }

    @Override
    public ROI createROI() {
        return ROIs.createPolylineROI(parseStringPoints(pointString == null ? "" : pointString), getPlane());
    }

    @Override
    public String toString() {
        return String.format("Polyline of points %s", pointString);
    }
}
