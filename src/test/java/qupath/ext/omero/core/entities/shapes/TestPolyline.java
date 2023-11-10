package qupath.ext.omero.core.entities.shapes;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import qupath.lib.geom.Point2;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.PathObjects;
import qupath.lib.roi.ROIs;

import java.util.List;

public class TestPolyline {

    @Test
    void Check_Polyline_Created_From_JSON() {
        Shape polyline = createPolylineFromJSON();

        Assertions.assertEquals(Polyline.class, polyline.getClass());
    }

    @Test
    void Check_Label_ROI() {
        Shape polyline = createPolylineFromPathObject();

        Assertions.assertEquals(Polyline.class, polyline.getClass());
    }

    @Test
    void Check_Polyline_Created_From_JSON_And_Path_Object_Are_Equal() {
        Shape polylineFromJSON = createPolylineFromJSON();

        Shape polylineFromPathObject = createPolylineFromPathObject();

        Assertions.assertEquals(polylineFromJSON, polylineFromPathObject);
    }

    @Test
    void Check_Polyline_Created_From_Path_Object() {
        PathObject pathObject = PathObjects.createAnnotationObject(ROIs.createPolylineROI(10, 10, null));

        List<Shape> shapes = Shape.createFromPathObject(pathObject);

        Assertions.assertTrue(shapes.get(0) instanceof Polyline);
    }

    private Shape createPolylineFromJSON() {
        return TestShape.createShapeFromJSON("""
                {
                    "@id": 713,
                    "text": "Annotation:Stroma:aba712b2-bbc2-4c05-bbba-d9fbab4d454f:NoParent",
                    "StrokeColor": -16776961,
                    "Locked": false,
                    "oldId": "454:713",
                    "@type": "http://www.openmicroscopy.org/Schemas/OME/2016-06#Polyline",
                    "points": "0,0 50,0 0,50"
                }
                """);    // -16776961 is the integer representation of the red color in the BGR format
    }

    private Shape createPolylineFromPathObject() {
        return Shape.createFromPathObject(PathObjects.createAnnotationObject(ROIs.createPolylineROI(List.of(
                new Point2(0, 0),
                new Point2(50, 0),
                new Point2(0, 50)
        ), null))).get(0);
    }
}
