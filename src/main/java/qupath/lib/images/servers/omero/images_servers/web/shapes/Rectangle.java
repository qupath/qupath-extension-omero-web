package qupath.lib.images.servers.omero.images_servers.web.shapes;

import com.google.gson.annotations.SerializedName;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

class Rectangle extends Shape {
    @SerializedName(value = "X", alternate = "x") private double x;
    @SerializedName(value = "Y", alternate = "y") private double y;
    @SerializedName(value = "Width", alternate = "width") private double width;
    @SerializedName(value = "Height", alternate = "height") private double height;

    public Rectangle(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public ROI createROI() {
        return ROIs.createRectangleROI(x, y, width, height, getPlane());
    }
}
