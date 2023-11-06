package qupath.ext.omero.core.entities.serverinformation;

import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

public class TestOmeroAPI {

    @Test
    void Check_Empty() {
        OmeroAPI omeroAPI = new Gson().fromJson("{}", OmeroAPI.class);

        Optional<String> latestVersionURL = omeroAPI.getLatestVersionURL();

        Assertions.assertTrue(latestVersionURL.isEmpty());
    }

    @Test
    void Check_Latest_Version_URL() {
        OmeroAPI omeroAPI = createOmeroAPI();

        String latestVersionURL = omeroAPI.getLatestVersionURL().orElse("");

        Assertions.assertEquals("https://idr.openmicroscopy.org/api/v1/", latestVersionURL);
    }

    private OmeroAPI createOmeroAPI() {
        String json = """
                {
                    "data": [
                        {
                            "url:base": "https://idr.openmicroscopy.org/api/v0/"
                        },
                        {
                            "url:base": "https://idr.openmicroscopy.org/api/v1/"
                        }
                    ]
                }
                """;
        return new Gson().fromJson(json, OmeroAPI.class);
    }
}
