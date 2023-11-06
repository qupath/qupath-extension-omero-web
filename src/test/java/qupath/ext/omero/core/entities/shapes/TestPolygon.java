package qupath.ext.omero.core.entities.shapes;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import qupath.lib.roi.PolygonROI;
import qupath.lib.roi.interfaces.ROI;

public class TestPolygon {

    @Test
    void Check_Polygon_Created() {
        Shape polygon = createPolygon();

        Class<? extends Shape> type = polygon.getClass();

        Assertions.assertEquals(Polygon.class, type);
    }

    @Test
    void Check_Label_ROI() {
        Shape polygon = createPolygon();

        Class<? extends ROI> roiClass = polygon.createROI().getClass();

        Assertions.assertEquals(PolygonROI.class, roiClass);
    }

    @Test
    void Check_Serialization_And_Deserialization() {
        Shape polygon = TestShape.createShapeFromJSON(TestShape.createJSONFromPathObject(createPolygon().createAnnotation()));

        Class<? extends Shape> type = polygon.getClass();

        Assertions.assertEquals(Polygon.class, type);
    }

    private Shape createPolygon() {
        return TestShape.createShapeFromJSON("""
                {
                    "@id": 713,
                    "text": "Annotation:Stroma:aba712b2-bbc2-4c05-bbba-d9fbab4d454f:NoParent",
                    "StrokeColor": -16776961,
                    "Locked": false,
                    "oldId": "454:713",
                    "@type": "http://www.openmicroscopy.org/Schemas/OME/2016-06#Polygon"
                }
                """);    // -16776961 is the integer representation of the red color in the BGR format
    }
}
