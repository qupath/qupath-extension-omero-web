package qupath.ext.omero.core.entities.shapes;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import qupath.lib.roi.PointsROI;
import qupath.lib.roi.interfaces.ROI;

public class TestPoint {

    @Test
    void Check_Point_Created() {
        Shape point = createPoint();

        Class<? extends Shape> type = point.getClass();

        Assertions.assertEquals(Point.class, type);
    }

    @Test
    void Check_Label_ROI() {
        Shape point = createPoint();

        Class<? extends ROI> roiClass = point.createROI().getClass();

        Assertions.assertEquals(PointsROI.class, roiClass);
    }

    @Test
    void Check_Serialization_And_Deserialization() {
        Shape point = TestShape.createShapeFromJSON(TestShape.createJSONFromPathObject(createPoint().createAnnotation()));

        Class<? extends Shape> type = point.getClass();

        Assertions.assertEquals(Point.class, type);
    }

    private Shape createPoint() {
        return TestShape.createShapeFromJSON("""
                {
                    "@id": 713,
                    "text": "Annotation:Stroma:aba712b2-bbc2-4c05-bbba-d9fbab4d454f:NoParent",
                    "StrokeColor": -16776961,
                    "Locked": false,
                    "oldId": "454:713",
                    "@type": "http://www.openmicroscopy.org/Schemas/OME/2016-06#Point"
                }
                """);    // -16776961 is the integer representation of the red color in the BGR format
    }
}
