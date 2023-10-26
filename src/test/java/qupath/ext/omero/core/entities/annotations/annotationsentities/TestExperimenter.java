package qupath.ext.omero.core.entities.annotations.annotationsentities;

import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class TestExperimenter {

    @Test
    void Check_Experimenter_Id() {
        Experimenter experimenter = createExperimenter();

        int id = experimenter.getId();

        Assertions.assertEquals(54, id);
    }

    @Test
    void Check_Experimenter_FirstName() {
        Experimenter experimenter = createExperimenter();

        String firstName = experimenter.getFirstName();

        Assertions.assertEquals("John", firstName);
    }

    @Test
    void Check_Experimenter_LastName() {
        Experimenter experimenter = createExperimenter();

        String lastName = experimenter.getLastName();

        Assertions.assertEquals("Doe", lastName);
    }

    @Test
    void Check_Experimenter_FullName() {
        Experimenter experimenter = createExperimenter();

        String fullName = experimenter.getFullName();

        Assertions.assertEquals("John Doe", fullName);
    }

    @Test
    void Check_Experimenter_Missing() {
        Experimenter experimenter = new Gson().fromJson("{}", Experimenter.class);

        String fullName = experimenter.getFullName();

        Assertions.assertEquals("", fullName);
    }

    private Experimenter createExperimenter() {
        String json = """
                {
                    "id": 54,
                    "firstName": "John",
                    "lastName": "Doe"
                }
                """;

        return new Gson().fromJson(json, Experimenter.class);
    }
}
