package qupath.ext.omero.core.entities.shapes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import qupath.lib.common.ColorTools;
import qupath.lib.objects.PathObject;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

import java.util.Optional;
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

    @Test
    void Check_Empty() {
        Shape shape = new Gson().fromJson("{}", ShapeImplementation.class);

        Optional<UUID> id = shape.getQuPathParentId();

        Assertions.assertTrue(id.isEmpty());
    }

    @Test
    void Check_Path_Object_Is_Annotation() {
        Shape shape = createShape();
        PathObject pathObject = shape.createPathObject();

        boolean isAnnotation = pathObject.isAnnotation();

        Assertions.assertTrue(isAnnotation);
    }

    @Test
    void Check_Path_Object_ID() {
        Shape shape = createShape();
        PathObject pathObject = shape.createPathObject();

        UUID id = pathObject.getID();

        Assertions.assertEquals(UUID.fromString("aba712b2-bbc2-4c05-bbba-d9fbab4d454f"), id);
    }

    @Test
    void Check_Path_Object_Class() {
        Shape shape = createShape();
        PathObject pathObject = shape.createPathObject();

        Set<String> classifications = pathObject.getClassifications();

        Assertions.assertArrayEquals(
                new String[] {"Stroma"},
                classifications.toArray()
        );
    }

    @Test
    void Check_Path_Object_ROI() {
        Shape shape = createShape();
        PathObject pathObject = shape.createPathObject();

        ROI roi = pathObject.getROI();

        Assertions.assertTrue(roi.isEmpty());
    }

    @Test
    void Check_Path_Object_Color() {
        Shape shape = createShape();
        PathObject pathObject = shape.createPathObject();

        Integer color = pathObject.getColor();

        Assertions.assertEquals(ColorTools.RED, color);
    }

    @Test
    void Check_Path_Object_Lock() {
        Shape shape = createShape();
        PathObject pathObject = shape.createPathObject();

        boolean lock = pathObject.isLocked();

        Assertions.assertFalse(lock);
    }

    @Test
    void Check_ID() {
        Shape shape = createShape();

        UUID id = shape.getQuPathId();

        Assertions.assertEquals(UUID.fromString("aba712b2-bbc2-4c05-bbba-d9fbab4d454f"), id);
    }

    @Test
    void Check_Parent_ID() {
        Shape shape = createShape();

        UUID id = shape.getQuPathParentId().orElse(null);

        Assertions.assertEquals(UUID.fromString("dfa7dfb2-fd32-4c05-bbba-d9fbab4d454f"), id);
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
                    "text": "Annotation:Stroma:aba712b2-bbc2-4c05-bbba-d9fbab4d454f:dfa7dfb2-fd32-4c05-bbba-d9fbab4d454f",
                    "StrokeColor": -16776961,
                    "Locked": false,
                    "oldId": "454:713"
                }
                """;    // -16776961 is the integer representation of the red color in the BGR format
        return new Gson().fromJson(json, ShapeImplementation.class);
    }

    private static class ShapeImplementation extends Shape {

        protected ShapeImplementation(String type) {
            super(type);
        }

        @Override
        protected ROI createROI() {
            return ROIs.createEmptyROI();
        }
    }
}

