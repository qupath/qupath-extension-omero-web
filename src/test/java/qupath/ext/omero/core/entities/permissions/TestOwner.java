package qupath.ext.omero.core.entities.permissions;

import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestOwner {

    @Test
    void Check_Owner_ID() {
        Owner owner = createOwnerFromJSON();

        int id = owner.id();

        Assertions.assertEquals(20, id);
    }

    @Test
    void Check_Owner_FirstName() {
        Owner owner = createOwnerFromJSON();

        String firstName = owner.firstName();

        Assertions.assertEquals("John", firstName);
    }

    @Test
    void Check_Owner_MiddleName() {
        Owner owner = createOwnerFromJSON();

        String middleName = owner.middleName();

        Assertions.assertEquals("H.", middleName);
    }

    @Test
    void Check_Owner_LastName() {
        Owner owner = createOwnerFromJSON();

        String lastName = owner.lastName();

        Assertions.assertEquals("Doe", lastName);
    }

    @Test
    void Check_Owner_Email() {
        Owner owner = createOwnerFromJSON();

        String emailAddress = owner.emailAddress();

        Assertions.assertEquals("john@doe.com", emailAddress);
    }

    @Test
    void Check_Owner_Institution() {
        Owner owner = createOwnerFromJSON();

        String institution = owner.institution();

        Assertions.assertEquals("IGC", institution);
    }

    @Test
    void Check_Owner_Username() {
        Owner owner = createOwnerFromJSON();

        String username = owner.username();

        Assertions.assertEquals("john_doe", username);
    }

    @Test
    void Check_Owner_FullName() {
        Owner owner = createOwnerFromJSON();

        String fullName = owner.getFullName();

        Assertions.assertEquals("John H. Doe", fullName);
    }

    @Test
    void Check_Owner_FullName_Empty() {
        Owner owner = new Gson().fromJson("{}", Owner.class);

        String fullName = owner.getFullName();

        Assertions.assertEquals("", fullName);
    }

    @Test
    void Check_Owner_Created_From_JSON_Is_Same_From_Constructor() {
        Owner ownerJSON = createOwnerFromJSON();

        Owner ownerConstructor = createOwnerFromConstructor();

        Assertions.assertEquals(ownerConstructor, ownerJSON);
    }

    private Owner createOwnerFromJSON() {
        String json = """
                {
                    "@id": 20,
                    "FirstName": "John",
                    "MiddleName": "H.",
                    "LastName": "Doe",
                    "Email": "john@doe.com",
                    "Institution": "IGC",
                    "UserName": "john_doe"
                }
                """;
        return new Gson().fromJson(json, Owner.class);
    }

    private Owner createOwnerFromConstructor() {
        return new Owner(
                20,
                "John",
                "H.",
                "Doe",
                "john@doe.com",
                "IGC",
                "john_doe"
        );
    }
}
