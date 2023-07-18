package qupath.lib.images.servers.omero.images_servers.web.shapes;

import com.google.gson.annotations.SerializedName;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

class Ellipse extends Shape {
    @SerializedName(value = "X", alternate = "x") private double x;
    @SerializedName(value = "Y", alternate = "y") private double y;
    @SerializedName(value = "RadiusX", alternate = "radiusX") private double radiusX;
    @SerializedName(value = "RadiusY", alternate = "radiusY") private double radiusY;

    public Ellipse(double x, double y, double radiusX, double radiusY) {
        this.x = x;
        this.y = y;
        this.radiusX = radiusX;
        this.radiusY = radiusY;
    }

    @Override
    public ROI createROI() {
        return ROIs.createEllipseROI(x-radiusX, y-radiusY, radiusX*2, radiusY*2, getPlane());
    }
}
