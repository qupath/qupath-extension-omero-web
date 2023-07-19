package qupath.lib.images.servers.omero.common.omero_entities.shapes;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.geom.Point2;
import qupath.lib.gui.prefs.PathPrefs;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.PathObjects;
import qupath.lib.objects.classes.PathClass;
import qupath.lib.regions.ImagePlane;
import qupath.lib.roi.*;
import qupath.lib.roi.interfaces.ROI;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * An OMERO shape represents a region that can be drawn to an image.
 */
public abstract class Shape {
    private static final Logger logger = LoggerFactory.getLogger(Shape.class);
    @SerializedName(value = "TheC") private Integer c = -1;
    @SerializedName(value = "TheZ") private int z;
    @SerializedName(value = "TheT") private int t;
    @SerializedName(value = "@type") private String type;
    @SerializedName(value = "Text", alternate = "text") private String text;
    @SerializedName(value = "Locked", alternate = "locked") private Boolean locked;
    @SerializedName(value = "FillColor", alternate = "fillColor") private int fillColor;
    @SerializedName(value = "StrokeColor", alternate = "strokeColor") private Integer strokeColor;
    @SerializedName(value = "oldId") private String oldId = "-1:-1";

    /**
     * @return the ROI that corresponds to this shape
     */
    abstract ROI createROI();

    /**
     * @return a PathObject build from the ROI corresponding to this shape
     */
    public PathObject createAnnotation() {
        var pathObject = PathObjects.createAnnotationObject(createROI());
        initializeObject(pathObject);

        return pathObject;
    }

    /**
     * Class that deserializes a JSON into a shape
     */
    public static class GsonShapeDeserializer implements JsonDeserializer<Shape> {
        @Override
        public Shape deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            try {
                String type = json.getAsJsonObject().get("@type").getAsString().toLowerCase();

                if (type.endsWith("#rectangle"))
                    return context.deserialize(json, Rectangle.class);
                if (type.endsWith("#ellipse"))
                    return context.deserialize(json, Ellipse.class);
                if (type.endsWith("#line"))
                    return context.deserialize(json, Line.class);
                if (type.endsWith("#polygon"))
                    return context.deserialize(json, Polygon.class);
                if (type.endsWith("#polyline"))
                    return context.deserialize(json, Polyline.class);
                if (type.endsWith("#point"))
                    return context.deserialize(json, Point.class);
                if (type.endsWith("#label"))
                    return context.deserialize(json, Label.class);

                logger.warn("Unsupported type {}", type);
                return null;
            } catch (Exception e) {
                logger.error("Could not deserialize " + json, e);
                return null;
            }
        }
    }

    /**
     * Class that serializes a shape into a JSON
     */
    public static class GsonShapeSerializer implements JsonSerializer<PathObject> {
        @Override
        public JsonElement serialize(PathObject src, Type typeOfSrc, JsonSerializationContext context) {
            ROI roi = src.getROI();
            Type type;
            Shape shape;

            if (roi instanceof RectangleROI) {
                type = Rectangle.class;
                shape = new Rectangle(roi.getBoundsX(), roi.getBoundsY(), roi.getBoundsWidth(), roi.getBoundsHeight());
                shape.setType("Rectangle");
            } else if (roi instanceof EllipseROI) {
                type = Ellipse.class;
                shape = new Ellipse(roi.getCentroidX(), roi.getCentroidY(), roi.getBoundsWidth()/2, roi.getBoundsHeight()/2);
                shape.setType("Ellipse");
            } else if (roi instanceof LineROI lineRoi) {
                type = Line.class;
                shape = new Line(lineRoi.getX1(), lineRoi.getY1(), lineRoi.getX2(), lineRoi.getY2());
                shape.setType("Line");
            } else if (roi instanceof PolylineROI) {
                type = Polyline.class;
                shape = new Polyline(pointsToString(roi.getAllPoints()));
                shape.setType("Polyline");
            } else if (roi instanceof PolygonROI) {
                type = Polygon.class;
                shape = new Polygon(pointsToString(roi.getAllPoints()));
                shape.setType("Polygon");
            } else if (roi instanceof PointsROI) {
                JsonElement[] points = new JsonElement[roi.getNumPoints()];
                List<Point2> roiPoints = roi.getAllPoints();
                PathClass pathClass = src.getPathClass();

                for (int i = 0; i < roiPoints.size(); i++) {
                    shape = new Point(roiPoints.get(i).getX(), roiPoints.get(i).getY());
                    shape.setType("Point");
                    shape.text = src.getName() != null ? src.getName() : "";
                    shape.fillColor = pathClass != null ? ARGBToRGBA(src.getPathClass().getColor()) : -256;
                    points[i] = context.serialize(shape, Point.class);
                }
                return context.serialize(points);
            } else if (roi instanceof GeometryROI) {
                logger.info("OMERO shapes do not support holes.");
                logger.warn("MultiPolygon will be split for OMERO compatibility.");

                roi = RoiTools.fillHoles(roi);
                PathClass pathClass = src.getPathClass();

                List<ROI> rois = RoiTools.splitROI(roi);
                JsonElement[] polygons = new JsonElement[rois.size()];

                for (int i = 0; i < polygons.length; i++) {
                    shape = new Polygon(pointsToString(rois.get(i).getAllPoints()));
                    shape.setType("Polygon");
                    shape.text = src.getName() != null ? src.getName() : "";
                    shape.fillColor = pathClass != null ? ARGBToRGBA(pathClass.getColor()) : -256;
                    polygons[i] = context.serialize(shape, Polygon.class);
                }
                return context.serialize(polygons);
            } else {
                logger.warn("Unsupported type {}", roi.getRoiName());
                return null;
            }

            // Set the appropriate colors
            if (src.getPathClass() != null) {
                int classColor = ARGBToRGBA(src.getPathClass().getColor());
                shape.fillColor = classColor;
                shape.strokeColor = classColor;
            } else {
                shape.fillColor = -256;	// Transparent
                shape.strokeColor = ARGBToRGBA(PathPrefs.colorDefaultObjectsProperty().get()); // Default Qupath object color
            }

            shape.text = src.getName() != null ? src.getName() : "";
            return context.serialize(shape, type);
        }
    }

    /**
     * Parse the OMERO string representing points into a list.
     *
     * @param pointsString  a String describing a list of points returned by the OMERO API,
     *                      for example "2,3 4,2 7,9"
     * @return a list of points corresponding to the input
     */
    protected static List<Point2> parseStringPoints(String pointsString) {
        return Arrays.stream(pointsString.split(" "))
                .map(pointStr -> {
                    String[] point = pointStr.split(",");
                    if (point.length > 1) {
                        return new Point2(Double.parseDouble(point[0]), Double.parseDouble(point[1]));
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * @return the ImagePlane corresponding to this shape
     */
    protected ImagePlane getPlane() {
        return c == null ? ImagePlane.getPlane(z, t) : ImagePlane.getPlaneWithChannel(c, z, t);
    }

    private void initializeObject(PathObject pathObject) {
        if (text != null && !text.isBlank())
            pathObject.setName(text);
        if (strokeColor != null)
            pathObject.setColor(strokeColor >> 8);
        if (locked != null)
            pathObject.setLocked(locked);
    }

    private void setType(String type) {
        this.type = "http://www.openmicroscopy.org/Schemas/OME/2016-06#" + type;
    }

    /**
     * Converts the specified list of {@code Point2}s into an OMERO-friendly string
     * @return string of points
     */
    private static String pointsToString(List<Point2> points) {
        return points.stream()
                .map(point -> point.getX() + "," + point.getY())
                .collect(Collectors.joining (" "));
    }

    private static int ARGBToRGBA(int argb) {
        int a =  (argb >> 24) & 0xff;
        int r =  (argb >> 16) & 0xff;
        int g =  (argb >> 8) & 0xff;
        int b =  argb & 0xff;
        return (r<<24) + (g<<16) + (b<<8) + a;
    }
}
