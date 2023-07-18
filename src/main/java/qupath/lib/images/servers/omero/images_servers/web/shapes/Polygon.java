package qupath.lib.images.servers.omero.images_servers.web.shapes;

import com.google.gson.annotations.SerializedName;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

class Polygon extends Shape {
    @SerializedName(value = "Points", alternate = "points") private String pointString;

    public Polygon(String pointString) {
        this.pointString = pointString;
    }

    @Override
    public ROI createROI() {
        return ROIs.createPolygonROI(parseStringPoints(pointString == null ? "" : pointString), getPlane());
    }
}
