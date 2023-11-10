package qupath.ext.omero.core.entities.shapes;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import qupath.lib.objects.PathObjects;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.RectangleROI;
import qupath.lib.roi.interfaces.ROI;

public class TestRectangle {

    @Test
    void Check_Rectangle_Created_From_JSON() {
        Shape rectangle = createRectangleFromJSON();

        Assertions.assertEquals(Rectangle.class, rectangle.getClass());
    }

    @Test
    void Check_Rectangle_Created_From_Path_Object() {
        Shape rectangle = createRectangleFromPathObject();

        Assertions.assertEquals(Rectangle.class, rectangle.getClass());
    }

    @Test
    void Check_Rectangle_Created_From_JSON_And_Path_Object_Are_Equal() {
        Shape rectangleFromJSON = createRectangleFromJSON();

        Shape rectangleFromPathObject = createRectangleFromPathObject();

        Assertions.assertEquals(rectangleFromJSON, rectangleFromPathObject);
    }

    @Test
    void Check_ROI() {
        Shape rectangle = createRectangleFromJSON();

        Class<? extends ROI> roiClass = rectangle.createROI().getClass();

        Assertions.assertEquals(RectangleROI.class, roiClass);
    }

    private Shape createRectangleFromJSON() {
        return TestShape.createShapeFromJSON("""
                {
                    "@id": 713,
                    "text": "Annotation:Stroma:aba712b2-bbc2-4c05-bbba-d9fbab4d454f:NoParent",
                    "StrokeColor": -16776961,
                    "Locked": false,
                    "oldId": "454:713",
                    "@type": "http://www.openmicroscopy.org/Schemas/OME/2016-06#Rectangle",
                    "x": 50,
                    "y": 100,
                    "width": 10,
                    "height": 20
                }
                """);    // -16776961 is the integer representation of the red color in the BGR format
    }

    private Shape createRectangleFromPathObject() {
        return Shape.createFromPathObject(PathObjects.createAnnotationObject(
                ROIs.createRectangleROI(50, 100, 10, 20, null)
        )).get(0);
    }
}
