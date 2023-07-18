package qupath.lib.images.servers.omero.images_servers.web.shapes;

import com.google.gson.annotations.SerializedName;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

class Polyline extends Shape {
    @SerializedName(value = "Points", alternate = "points") private String pointString;

    public Polyline(String pointString) {
        this.pointString = pointString;
    }

    @Override
    public ROI createROI() {
        return ROIs.createPolylineROI(parseStringPoints(pointString == null ? "" : pointString), getPlane());
    }
}
