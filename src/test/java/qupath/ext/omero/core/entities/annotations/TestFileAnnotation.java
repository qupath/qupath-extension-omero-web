package qupath.ext.omero.core.entities.annotations;

import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

public class TestFileAnnotation {

    @Test
    void Check_File_Name() {
        FileAnnotation fileAnnotation = createFileAnnotation();

        String fileName = fileAnnotation.getFilename().orElse("");

        Assertions.assertEquals("file.csv", fileName);
    }

    @Test
    void Check_File_Mimetype() {
        FileAnnotation fileAnnotation = createFileAnnotation();

        String mimetype = fileAnnotation.getMimeType().orElse("");

        Assertions.assertEquals("text/csv", mimetype);
    }

    @Test
    void Check_File_Size() {
        FileAnnotation fileAnnotation = createFileAnnotation();

        long fileSize = fileAnnotation.getFileSize().orElse(0L);

        Assertions.assertEquals(5000, fileSize);
    }

    @Test
    void Check_File_Missing() {
        FileAnnotation fileAnnotation = new Gson().fromJson("{}", FileAnnotation.class);

        Optional<String> fileName = fileAnnotation.getFilename();

        Assertions.assertTrue(fileName.isEmpty());
    }

    private FileAnnotation createFileAnnotation() {
        Map<String, String> expectedFile = Map.of(
                "name", "file.csv",
                "mimetype", "text/csv",
                "size", "5000"
        );
        String json = String.format("""
                {
                    "file": %s
                }
                """, new Gson().toJson(expectedFile));
        return new Gson().fromJson(json, FileAnnotation.class);
    }
}
