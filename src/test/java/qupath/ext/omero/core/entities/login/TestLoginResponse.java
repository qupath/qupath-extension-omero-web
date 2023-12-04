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
    void Check_Blank_Response() {
        LoginResponse loginResponse = LoginResponse.createSuccessfulLoginResponse("");

        LoginResponse.Status status = loginResponse.getStatus();

        Assertions.assertEquals(LoginResponse.Status.FAILED, status);
    }

    @Test
    void Check_Empty_Response() {
        LoginResponse loginResponse = LoginResponse.createSuccessfulLoginResponse("{}");

        LoginResponse.Status status = loginResponse.getStatus();

        Assertions.assertEquals(LoginResponse.Status.FAILED, status);
    }

    @Test
    void Check_Successful_Response() {
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
    void Check_Session_UUID() {
        LoginResponse loginResponse = getSuccessfulLoginResponse();

        String sessionUuid = loginResponse.getSessionUuid();

        Assertions.assertEquals("86cdf82c-8df9-11ee-b9d1-0242ac120002", sessionUuid);
    }

    @Test
    void Check_Group_ID() {
        LoginResponse loginResponse = getSuccessfulLoginResponse();

        int groupID = loginResponse.getGroup().getId();

        Assertions.assertEquals(54, groupID);
    }

    private LoginResponse getSuccessfulLoginResponse() {
        return LoginResponse.createSuccessfulLoginResponse(
                """
                {
                    "eventContext": {
                        "userId": 15,
                        "userName": "username",
                        "@id": 54,
                        "sessionUuid": "86cdf82c-8df9-11ee-b9d1-0242ac120002"
                    }
                }
                """
        );
    }
}
