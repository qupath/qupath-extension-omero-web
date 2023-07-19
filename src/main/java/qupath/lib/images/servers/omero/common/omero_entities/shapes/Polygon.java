package qupath.lib.images.servers.omero.common.omero_entities.shapes;

import com.google.gson.annotations.SerializedName;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

/**
 * A polygon.
 */
class Polygon extends Shape {
    @SerializedName(value = "Points", alternate = "points") private String pointString;

    /**
     * Creates a polygon.
     *
     * @param pointString  a list of points as returned by the OMERO API
     *                     (see {@link qupath.lib.images.servers.omero.common.omero_entities.shapes.Shape#parseStringPoints(String) parseStringPoints()})
     */
    Polygon(String pointString) {
        this.pointString = pointString;
    }

    @Override
    ROI createROI() {
        return ROIs.createPolygonROI(parseStringPoints(pointString == null ? "" : pointString), getPlane());
    }
}
