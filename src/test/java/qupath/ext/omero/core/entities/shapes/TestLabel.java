package qupath.ext.omero.core.entities.shapes;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import qupath.lib.roi.PointsROI;
import qupath.lib.roi.interfaces.ROI;

public class TestLabel {

    @Test
    void Check_Label_Created() {
        Shape label = createLabel();

        Class<? extends Shape> type = label.getClass();

        Assertions.assertEquals(Label.class, type);
    }

    @Test
    void Check_Label_ROI() {
        Shape label = createLabel();

        Class<? extends ROI> roiClass = label.createROI().getClass();

        Assertions.assertEquals(PointsROI.class, roiClass); // Labels are unsupported and converted to points
    }

    @Test
    void Check_Serialization_And_Deserialization() {
        Shape label = TestShape.createShapeFromJSON(TestShape.createJSONFromPathObject(createLabel().createAnnotation()));

        Class<? extends Shape> type = label.getClass();

        Assertions.assertEquals(Point.class, type); // Labels are unsupported and converted to points
    }

    private Shape createLabel() {
        return TestShape.createShapeFromJSON("""
                {
                    "@id": 713,
                    "text": "Annotation:Stroma:aba712b2-bbc2-4c05-bbba-d9fbab4d454f:NoParent",
                    "StrokeColor": -16776961,
                    "Locked": false,
                    "oldId": "454:713",
                    "@type": "http://www.openmicroscopy.org/Schemas/OME/2016-06#Label"
                }
                """);    // -16776961 is the integer representation of the red color in the BGR format
    }
}
