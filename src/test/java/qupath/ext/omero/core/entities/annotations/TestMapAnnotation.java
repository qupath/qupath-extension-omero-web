package qupath.ext.omero.core.entities.annotations;

import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class TestMapAnnotation {

    @Test
    void Check_Values() {
        MapAnnotation mapAnnotation = createMapAnnotation();

        Map<String, String> map = mapAnnotation.getValues();

        Assertions.assertEquals("value1", map.get("key1"));
    }

    @Test
    void Check_Values_Missing() {
        MapAnnotation mapAnnotation = new Gson().fromJson("{}", MapAnnotation.class);

        Map<String, String> map = mapAnnotation.getValues();

        Assertions.assertTrue(map.isEmpty());
    }

    private MapAnnotation createMapAnnotation() {
        Map<String, String> expectedFile = Map.of(
                "key1", "value1"
        );
        String json = String.format("""
                {
                    "values": %s
                }
                """, new Gson().toJson(expectedFile));
        return new Gson().fromJson(json, MapAnnotation.class);
    }
}
