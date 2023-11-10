package qupath.ext.omero.core.entities.shapes;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import qupath.lib.objects.PathObjects;
import qupath.lib.roi.EllipseROI;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

public class TestEllipse {

    @Test
    void Check_Ellipse_Created_From_JSON() {
        Shape ellipse = createEllipseFromJSON();

        Assertions.assertEquals(Ellipse.class, ellipse.getClass());
    }

    @Test
    void Check_Ellipse_Created_From_Path_Object() {
        Shape ellipse = createEllipseFromPathObject();

        Assertions.assertEquals(Ellipse.class, ellipse.getClass());
    }

    @Test
    void Check_Ellipse_Created_From_JSON_And_Path_Object_Are_Equal() {
        Shape ellipseFromJSON = createEllipseFromJSON();

        Shape ellipseFromPathObject = createEllipseFromPathObject();

        Assertions.assertEquals(ellipseFromJSON, ellipseFromPathObject);
    }

    @Test
    void Check_ROI() {
        Shape ellipse = createEllipseFromJSON();

        Class<? extends ROI> roiClass = ellipse.createROI().getClass();

        Assertions.assertEquals(EllipseROI.class, roiClass);
    }

    private Shape createEllipseFromJSON() {
        return TestShape.createShapeFromJSON("""
                {
                    "@id": 713,
                    "text": "Annotation:Stroma:aba712b2-bbc2-4c05-bbba-d9fbab4d454f:NoParent",
                    "StrokeColor": -16776961,
                    "Locked": false,
                    "oldId": "454:713",
                    "@type": "http://www.openmicroscopy.org/Schemas/OME/2016-06#Ellipse",
                    "x": 50,
                    "y": 100,
                    "radiusX": 10,
                    "radiusY": 20
                }
                """);    // -16776961 is the integer representation of the red color in the BGR format
    }

    private Shape createEllipseFromPathObject() {
        return Shape.createFromPathObject(PathObjects.createAnnotationObject(
                ROIs.createEllipseROI(40, 80, 20, 40, null)
        )).get(0);
    }
}
