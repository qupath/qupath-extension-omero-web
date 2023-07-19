package qupath.lib.images.servers.omero.common.api.requests.entities.login;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.images.servers.omero.common.api.requests.RequestsHandler;
import qupath.lib.images.servers.omero.common.omeroentities.permissions.Group;
import qupath.lib.io.GsonTools;

/**
 * <p>Reads the response from a login request.</p>
 * <p>
 *     When accessing data of an instance of this class, remember to first
 *     check that the login operation was successful with {@link #isLoginSuccessful()}
 *     (otherwise some data might be null or incorrect).
 * </p>
 */
public class LoginResponse {
    private final static Logger logger = LoggerFactory.getLogger(RequestsHandler.class);
    private final boolean success;
    private Group group;
    private int userId;
    private String sessionUUID;
    private String username;

    private LoginResponse(boolean success) {
        this.success = success;
    }

    private LoginResponse(Group group, int userId, String sessionUUID, String username) {
        this(true);
        this.group = group;
        this.userId = userId;
        this.sessionUUID = sessionUUID;
        this.username = username;
    }

    /**
     * @return a response with a failed status
     */
    public static LoginResponse createFailedLoginResponse() {
        return new LoginResponse(false);
    }

    /**
     * Parse and reads a server response.
     *
     * @param serverResponse  the raw server response of the login request
     * @return a successful LoginResponse if all necessary information was correctly
     * parsed, or a response with a failed status
     */
    public static LoginResponse createLoginResponse(String serverResponse) {
        try {
            JsonElement element = JsonParser.parseString(serverResponse);

            return new LoginResponse(
                    GsonTools.getInstance().fromJson(element.getAsJsonObject().get("eventContext"), Group.class),
                    element.getAsJsonObject().get("eventContext").getAsJsonObject().get("userId").getAsInt(),
                    element.getAsJsonObject().get("eventContext").getAsJsonObject().get("sessionUuid").getAsString(),
                    element.getAsJsonObject().get("eventContext").getAsJsonObject().get("userName").getAsString()
            );
        } catch (Exception e) {
            logger.error("Error when reading login response", e);
            return new LoginResponse(false);
        }
    }

    /**
     * @return whether the login attempt was successful
     */
    public boolean isLoginSuccessful() {
        return success;
    }

    /**
     * @return the group of the authenticated user, or null if the login
     * attempt failed
     */
    public Group getGroup() {
        return group;
    }

    /**
     * @return the user ID of the authenticated user, or 0 if the login
     * attempt failed
     */
    public int getUserId() {
        return userId;
    }

    /**
     * @return the session UUID of the authenticated user, or null if the login
     * attempt failed
     */
    public String getSessionUUID() {
        return sessionUUID;
    }

    /**
     * @return the username of the authenticated user, or null if the login
     * attempt failed
     */
    public String getUsername() {
        return username;
    }
}
