package qupath.ext.omero.core.entities.shapes;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import qupath.lib.geom.Point2;
import qupath.lib.objects.PathObjects;
import qupath.lib.roi.PolygonROI;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

import java.util.List;

public class TestPolygon {

    @Test
    void Check_Polygon_Created_From_JSON() {
        Shape polygon = createPolygonFromJSON();

        Assertions.assertEquals(Polygon.class, polygon.getClass());
    }

    @Test
    void Check_Polygon_Created_From_Path_Object() {
        Shape polygon = createPolygonFromPathObject();

        Assertions.assertEquals(Polygon.class, polygon.getClass());
    }

    @Test
    void Check_Polygon_Created_From_JSON_And_Path_Object_Are_Equal() {
        Shape polygonFromJSON = createPolygonFromJSON();

        Shape polygonFromPathObject = createPolygonFromPathObject();

        Assertions.assertEquals(polygonFromJSON, polygonFromPathObject);
    }

    @Test
    void Check_ROI() {
        Shape polygon = createPolygonFromJSON();

        Class<? extends ROI> roiClass = polygon.createROI().getClass();

        Assertions.assertEquals(PolygonROI.class, roiClass);
    }

    private Shape createPolygonFromJSON() {
        return TestShape.createShapeFromJSON("""
                {
                    "@id": 713,
                    "text": "Annotation:Stroma:aba712b2-bbc2-4c05-bbba-d9fbab4d454f:NoParent",
                    "StrokeColor": -16776961,
                    "Locked": false,
                    "oldId": "454:713",
                    "@type": "http://www.openmicroscopy.org/Schemas/OME/2016-06#Polygon",
                    "points": "0,0 50,0 0,50"
                }
                """);    // -16776961 is the integer representation of the red color in the BGR format
    }

    private Shape createPolygonFromPathObject() {
        return Shape.createFromPathObject(PathObjects.createAnnotationObject(ROIs.createPolygonROI(
                List.of(
                        new Point2(0, 0),
                        new Point2(50, 0),
                        new Point2(0, 50)
                ), null
        ))).get(0);
    }
}
