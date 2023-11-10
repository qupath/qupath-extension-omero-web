package qupath.ext.omero.core.entities.shapes;

import com.google.gson.annotations.SerializedName;
import qupath.lib.objects.PathObject;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

import java.util.Objects;

/**
 * A rectangle.
 */
public class Rectangle extends Shape {

    public static final String TYPE = TYPE_URL + "Rectangle";
    @SerializedName(value = "X", alternate = "x") private final double x;
    @SerializedName(value = "Y", alternate = "y") private final double y;
    @SerializedName(value = "Width", alternate = "width") private final double width;
    @SerializedName(value = "Height", alternate = "height") private final double height;

    /**
     * Creates a rectangle.
     *
     * @param x  the x-coordinate of the top left point of the rectangle
     * @param y  the y-coordinate of the top left point of the rectangle
     * @param width  the width of the rectangle
     * @param height  the height of the rectangle
     */
    public Rectangle(
            double x,
            double y,
            double width,
            double height
    ) {
        super(TYPE);

        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Creates a rectangle corresponding to a path object.
     *
     * @param pathObject  the path object corresponding to this shape
     */
    public Rectangle(PathObject pathObject) {
        this(
                pathObject.getROI().getBoundsX(),
                pathObject.getROI().getBoundsY(),
                pathObject.getROI().getBoundsWidth(),
                pathObject.getROI().getBoundsHeight()
        );

        linkWithPathObject(pathObject);
    }

    @Override
    public ROI createROI() {
        return ROIs.createRectangleROI(x, y, width, height, getPlane());
    }

    @Override
    public String toString() {
        return String.format("Rectangle located (top left) at {x: %f, y: %f} of width %f and height %f", x, y, width, height);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof Rectangle rectangle))
            return false;
        return rectangle.x == this.x && rectangle.y == this.y && rectangle.width == this.width && rectangle.height == this.height;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, width, height);
    }
}
