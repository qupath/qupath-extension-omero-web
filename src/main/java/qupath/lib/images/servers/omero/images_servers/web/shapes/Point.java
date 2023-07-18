package qupath.lib.images.servers.omero.images_servers.web.shapes;

import com.google.gson.annotations.SerializedName;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

class Point extends Shape {
    @SerializedName(value = "X", alternate = "x") private double x;
    @SerializedName(value = "Y", alternate = "y") private double y;

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public ROI createROI() {
        return ROIs.createPointsROI(x, y, getPlane());
    }
}
