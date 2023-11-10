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
import java.util.*;
import java.util.stream.Collectors;

/**
 * An OMERO shape represents a region that can be drawn to an image.
 */
public abstract class Shape {

    private static final Logger logger = LoggerFactory.getLogger(Shape.class);
    protected static String TYPE_URL = "http://www.openmicroscopy.org/Schemas/OME/2016-06#";
    @SerializedName(value = "@type") private String type;
    @SerializedName(value = "@id") private int id;
    @SerializedName(value = "TheC") private Integer c;
    @SerializedName(value = "TheZ") private int z;
    @SerializedName(value = "TheT") private int t;
    @SerializedName(value = "Text", alternate = "text") private String text;
    @SerializedName(value = "Locked", alternate = "locked") private Boolean locked;
    @SerializedName(value = "FillColor", alternate = "fillColor") private int fillColor;
    @SerializedName(value = "StrokeColor", alternate = "strokeColor") private Integer strokeColor;
    @SerializedName(value = "oldId") private String oldId = "-1:-1";

    protected Shape(String type) {
        this.type = type;
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
     * Create a list of shapes from a path object.
     *
     * @param pathObject  the path object that represents one or more shapes
     * @return a list of shapes corresponding to this path object
     */
    public static List<Shape> createFromPathObject(PathObject pathObject) {
        ROI roi = pathObject.getROI();

        if (roi instanceof RectangleROI) {
            return List.of(new Rectangle(pathObject));
        } else if (roi instanceof EllipseROI) {
            return List.of(new Ellipse(pathObject));
        } else if (roi instanceof LineROI lineRoi) {
            return List.of(new Line(pathObject, lineRoi));
        } else if (roi instanceof PolylineROI) {
            return List.of(new Polyline(pathObject));
        } else if (roi instanceof PolygonROI) {
            return List.of(new Polygon(pathObject, pathObject.getROI()));
        } else if (roi instanceof PointsROI) {
            return new ArrayList<>(Point.create(pathObject));
        } else if (roi instanceof GeometryROI) {
            logger.info("OMERO shapes do not support holes.");
            logger.warn("MultiPolygon will be split for OMERO compatibility.");

            return new ArrayList<>(RoiTools.splitROI(RoiTools.fillHoles(roi)).stream()
                    .map(r -> new Polygon(pathObject, r))
                    .toList()
            );
        } else {
            logger.warn(String.format("Unsupported type: %s", roi.getRoiName()));
            return List.of();
        }
    }

    /**
     * @return a path object built from this shape
     */
    public PathObject createPathObject() {
        String[] parsedComment = parseComment();
        PathClass classes = PathClass.fromCollection(Arrays.stream(parsedComment[1].split("&")).toList());

        PathObject pathObject;
        if (parsedComment[0].equals("Detection")) {
            pathObject = PathObjects.createDetectionObject(createROI(), classes);
        } else {
            pathObject = PathObjects.createAnnotationObject(createROI(), classes);
        }

        pathObject.setID(getQuPathId());

        if (strokeColor != null)
            pathObject.setColor(strokeColor >> 8);

        if (locked != null)
            pathObject.setLocked(locked);

        return pathObject;
    }

    /**
     * @return the ID of the QuPath annotation corresponding to this shape
     */
    public UUID getQuPathId() {
        String[] parsedComment = parseComment();
        try {
            return UUID.fromString(parsedComment[2]);
        } catch (IllegalArgumentException e) {
            UUID uuid = UUID.randomUUID();

            parsedComment[2] = uuid.toString();
            text = String.join(":", parsedComment);

            return uuid;
        }
    }

    /**
     * @return the ID of the QuPath annotation corresponding to the parent of this shape,
     * or an empty optional if this shape has no parent
     */
    public Optional<UUID> getQuPathParentId() {
        try {
            return Optional.of(UUID.fromString(parseComment()[3]));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
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
     * @return the ROI that corresponds to this shape
     */
    protected abstract ROI createROI();

    /**
     * <p>
     *     Link this shape with a path object.
     * </p>
     * <p>
     *     Its text will be formatted as {@code Type:Class1&Class2:ObjectID:ParentID},
     *     for example {@code Annotation:NoClass:aba712b2-bbc2-4c05-bbba-d9fbab4d454f:NoParent}
     *     or {@code Detection:Stroma:aba712b2-bbc2-4c05-bbba-d9fbab4d454f:205037ff-7dd7-4549-89d8-a4e3cbf61294}
     * </p>
     *
     * @param pathObject  the path object that should correspond to this shape
     */
    protected void linkWithPathObject(PathObject pathObject) {
        this.text = String.format(
                "%s:%s:%s:%s",
                pathObject.isDetection() ? "Detection" : "Annotation",
                pathObject.getPathClass() == null ? "NoClass" : pathObject.getPathClass().toString().replaceAll(":","&"),
                pathObject.getID().toString(),
                pathObject.getParent() == null ? "NoParent" : pathObject.getParent().getID().toString()
        );

        if (pathObject.getPathClass() != null) {
            fillColor = -256;	// Transparent
            strokeColor = ARGBToRGBA(pathObject.getPathClass().getColor());
        } else {
            fillColor = -256;	// Transparent
            strokeColor = ARGBToRGBA(PathPrefs.colorDefaultObjectsProperty().get());
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

    private String[] parseComment() {
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
    protected static String pointsToString(List<Point2> points) {
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
