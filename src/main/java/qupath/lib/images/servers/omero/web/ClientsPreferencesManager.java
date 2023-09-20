package qupath.lib.images.servers.omero.web;

import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import qupath.lib.gui.prefs.PathPrefs;

import java.util.Arrays;

/**
 * <p>Utility class to handle client information stored in the preferences of the application.</p>
 * <p>These preferences can store a list of server URI, the last server URI, and the last username given.</p>
 */
public class ClientsPreferencesManager {

    private static final StringProperty serverListPreference = PathPrefs.createPersistentPreference("omero_ext.server_list", "");
    private static final StringProperty latestServerPreference = PathPrefs.createPersistentPreference("omero_ext.last_server", "");
    private static final StringProperty latestUsernamePreference = PathPrefs.createPersistentPreference("omero_ext.last_username", "");
    private static final ObservableList<String> uris = FXCollections.observableArrayList(
            Arrays.stream(serverListPreference.get().split(","))
                    .filter(uri -> !uri.isEmpty())
                    .toList()
    );
    private static final ObservableList<String> urisImmutable = FXCollections.unmodifiableObservableList(uris);

    static {
        uris.addListener((ListChangeListener<? super String>) c -> setServerListPreference(String.join(",", uris)));
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
}
