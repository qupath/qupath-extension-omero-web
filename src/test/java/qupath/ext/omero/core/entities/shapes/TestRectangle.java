package qupath.ext.omero.core.entities.shapes;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import qupath.lib.roi.RectangleROI;
import qupath.lib.roi.interfaces.ROI;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class TestRectangle {

    @Test
    void Check_Rectangle_Created() {
        Shape rectangle = createRectangle();

        Class<? extends Shape> type = rectangle.getClass();

        Assertions.assertEquals(Rectangle.class, type);
    }

    @Test
    void Check_Label_ROI() {
        Shape rectangle = createRectangle();

        Class<? extends ROI> roiClass = rectangle.createROI().getClass();

        Assertions.assertEquals(RectangleROI.class, roiClass);
    }

    @Test
    void Check_Serialization_And_Deserialization() {
        Shape rectangle = TestShape.createShapeFromJSON(TestShape.createJSONFromPathObject(createRectangle().createAnnotation()));

        Class<? extends Shape> type = rectangle.getClass();

        Assertions.assertEquals(Rectangle.class, type);
    }

    private Shape createRectangle() {
        return TestShape.createShapeFromJSON("""
                {
                    "@id": 713,
                    "text": "Annotation:Stroma:aba712b2-bbc2-4c05-bbba-d9fbab4d454f:NoParent",
                    "StrokeColor": -16776961,
                    "Locked": false,
                    "oldId": "454:713",
                    "@type": "http://www.openmicroscopy.org/Schemas/OME/2016-06#Rectangle"
                }
                """);    // -16776961 is the integer representation of the red color in the BGR format
    }
}
