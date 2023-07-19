package qupath.lib.images.servers.omero.common.omero_entities.shapes;

import com.google.gson.annotations.SerializedName;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

/**
 * A rectangle.
 */
class Rectangle extends Shape {
    @SerializedName(value = "X", alternate = "x") private double x;
    @SerializedName(value = "Y", alternate = "y") private double y;
    @SerializedName(value = "Width", alternate = "width") private double width;
    @SerializedName(value = "Height", alternate = "height") private double height;

    /**
     * Creates a rectangle.
     *
     * @param x  the x-coordinate of the top left point of the rectangle
     * @param y  the y-coordinate of the top left point of the rectangle
     * @param width  the width of the rectangle
     * @param height  the height of the rectangle
     */
    Rectangle(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    ROI createROI() {
        return ROIs.createRectangleROI(x, y, width, height, getPlane());
    }
}
