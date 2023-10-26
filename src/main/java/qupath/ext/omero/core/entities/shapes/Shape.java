package qupath.ext.omero.core.entities.shapes;

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
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * An OMERO shape represents a region that can be drawn to an image.
 */
public abstract class Shape {

    private static final Logger logger = LoggerFactory.getLogger(Shape.class);
    protected static String TYPE_URL = "http://www.openmicroscopy.org/Schemas/OME/2016-06#";
    @SerializedName(value = "@type") protected String type;
    @SerializedName(value = "@id") private int id;
    @SerializedName(value = "TheC") private Integer c;
    @SerializedName(value = "TheZ") private int z;
    @SerializedName(value = "TheT") private int t;
    @SerializedName(value = "Text", alternate = "text") private String text;
    @SerializedName(value = "Locked", alternate = "locked") private Boolean locked;
    @SerializedName(value = "FillColor", alternate = "fillColor") private int fillColor;
    @SerializedName(value = "StrokeColor", alternate = "strokeColor") private Integer strokeColor;
    @SerializedName(value = "oldId") private String oldId = "-1:-1";

    /**
     * <p>
     *     Create a new shape.
     * </p>
     * <p>
     *     Its text will be formatted as {@code Type:Class1&Class2:ObjectID:ParentID},
     *     for example {@code Annotation:NoClass:aba712b2-bbc2-4c05-bbba-d9fbab4d454f:NoParent}
     *     or {@code Detection:Stroma:aba712b2-bbc2-4c05-bbba-d9fbab4d454f:205037ff-7dd7-4549-89d8-a4e3cbf61294}
     * </p>
     *
     * @param pathObject  the path object corresponding to this shape
     */
    public Shape(PathObject pathObject) {
        this.text = String.format(
                "%s:%s:%s:%s",
                pathObject.isDetection() ? "Detection" : "Annotation",
                pathObject.getPathClass() == null ? "NoClass" : pathObject.getPathClass().toString().replaceAll(":","&"),
                pathObject.getID().toString(),
                pathObject.getParent() == null ? "NoParent" : pathObject.getParent().getID().toString()
        );
    }

    /**
     * @return a PathObject built from the ROI corresponding to this shape
     */
    public PathObject createAnnotation() {
        String[] parsedComment = parseROIComment();
        PathClass classes = PathClass.fromCollection(Arrays.stream(parsedComment[1].split("&")).toList());

        PathObject pathObject;
        if (parsedComment[0].equals("Detection")) {
            pathObject = PathObjects.createDetectionObject(createROI(), classes);
        } else {
            pathObject = PathObjects.createAnnotationObject(createROI(), classes);
        }

        try {
            pathObject.setID(UUID.fromString(parsedComment[2]));
        } catch (IllegalArgumentException e) {
            logger.warn(String.format("%s is not a valid UUID", parsedComment[2]));
        }

        if (strokeColor != null)
            pathObject.setColor(strokeColor >> 8);

        if (locked != null)
            pathObject.setLocked(locked);

        return pathObject;
    }

    /**
     * @return the ID of the QuPath annotation corresponding to this shape
     */
    public String getQuPathId() {
        return parseROIComment()[2];
    }

    /**
     * @return the ID of the QuPath annotation corresponding to the parent of this shape
     */
    public String getQuPathParentId() {
        return parseROIComment()[3];
    }

    /**
     * <p>
     *     Set the {@code oldId} field of this shape.
     * </p>
     * <p>
     *     This corresponds to "roiID:shapeID" (see
     *     <a href="https://docs.openmicroscopy.org/omero/latest/developers/json-api.html#rois-and-shapes">here</a>
     *     for the difference between ROI ID and shape ID).
     * </p>
     *
     * @param roiID the ROI ID (as explained above)
     */
    public void setOldId(int roiID) {
        oldId = String.format("%d:%d", roiID, id);
    }

    /**
     * @return the {@code oldId} field of this shape (see {@link #setOldId(int)})
     */
    public String getOldId() {
        return oldId;
    }

    /**
     * Class that deserializes a JSON into a shape
     */
    public static class GsonShapeDeserializer implements JsonDeserializer<Shape> {
        @Override
        public Shape deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            try {
                String type = json.getAsJsonObject().get("@type").getAsString();

                if (type.equalsIgnoreCase(Rectangle.TYPE))
                    return context.deserialize(json, Rectangle.class);
                if (type.equalsIgnoreCase(Ellipse.TYPE))
                    return context.deserialize(json, Ellipse.class);
                if (type.equalsIgnoreCase(Line.TYPE))
                    return context.deserialize(json, Line.class);
                if (type.equalsIgnoreCase(Polygon.TYPE))
                    return context.deserialize(json, Polygon.class);
                if (type.equalsIgnoreCase(Polyline.TYPE))
                    return context.deserialize(json, Polyline.class);
                if (type.equalsIgnoreCase(Point.TYPE))
                    return context.deserialize(json, Point.class);
                if (type.equalsIgnoreCase(Label.TYPE))
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
                shape = new Rectangle(src, roi.getBoundsX(), roi.getBoundsY(), roi.getBoundsWidth(), roi.getBoundsHeight());
            } else if (roi instanceof EllipseROI) {
                type = Ellipse.class;
                shape = new Ellipse(src, roi.getCentroidX(), roi.getCentroidY(), roi.getBoundsWidth()/2, roi.getBoundsHeight()/2);
            } else if (roi instanceof LineROI lineRoi) {
                type = Line.class;
                shape = new Line(src, lineRoi.getX1(), lineRoi.getY1(), lineRoi.getX2(), lineRoi.getY2());
            } else if (roi instanceof PolylineROI) {
                type = Polyline.class;
                shape = new Polyline(src, pointsToString(roi.getAllPoints()));
            } else if (roi instanceof PolygonROI) {
                type = Polygon.class;
                shape = new Polygon(src, pointsToString(roi.getAllPoints()));
            } else if (roi instanceof PointsROI) {
                JsonElement[] points = new JsonElement[roi.getNumPoints()];
                List<Point2> roiPoints = roi.getAllPoints();
                PathClass pathClass = src.getPathClass();

                for (int i = 0; i < roiPoints.size(); i++) {
                    shape = new Point(src, roiPoints.get(i).getX(), roiPoints.get(i).getY());
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
                    shape = new Polygon(src, pointsToString(rois.get(i).getAllPoints()));
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
                shape.fillColor = -256;	// Transparent
                shape.strokeColor = ARGBToRGBA(src.getPathClass().getColor());
            } else {
                shape.fillColor = -256;	// Transparent
                shape.strokeColor = ARGBToRGBA(PathPrefs.colorDefaultObjectsProperty().get());
            }

            return context.serialize(shape, type);
        }
    }

    /**
     * @return the ROI that corresponds to this shape
     */
    protected abstract ROI createROI();

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

    private String[] parseROIComment() {
        String[] parsedComment = {
                "Annotation",
                "NoClass",
                "",
                "NoParent"
        };
        if (text != null) {
            String[] tokens = text.split(":");

            for (int i=0; i<4; ++i) {
                if (tokens.length > i && !tokens[i].isEmpty()) {
                    parsedComment[i] = tokens[i];
                }
            }
        }

        return parsedComment;
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
