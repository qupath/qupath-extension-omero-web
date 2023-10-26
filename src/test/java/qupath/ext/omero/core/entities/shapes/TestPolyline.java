package qupath.ext.omero.core.entities.shapes;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import qupath.lib.roi.PolylineROI;
import qupath.lib.roi.interfaces.ROI;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class TestPolyline {

    @Test
    void Check_Polyline_Created() {
        Shape polyline = createPolyline();

        Class<? extends Shape> type = polyline.getClass();

        Assertions.assertEquals(Polyline.class, type);
    }

    @Test
    void Check_Label_ROI() {
        Shape polyline = createPolyline();

        Class<? extends ROI> roiClass = polyline.createROI().getClass();

        Assertions.assertEquals(PolylineROI.class, roiClass);
    }

    @Test
    void Check_Serialization_And_Deserialization() {
        Shape polyline = TestShape.createShapeFromJSON(TestShape.createJSONFromPathObject(createPolyline().createAnnotation()));

        Class<? extends Shape> type = polyline.getClass();

        Assertions.assertEquals(Polyline.class, type);
    }

    private Shape createPolyline() {
        return TestShape.createShapeFromJSON("""
                {
                    "@id": 713,
                    "text": "Annotation:Stroma:aba712b2-bbc2-4c05-bbba-d9fbab4d454f:NoParent",
                    "StrokeColor": -16776961,
                    "Locked": false,
                    "oldId": "454:713",
                    "@type": "http://www.openmicroscopy.org/Schemas/OME/2016-06#Polyline"
                }
                """);    // -16776961 is the integer representation of the red color in the BGR format
    }
}
