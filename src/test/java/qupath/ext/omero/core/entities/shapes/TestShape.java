package qupath.ext.omero.core.entities.shapes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import qupath.lib.common.ColorTools;
import qupath.lib.objects.PathAnnotationObject;
import qupath.lib.objects.PathDetectionObject;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.TMACoreObject;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

import java.util.Set;
import java.util.UUID;

public class TestShape {

    public static Shape createShapeFromJSON(String json) {
        Gson gson = new GsonBuilder().registerTypeAdapter(Shape.class, new Shape.GsonShapeDeserializer()).setLenient().create();
        JsonElement jsonElement = JsonParser.parseString(json);
        if (jsonElement.isJsonArray()) {
            return gson.fromJson(JsonParser.parseString(json).getAsJsonArray().get(0), Shape.class);
        }
        if (jsonElement.isJsonObject()) {
            return gson.fromJson(JsonParser.parseString(json).getAsJsonObject(), Shape.class);
        }
        throw new IllegalArgumentException("Invalid JSON: " + json);
    }

    public static String createJSONFromPathObject(PathObject pathObject) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(TMACoreObject.class, new Shape.GsonShapeSerializer())
                .registerTypeAdapter(PathAnnotationObject.class, new Shape.GsonShapeSerializer())
                .registerTypeAdapter(PathDetectionObject.class, new Shape.GsonShapeSerializer())
                .serializeSpecialFloatingPointValues()
                .setLenient()
                .create();
        return gson.toJson(pathObject);
    }

    @Test
    void Check_Empty() {
        Shape shape = new Gson().fromJson("{}", ShapeImplementation.class);

        String id = shape.getQuPathId();

        Assertions.assertEquals("", id);
    }

    @Test
    void Check_Annotation_Type() {
        Shape shape = createShape();
        PathObject pathObject = shape.createAnnotation();

        boolean isAnnotation = pathObject.isAnnotation();

        Assertions.assertTrue(isAnnotation);
    }

    @Test
    void Check_Annotation_ID() {
        Shape shape = createShape();
        PathObject pathObject = shape.createAnnotation();

        UUID id = pathObject.getID();

        Assertions.assertEquals(UUID.fromString("aba712b2-bbc2-4c05-bbba-d9fbab4d454f"), id);
    }

    @Test
    void Check_Annotation_Class() {
        Shape shape = createShape();
        PathObject pathObject = shape.createAnnotation();

        Set<String> classifications = pathObject.getClassifications();

        Assertions.assertArrayEquals(
                new String[] {"Stroma"},
                classifications.toArray()
        );
    }

    @Test
    void Check_Annotation_ROI() {
        Shape shape = createShape();
        PathObject pathObject = shape.createAnnotation();

        ROI roi = pathObject.getROI();

        Assertions.assertTrue(roi.isEmpty());
    }

    @Test
    void Check_Annotation_Color() {
        Shape shape = createShape();
        PathObject pathObject = shape.createAnnotation();

        Integer color = pathObject.getColor();

        Assertions.assertEquals(ColorTools.RED, color);
    }

    @Test
    void Check_Annotation_Lock() {
        Shape shape = createShape();
        PathObject pathObject = shape.createAnnotation();

        boolean lock = pathObject.isLocked();

        Assertions.assertFalse(lock);
    }

    @Test
    void Check_Shape_ID() {
        Shape shape = createShape();

        String id = shape.getQuPathId();

        Assertions.assertEquals("aba712b2-bbc2-4c05-bbba-d9fbab4d454f", id);
    }

    @Test
    void Check_Shape_Parent_ID() {
        Shape shape = createShape();

        String id = shape.getQuPathParentId();

        Assertions.assertEquals("NoParent", id);
    }

    @Test
    void Check_Old_ID() {
        Shape shape = createShape();

        String id = shape.getOldId();

        Assertions.assertEquals("454:713", id);
    }

    @Test
    void Check_Old_ID_After_Changed() {
        Shape shape = createShape();
        shape.setOldId(999);

        String id = shape.getOldId();

        Assertions.assertEquals("999:713", id);
    }

    private Shape createShape() {
        String json = """
                {
                    "@id": 713,
                    "text": "Annotation:Stroma:aba712b2-bbc2-4c05-bbba-d9fbab4d454f:NoParent",
                    "StrokeColor": -16776961,
                    "Locked": false,
                    "oldId": "454:713"
                }
                """;    // -16776961 is the integer representation of the red color in the BGR format
        return new Gson().fromJson(json, ShapeImplementation.class);
    }
}

class ShapeImplementation extends Shape {

    public ShapeImplementation(PathObject pathObject) {
        super(pathObject);
    }

    @Override
    protected ROI createROI() {
        return ROIs.createEmptyROI();
    }
}