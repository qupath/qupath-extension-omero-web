package qupath.lib.images.servers.omero.common.omeroentities.shapes;

import com.google.gson.annotations.SerializedName;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

/**
 * An ellipse.
 */
class Ellipse extends Shape {
    @SerializedName(value = "X", alternate = "x") private double x;
    @SerializedName(value = "Y", alternate = "y") private double y;
    @SerializedName(value = "RadiusX", alternate = "radiusX") private double radiusX;
    @SerializedName(value = "RadiusY", alternate = "radiusY") private double radiusY;

    /**
     * Creates an ellipse.
     *
     * @param x  x-coordinate of the center of the ellipse
     * @param y  y-coordinate of the center of the ellipse
     * @param radiusX  radius along the x-axis
     * @param radiusY  radius along the y-axis
     */
    Ellipse(double x, double y, double radiusX, double radiusY) {
        this.x = x;
        this.y = y;
        this.radiusX = radiusX;
        this.radiusY = radiusY;
    }

    @Override
    ROI createROI() {
        return ROIs.createEllipseROI(x-radiusX, y-radiusY, radiusX*2, radiusY*2, getPlane());
    }
}
