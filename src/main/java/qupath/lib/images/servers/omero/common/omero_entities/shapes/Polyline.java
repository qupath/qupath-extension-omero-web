package qupath.lib.images.servers.omero.common.omero_entities.shapes;

import com.google.gson.annotations.SerializedName;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

/**
 * A set of lines.
 */
class Polyline extends Shape {
    @SerializedName(value = "Points", alternate = "points") private String pointString;

    /**
     * Creates a polyline.
     *
     * @param pointString  a list of points as returned by the OMERO API
     *                     (see {@link qupath.lib.images.servers.omero.common.omero_entities.shapes.Shape#parseStringPoints(String) parseStringPoints()})
     */
    Polyline(String pointString) {
        this.pointString = pointString;
    }

    @Override
    ROI createROI() {
        return ROIs.createPolylineROI(parseStringPoints(pointString == null ? "" : pointString), getPlane());
    }
}
