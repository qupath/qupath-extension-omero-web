package qupath.ext.omero.core.entities.shapes;

import com.google.gson.annotations.SerializedName;
import qupath.lib.objects.PathObject;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

/**
 * A rectangle.
 */
class Rectangle extends Shape {

    public static final String TYPE = TYPE_URL + "Rectangle";
    @SerializedName(value = "X", alternate = "x") private final double x;
    @SerializedName(value = "Y", alternate = "y") private final double y;
    @SerializedName(value = "Width", alternate = "width") private final double width;
    @SerializedName(value = "Height", alternate = "height") private final double height;

    /**
     * Creates a rectangle.
     *
     * @param pathObject  the path object corresponding to this shape
     * @param x  the x-coordinate of the top left point of the rectangle
     * @param y  the y-coordinate of the top left point of the rectangle
     * @param width  the width of the rectangle
     * @param height  the height of the rectangle
     */
    public Rectangle(PathObject pathObject, double x, double y, double width, double height) {
        super(pathObject);

        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.type = TYPE;
    }

    @Override
    public ROI createROI() {
        return ROIs.createRectangleROI(x, y, width, height, getPlane());
    }

    @Override
    public String toString() {
        return String.format("Rectangle located (top left) at {x: %f, y: %f} of width %f and height %f", x, y, width, height);
    }
}
