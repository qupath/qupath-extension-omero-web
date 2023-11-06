package qupath.ext.omero.core.entities.login;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestLoginResponse {

    @Test
    void Check_Failed_Response() {
        LoginResponse loginResponse = LoginResponse.createNonSuccessfulLoginResponse(LoginResponse.Status.CANCELED);

        LoginResponse.Status status = loginResponse.getStatus();

        Assertions.assertEquals(LoginResponse.Status.CANCELED, status);
    }

    @Test
    void Check_Blank_LoginResponse() {
        LoginResponse loginResponse = LoginResponse.createSuccessLoginResponse(
                "",
                new char[]{}
        );

        LoginResponse.Status status = loginResponse.getStatus();

        Assertions.assertEquals(LoginResponse.Status.FAILED, status);
    }

    @Test
    void Check_Empty_LoginResponse() {
        LoginResponse loginResponse = LoginResponse.createSuccessLoginResponse(
                "{}",
                new char[]{}
        );

        LoginResponse.Status status = loginResponse.getStatus();

        Assertions.assertEquals(LoginResponse.Status.FAILED, status);
    }

    @Test
    void Check_Successful_LoginResponse() {
        LoginResponse loginResponse = getSuccessfulLoginResponse();

        LoginResponse.Status status = loginResponse.getStatus();

        Assertions.assertEquals(LoginResponse.Status.SUCCESS, status);
    }

    @Test
    void Check_User_ID() {
        LoginResponse loginResponse = getSuccessfulLoginResponse();

        int userId = loginResponse.getUserId();

        Assertions.assertEquals(15, userId);
    }

    @Test
    void Check_Username() {
        LoginResponse loginResponse = getSuccessfulLoginResponse();

        String username = loginResponse.getUsername();

        Assertions.assertEquals("username", username);
    }

    @Test
    void Check_Password() {
        LoginResponse loginResponse = getSuccessfulLoginResponse();

        char[] password = loginResponse.getPassword();

        Assertions.assertArrayEquals("password".toCharArray(), password);
    }

    @Test
    void Check_Group_ID() {
        LoginResponse loginResponse = getSuccessfulLoginResponse();

        int groupID = loginResponse.getGroup().getId();

        Assertions.assertEquals(54, groupID);
    }

    private LoginResponse getSuccessfulLoginResponse() {
        return LoginResponse.createSuccessLoginResponse(
                """
                {
                    "eventContext": {
                        "userId": 15,
                        "userName": "username",
                        "@id": 54
                    }
                }
                """,
                "password".toCharArray()
        );
    }
}
