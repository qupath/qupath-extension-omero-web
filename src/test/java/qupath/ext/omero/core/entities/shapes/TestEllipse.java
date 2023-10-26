package qupath.ext.omero.core.entities.shapes;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import qupath.lib.roi.EllipseROI;
import qupath.lib.roi.interfaces.ROI;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class TestEllipse {

    @Test
    void Check_Ellipse_Created() {
        Shape ellipse = createEllipse();

        Class<? extends Shape> type = ellipse.getClass();

        Assertions.assertEquals(Ellipse.class, type);
    }

    @Test
    void Check_Ellipse_ROI() {
        Shape ellipse = createEllipse();

        Class<? extends ROI> roiClass = ellipse.createROI().getClass();

        Assertions.assertEquals(EllipseROI.class, roiClass);
    }

    @Test
    void Check_Serialization_And_Deserialization() {
        Shape ellipse = TestShape.createShapeFromJSON(TestShape.createJSONFromPathObject(createEllipse().createAnnotation()));

        Class<? extends Shape> type = ellipse.getClass();

        Assertions.assertEquals(Ellipse.class, type);
    }

    private Shape createEllipse() {
        return TestShape.createShapeFromJSON("""
                {
                    "@id": 713,
                    "text": "Annotation:Stroma:aba712b2-bbc2-4c05-bbba-d9fbab4d454f:NoParent",
                    "StrokeColor": -16776961,
                    "Locked": false,
                    "oldId": "454:713",
                    "@type": "http://www.openmicroscopy.org/Schemas/OME/2016-06#Ellipse"
                }
                """);    // -16776961 is the integer representation of the red color in the BGR format
    }
}
