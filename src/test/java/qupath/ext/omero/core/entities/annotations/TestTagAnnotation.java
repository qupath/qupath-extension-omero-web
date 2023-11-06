package qupath.ext.omero.core.entities.annotations;

import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

public class TestTagAnnotation {

    @Test
    void Check_Tag_Value() {
        TagAnnotation tagAnnotation = createTagAnnotation("""
                {
                    "textValue": "Some comment"
                }
                """);

        String comment = tagAnnotation.getValue().orElse("");

        Assertions.assertEquals("Some comment", comment);
    }

    @Test
    void Check_Comment_Missing() {
        TagAnnotation tagAnnotation = createTagAnnotation("{}");

        Optional<String> comment = tagAnnotation.getValue();

        Assertions.assertTrue(comment.isEmpty());
    }

    private TagAnnotation createTagAnnotation(String json) {
        return new Gson().fromJson(json, TagAnnotation.class);
    }
}
