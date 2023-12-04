package qupath.ext.omero.core;

import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.gui.prefs.PathPrefs;

import java.util.Arrays;
import java.util.Optional;

/**
 * Utility class to handle client information stored in the preferences of the application.
 */
public class ClientsPreferencesManager {

    private static final Logger logger = LoggerFactory.getLogger(ClientsPreferencesManager.class);
    private static final String PREFERENCE_DELIMITER = ",";
    private static final String PROPERTY_DELIMITER = "%";
    private static final StringProperty serverListPreference = PathPrefs.createPersistentPreference(
            "omero_ext.server_list",
            ""
    );
    private static final StringProperty latestServerPreference = PathPrefs.createPersistentPreference(
            "omero_ext.last_server",
            ""
    );
    private static final StringProperty latestUsernamePreference = PathPrefs.createPersistentPreference(
            "omero_ext.last_username",
            ""
    );
    private static final StringProperty msPixelBufferPortPreference = PathPrefs.createPersistentPreference(
            "omero_ext.ms_pixel_buffer_port",
            ""
    );
    private static final StringProperty webJpegQualityPreference = PathPrefs.createPersistentPreference(
            "omero_ext.web_jpeg_quality",
            ""
    );
    private static final ObservableList<String> uris = FXCollections.observableArrayList(
            Arrays.stream(serverListPreference.get().split(PREFERENCE_DELIMITER))
                    .filter(uri -> !uri.isEmpty())
                    .toList()
    );
    private static final ObservableList<String> urisImmutable = FXCollections.unmodifiableObservableList(uris);

    static {
        uris.addListener((ListChangeListener<? super String>) c -> setServerListPreference(String.join(PREFERENCE_DELIMITER, uris)));
    }

    private ClientsPreferencesManager() {
        throw new AssertionError("This class is not instantiable.");
    }

    /**
     * <p>
     *     Returns the list of server URI stored by the preferences.
     *     This list is unmodifiable, use the {@link #addURI(String) addURI}
     *     or the {@link #removeURI(String) removeURI} methods to update its state.
     * </p>
     * <p>This list may be updated from any thread.</p>
     *
     * @return a list of server URI.
     */
    public static ObservableList<String> getURIs() {
        return urisImmutable;
    }

    /**
     * Adds a server URI to the list of server URI stored by the preferences
     * (if it's not already there).
     *
     * @param uri  the URI to add
     */
    public static void addURI(String uri) {
        setLatestServerPreference(uri);

        if (!isUriAlreadyPresent(uri)) {
            updateURIs(uri, true);
        }
    }

    /**
     * Remove a server URI from the list of server URI stored by the preferences
     * (if it exists).
     *
     * @param uri  the URI to remove
     */
    public static void removeURI(String uri) {
        if (latestServerPreference.get().equals(uri)) {
            setLatestServerPreference("");
        }

        updateURIs(uri, false);
    }

    /**
     * Returns the last URI given to {@link #addURI(String) addURI}.
     *
     * @return the URI of the last server
     */
    public static String getLastServerURI() {
        return latestServerPreference.get();
    }

    /**
     * Returns the last username set by {@link #setLastUsername(String) setLastUsername}.
     *
     * @return the last username
     */
    public static String getLastUsername() {
        return latestUsernamePreference.get();
    }

    /**
     * Set the last username value.
     *
     * @param username  the username to set
     */
    public static void setLastUsername(String username) {
        setLatestUsernamePreference(username);
    }

    /**
     * Get the saved port used by the pixel buffer microservice of the OMERO server
     * corresponding to the provided URI.
     *
     * @param serverURI  the URI of the OMERO server to whose port should be retrieved
     * @return the port, or an empty optional if not found
     */
    public static Optional<Integer> getMsPixelBufferPort(String serverURI) {
        return getProperty(msPixelBufferPortPreference, serverURI).map(p -> {
            try {
                return Integer.valueOf(p);
            } catch (NumberFormatException e) {
                logger.error(String.format("Can't convert %s to int", p), e);
                return null;
            }
        });
    }

    /**
     * Set the saved port used by the pixel buffer microservice of the OMERO server
     * corresponding to the provided URI.
     *
     * @param serverURI  the URI of the OMERO server to whose port should be set
     * @param port  the pixel buffer microservice port
     */
    public static void setMsPixelBufferPort(String serverURI, int port) {
        setProperty(msPixelBufferPortPreference, serverURI, String.valueOf(port));
    }

    /**
     * Get the saved JPEG quality used by the pixel web API corresponding to the provided URI.
     *
     * @param serverURI  the URI of the OMERO server to whose JPEG quality should be retrieved
     * @return the JPEG quality, or an empty optional if not found
     */
    public static Optional<Float> getWebJpegQuality(String serverURI) {
        return getProperty(webJpegQualityPreference, serverURI).map(p -> {
            try {
                return Float.valueOf(p);
            } catch (NumberFormatException e) {
                logger.error(String.format("Can't convert %s to float", p), e);
                return null;
            }
        });
    }

    /**
     * Set the saved JPEG quality used by the pixel web API corresponding to the provided URI.
     *
     * @param serverURI  the URI of the OMERO server to whose port should be set
     * @param jpegQuality  the JPEG quality
     */
    public static synchronized void setWebJpegQuality(String serverURI, float jpegQuality) {
        setProperty(webJpegQualityPreference, serverURI, String.valueOf(jpegQuality));
    }

    private static synchronized void setServerListPreference(String serverListPreference) {
        ClientsPreferencesManager.serverListPreference.set(serverListPreference);
    }

    private static synchronized void setLatestServerPreference(String latestServerPreference) {
        ClientsPreferencesManager.latestServerPreference.set(latestServerPreference);
    }

    private static boolean isUriAlreadyPresent(String uri) {
        return uris.contains(uri);
    }

    private static synchronized void setLatestUsernamePreference(String latestUsernamePreference) {
        ClientsPreferencesManager.latestUsernamePreference.set(latestUsernamePreference);
    }

    private static synchronized void updateURIs(String uri, boolean add) {
        if (add) {
            uris.add(uri);
        } else {
            uris.remove(uri);
        }
    }

    private static synchronized Optional<String> getProperty(StringProperty preference, String serverURI) {
        String[] uriProperties = preference.get().split(PREFERENCE_DELIMITER);

        for (String uriProperty: uriProperties) {
            String[] uriPropertySplit = uriProperty.split(PROPERTY_DELIMITER);

            if (uriPropertySplit.length > 1 && uriPropertySplit[0].equals(serverURI)) {
                return Optional.of(uriPropertySplit[1]);
            }
        }

        return Optional.empty();
    }

    public static synchronized void setProperty(StringProperty preference, String serverURI, String property) {
        String[] uriProperties = preference.get().split(PREFERENCE_DELIMITER);

        boolean propertyAdded = false;
        for (int i=0; i<uriProperties.length; ++i) {
            String[] uriPropertySplit = uriProperties[i].split(PROPERTY_DELIMITER);

            if (uriPropertySplit.length > 0 && uriPropertySplit[0].equals(serverURI)) {
                uriProperties[i] = serverURI + PROPERTY_DELIMITER + property;
                propertyAdded = true;
            }
        }

        String newPreference = String.join(PREFERENCE_DELIMITER, uriProperties);

        if (!propertyAdded) {
            newPreference += PREFERENCE_DELIMITER + serverURI + PROPERTY_DELIMITER + property;
        }

        preference.set(newPreference);
    }
}
