package qupath.ext.omero.core.entities.login;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.omero.core.entities.permissions.Group;

/**
 * <p>Reads the response from a login request.</p>
 * <p>
 *     When accessing data of an instance of this class, remember to first
 *     check that the login operation was successful with {@link #getStatus()}
 *     (otherwise some data might be null or incorrect).
 * </p>
 */
public class LoginResponse {

    private static final Logger logger = LoggerFactory.getLogger(LoginResponse.class);
    private final Status status;
    private Group group;
    private int userId;
    private String username;
    private String sessionUuid;
    public enum Status {
        CANCELED,
        FAILED,
        UNAUTHENTICATED,
        SUCCESS
    }

    private LoginResponse(Status reason) {
        this.status = reason;
    }

    private LoginResponse(Group group, int userId, String username, String sessionUuid) {
        this(Status.SUCCESS);
        this.group = group;
        this.userId = userId;
        this.username = username;
        this.sessionUuid = sessionUuid;
    }

    @Override
    public String toString() {
        return String.format("LoginResponse of status %s for %s of ID %d", status, username, userId);
    }

    /**
     * Create a new login response with an unauthenticated, failed, or canceled status.
     * It is not possible to call this function with a successful login.
     *
     * @param status  the status of the login
     * @return a response with the given status
     * @throws IllegalArgumentException when {@code status} is {@code Status.SUCCESS}
     */
    public static LoginResponse createNonSuccessfulLoginResponse(Status status) {
        if (status.equals(Status.SUCCESS)) {
            throw new IllegalArgumentException("You cannot create a non successful login response with a success status");
        }
        return new LoginResponse(status);
    }

    /**
     * Parse and read a server response.
     *
     * @param serverResponse  the raw server response of the login request
     * @return a successful LoginResponse if all necessary information was correctly
     * parsed, or a response with a failed status
     */
    public static LoginResponse createSuccessfulLoginResponse(String serverResponse) {
        try {
            JsonElement element = JsonParser.parseString(serverResponse).getAsJsonObject().get("eventContext");

            return new LoginResponse(
                    new Gson().fromJson(element, Group.class),
                    element.getAsJsonObject().get("userId").getAsInt(),
                    element.getAsJsonObject().get("userName").getAsString(),
                    element.getAsJsonObject().get("sessionUuid").getAsString()
            );
        } catch (Exception e) {
            logger.error("Error when reading login response", e);
            return new LoginResponse(Status.FAILED);
        }
    }

    /**
     * @return whether the login attempt was successful
     */
    public Status getStatus() {
        return status;
    }

    /**
     * @return the user ID of the authenticated user, or 0 if the login
     * attempt failed
     */
    public int getUserId() {
        return userId;
    }

    /**
     * @return the username of the authenticated user, or null if the login
     * attempt failed
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return the session UUID of the authenticated user, or null if the login
     * attempt failed
     */
    public String getSessionUuid() {
        return sessionUuid;
    }

    /**
     * @return the group of the authenticated user, or null if the login
     * attempt failed
     */
    public Group getGroup() {
        return group;
    }
}
