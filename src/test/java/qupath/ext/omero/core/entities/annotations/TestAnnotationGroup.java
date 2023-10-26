package qupath.ext.omero.core.entities.annotations;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class TestAnnotationGroup {

    @Test
    void Check_Annotations_Not_Created_Because_Class_Missing() {
        AnnotationGroup annotationGroup = createAnnotationGroup("{}, {}");

        Map<Class<? extends Annotation>, List<Annotation>> annotations = annotationGroup.getAnnotations();

        Assertions.assertEquals(0, annotations.values().stream().mapToInt(List::size).sum());
    }

    @Test
    void Check_2_Annotations_Created() {
        AnnotationGroup annotationGroup = createAnnotationGroup("""
                {
                    "class": "comment"
                },
                {
                    "class": "file"
                }
                """);

        Map<Class<? extends Annotation>, List<Annotation>> annotations = annotationGroup.getAnnotations();

        Assertions.assertEquals(2, annotations.values().stream().mapToInt(List::size).sum());
    }

    @Test
    void Check_2_Comment_Annotations_Created() {
        AnnotationGroup annotationGroup = createAnnotationGroup("""
                {
                    "class": "comment"
                },
                {
                    "class": "comment"
                }
                """);

        Map<Class<? extends Annotation>, List<Annotation>> annotations = annotationGroup.getAnnotations();

        Assertions.assertEquals(2, annotations.get(CommentAnnotation.class).size());
    }

    private AnnotationGroup createAnnotationGroup(String annotations) {
        String json = String.format("""
                {
                    "annotations": [%s],
                    "experimenters": []
                }
                """, annotations);
        return new AnnotationGroup(JsonParser.parseString(json).getAsJsonObject());
    }
}
