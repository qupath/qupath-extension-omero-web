package qupath.ext.omero.core.entities.shapes;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import qupath.lib.objects.PathObjects;
import qupath.lib.roi.LineROI;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

public class TestLine {

    @Test
    void Check_Line_Created_From_JSON() {
        Shape line = createLineFromJSON();

        Assertions.assertEquals(Line.class, line.getClass());
    }

    @Test
    void Check_Line_Created_From_Path_Object() {
        Shape line = createLineFromPathObject();

        Assertions.assertEquals(Line.class, line.getClass());
    }

    @Test
    void Check_Line_Created_From_JSON_And_Path_Object_Are_Equal() {
        Shape lineFromJSON = createLineFromJSON();

        Shape lineFromPathObject = createLineFromPathObject();

        Assertions.assertEquals(lineFromJSON, lineFromPathObject);
    }

    @Test
    void Check_ROI() {
        Shape line = createLineFromJSON();

        Class<? extends ROI> roiClass = line.createROI().getClass();

        Assertions.assertEquals(LineROI.class, roiClass);
    }

    private Shape createLineFromJSON() {
        return TestShape.createShapeFromJSON("""
                {
                    "@id": 713,
                    "text": "Annotation:Stroma:aba712b2-bbc2-4c05-bbba-d9fbab4d454f:NoParent",
                    "StrokeColor": -16776961,
                    "Locked": false,
                    "oldId": "454:713",
                    "@type": "http://www.openmicroscopy.org/Schemas/OME/2016-06#Line",
                    "x1": 10,
                    "y1": 20,
                    "x2": 50,
                    "y2": 100
                }
                """);    // -16776961 is the integer representation of the red color in the BGR format
    }

    private Shape createLineFromPathObject() {
        return Shape.createFromPathObject(PathObjects.createAnnotationObject(
                ROIs.createLineROI(10, 20, 50, 100, null)
        )).get(0);
    }
}
