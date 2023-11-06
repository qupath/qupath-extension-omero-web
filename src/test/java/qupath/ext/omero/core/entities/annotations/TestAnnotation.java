package qupath.ext.omero.core.entities.annotations;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import qupath.ext.omero.core.entities.annotations.annotationsentities.Experimenter;
import qupath.ext.omero.core.entities.permissions.Owner;

import java.util.List;

public class TestAnnotation {

    @Test
    void Check_Adder() {
        Annotation annotation = createAnnotation();

        String adderFullName = annotation.getAdderFullName();

        Assertions.assertEquals("Alice Bob", adderFullName);
    }

    @Test
    void Check_Owner() {
        Annotation annotation = createAnnotation();

        String ownerFullName = annotation.getOwnerFullName();

        Assertions.assertEquals("John Doe", ownerFullName);
    }

    @Test
    void Check_Adder_After_Updated() {
        Experimenter experimenter = new Gson().fromJson("""
                {
                    "id": 20,
                    "firstName": "Carol",
                    "lastName": "Dan"
                }
                """, Experimenter.class);
        Annotation annotation = createAnnotation();

        annotation.updateAdderAndOwner(List.of(experimenter));

        Assertions.assertEquals("Carol Dan", annotation.getAdderFullName());
    }

    @Test
    void Check_Owner_After_Updated() {
        Experimenter experimenter = new Gson().fromJson("""
                {
                    "id": 10,
                    "firstName": "Erin",
                    "lastName": "Frank"
                }
                """, Experimenter.class);
        Annotation annotation = createAnnotation();

        annotation.updateAdderAndOwner(List.of(experimenter));

        Assertions.assertEquals("Erin Frank", annotation.getOwnerFullName());
    }

    @Test
    void Check_Adder_After_Not_Updated() {
        Experimenter experimenter = new Gson().fromJson("""
                {
                    "id": 99,
                    "firstName": "Carol",
                    "lastName": "Dan"
                }
                """, Experimenter.class);
        Annotation annotation = createAnnotation();

        annotation.updateAdderAndOwner(List.of(experimenter));

        Assertions.assertEquals("Alice Bob", annotation.getAdderFullName());
    }

    @Test
    void Check_Owner_After_Not_Updated() {
        Experimenter experimenter = new Gson().fromJson("""
                {
                    "id": 99,
                    "firstName": "Erin",
                    "lastName": "Frank"
                }
                """, Experimenter.class);
        Annotation annotation = createAnnotation();

        annotation.updateAdderAndOwner(List.of(experimenter));

        Assertions.assertEquals("John Doe", annotation.getOwnerFullName());
    }

    private Annotation createAnnotation() {
        Owner owner = new Owner(
                10,
                "John",
                "",
                "Doe",
                "john.doe@qupath.com",
                "UoE",
                "john_doe"
        );
        Owner adder = new Owner(
                20,
                "Alice",
                "",
                "Bob",
                "alice.bob@qupath.com",
                "UoE",
                "alice_bob"
        );
        String link = String.format("""
                {
                    "owner": %s
                }
                """, new Gson().toJson(adder)
        );
        String json = String.format(
                """
                {
                    "owner": %s,
                    "link": %s
                }
                """, new Gson().toJson(owner), link);
        Gson gson = new GsonBuilder().registerTypeAdapter(Annotation.class, new Annotation.GsonOmeroAnnotationDeserializer()).setLenient().create();
        return gson.fromJson(json, AnnotationImplementation.class);
    }
}

class AnnotationImplementation extends Annotation {}
