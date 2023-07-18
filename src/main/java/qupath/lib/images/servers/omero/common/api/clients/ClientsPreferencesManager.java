package qupath.lib.images.servers.omero.common.api.clients;

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
    private final static StringProperty serverListPreference = PathPrefs.createPersistentPreference("omero_ext.server_list", "");
    private final static StringProperty latestServerPreference = PathPrefs.createPersistentPreference("omero_ext.last_server", "");
    private final static StringProperty latestUsernamePreference = PathPrefs.createPersistentPreference("omero_ext.last_username", "");
    private final static ObservableList<String> uris = FXCollections.observableArrayList(
            Arrays.stream(serverListPreference.get().split(","))
                    .filter(uri -> !uri.isEmpty())
                    .toList()
    );
    private final static ObservableList<String> urisImmutable = FXCollections.unmodifiableObservableList(uris);

    static {
        uris.addListener((ListChangeListener<? super String>) c -> serverListPreference.set(String.join(",", uris)));
    }

    private ClientsPreferencesManager() {
        throw new RuntimeException("This class is not instantiable.");
    }

    /**
     * Returns the list of server URI stored by the preferences.
     * This list is unmodifiable, use the {@link #addURI(String) addURI}
     * or the {@link #removeURI(String) removeURI} methods to update its state.
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
        latestServerPreference.set(uri);

        if (!isUriAlreadyPresent(uri)) {
            uris.add(uri);
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
            latestServerPreference.set("");
        }

        uris.remove(uri);
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
        latestUsernamePreference.set(username);
    }

    private static boolean isUriAlreadyPresent(String uri) {
        return getURIs().contains(uri);
    }
}
