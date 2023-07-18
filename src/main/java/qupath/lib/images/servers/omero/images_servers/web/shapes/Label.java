package qupath.lib.images.servers.omero.images_servers.web.shapes;

import com.google.gson.annotations.SerializedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

class Label extends Shape {
    final private static Logger logger = LoggerFactory.getLogger(Shape.class);
    @SerializedName(value = "X", alternate = "x") private double x;
    @SerializedName(value = "Y", alternate = "y") private double y;

    @Override
    public ROI createROI() {
        logger.warn("Creating point (requested label shape is unsupported)");
        return ROIs.createPointsROI(x, y, getPlane());
    }
}
