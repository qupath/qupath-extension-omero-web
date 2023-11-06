package qupath.ext.omero.core.entities.shapes;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import qupath.lib.roi.LineROI;
import qupath.lib.roi.interfaces.ROI;

public class TestLine {

    @Test
    void Check_Line_Created() {
        Shape line = createLine();

        Class<? extends Shape> type = line.getClass();

        Assertions.assertEquals(Line.class, type);
    }

    @Test
    void Check_Line_ROI() {
        Shape line = createLine();

        Class<? extends ROI> roiClass = line.createROI().getClass();

        Assertions.assertEquals(LineROI.class, roiClass);
    }

    @Test
    void Check_Serialization_And_Deserialization() {
        Shape line = TestShape.createShapeFromJSON(TestShape.createJSONFromPathObject(createLine().createAnnotation()));

        Class<? extends Shape> type = line.getClass();

        Assertions.assertEquals(Line.class, type);
    }

    private Shape createLine() {
        return TestShape.createShapeFromJSON("""
                {
                    "@id": 713,
                    "text": "Annotation:Stroma:aba712b2-bbc2-4c05-bbba-d9fbab4d454f:NoParent",
                    "StrokeColor": -16776961,
                    "Locked": false,
                    "oldId": "454:713",
                    "@type": "http://www.openmicroscopy.org/Schemas/OME/2016-06#Line"
                }
                """);    // -16776961 is the integer representation of the red color in the BGR format
    }
}
