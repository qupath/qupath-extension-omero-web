package qupath.ext.omero.core.entities.permissions;

import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class TestGroup {

    @Test
    void Check_Group_ID() {
        Group group = createGroup();

        int id = group.getId();

        Assertions.assertEquals(20, id);
    }

    @Test
    void Check_Group_Name() {
        Group group = createGroup();

        String name = group.getName();

        Assertions.assertEquals("Some group", name);
    }

    @Test
    void Check_Group_Name_When_Group_Empty() {
        Group group = new Gson().fromJson("{}", Group.class);

        String name = group.getName();

        Assertions.assertEquals("", name);
    }

    @Test
    void Check_Group_URL() {
        Group group = createGroup();

        String url = group.getExperimentersLink();

        Assertions.assertEquals("http://group.com", url);
    }

    @Test
    void Check_Group_Owners() {
        Owner owner = new Owner(
                2,
                "John",
                "",
                "Doe",
                "john@doe.com",
                "IGC",
                "john_doe"
        );
        Group group = createGroup();
        group.setOwners(List.of(owner));

        List<Owner> owners = group.getOwners();

        Assertions.assertArrayEquals(new Owner[] {owner}, owners.toArray());
    }

    private Group createGroup() {
        String json = """
                {
                    "@id": 20,
                    "Name": "Some group",
                    "url:experimenters": "http://group.com"
                }
                """;
        return new Gson().fromJson(json, Group.class);
    }
}
