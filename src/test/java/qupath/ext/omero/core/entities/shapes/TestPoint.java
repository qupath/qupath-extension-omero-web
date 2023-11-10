package qupath.ext.omero.core.entities.shapes;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import qupath.lib.objects.PathObjects;
import qupath.lib.roi.PointsROI;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

public class TestPoint {

    @Test
    void Check_Point_Created_From_JSON() {
        Shape point = createPointFromJSON();

        Assertions.assertEquals(Point.class, point.getClass());
    }

    @Test
    void Check_Point_Created_From_Path_Object() {
        Shape point = createPointFromPathObject();

        Assertions.assertEquals(Point.class, point.getClass());
    }

    @Test
    void Check_Point_Created_From_JSON_And_Path_Object_Are_Equal() {
        Shape pointFromJSON = createPointFromJSON();

        Shape pointFromPathObject = createPointFromPathObject();

        Assertions.assertEquals(pointFromJSON, pointFromPathObject);
    }

    @Test
    void Check_ROI() {
        Shape point = createPointFromJSON();

        Class<? extends ROI> roiClass = point.createROI().getClass();

        Assertions.assertEquals(PointsROI.class, roiClass);
    }

    private Shape createPointFromJSON() {
        return TestShape.createShapeFromJSON("""
                {
                    "@id": 713,
                    "text": "Annotation:Stroma:aba712b2-bbc2-4c05-bbba-d9fbab4d454f:NoParent",
                    "StrokeColor": -16776961,
                    "Locked": false,
                    "oldId": "454:713",
                    "@type": "http://www.openmicroscopy.org/Schemas/OME/2016-06#Point",
                    "x": 10,
                    "y": 20
                }
                """);    // -16776961 is the integer representation of the red color in the BGR format
    }

    private Shape createPointFromPathObject() {
        return Shape.createFromPathObject(PathObjects.createAnnotationObject(
                ROIs.createPointsROI(10, 20, null)
        )).get(0);
    }
}
