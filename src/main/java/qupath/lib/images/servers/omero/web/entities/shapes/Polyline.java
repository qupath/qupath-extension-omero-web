package qupath.lib.images.servers.omero.web.entities.shapes;

import com.google.gson.annotations.SerializedName;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

/**
 * A set of lines.
 */
class Polyline extends Shape {

    @SerializedName(value = "Points", alternate = "points") private final String pointString;

    /**
     * Creates a polyline.
     *
     * @param pointString  a list of points as returned by the OMERO API
     *                     (see {@link Shape#parseStringPoints(String) parseStringPoints()})
     */
    public Polyline(String pointString) {
        this.pointString = pointString;
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
