package qupath.lib.images.servers.omero.gui.browser.serverbrowser;

import javafx.beans.property.*;
import javafx.collections.*;
import qupath.lib.images.servers.omero.web.WebClient;
import qupath.lib.images.servers.omero.web.apis.ApisHandler;
import qupath.lib.images.servers.omero.gui.UiUtilities;
import qupath.lib.images.servers.omero.web.entities.permissions.Group;
import qupath.lib.images.servers.omero.web.entities.permissions.Owner;
import qupath.lib.images.servers.omero.web.entities.repositoryentities.Server;

import java.net.URI;

/**
 * <p>
 *     The model of the browser. It contains properties and list which determine
 *     parts of the UI rendered by the browser.
 * </p>
 * <p>
 *     In effect, this class acts as an intermediate between a browser and a
 *     {@link WebClient WebClient}.
 *     Properties and lists of a WebClient can be updated from any thread but the browser can
 *     only be accessed from the UI thread, so this class propagates changes made to these elements
 *     from any thread to the UI thread.
 * </p>
 */
public class BrowserModel {

    private final BooleanProperty authenticated = new SimpleBooleanProperty();
    private final StringProperty username = new SimpleStringProperty();
    private final IntegerProperty numberOfEntitiesLoading = new SimpleIntegerProperty();
    private final BooleanProperty areOrphanedImagesLoading = new SimpleBooleanProperty(false);
    private final IntegerProperty numberOfOrphanedImages = new SimpleIntegerProperty();
    private final IntegerProperty numberOfOrphanedImagesLoaded = new SimpleIntegerProperty(0);
    private final IntegerProperty numberOfThumbnailsLoading = new SimpleIntegerProperty(0);
    private final ObjectProperty<Owner> defaultUser = new SimpleObjectProperty<>();
    private final ObjectProperty<Group> defaultGroup = new SimpleObjectProperty<>();
    private final ObservableSet<URI> openedImagesURIs = FXCollections.observableSet();
    private final ObservableSet<URI> openedImagesURIsImmutable = FXCollections.unmodifiableObservableSet(openedImagesURIs);
    private final ObservableList<Owner> owners = FXCollections.observableArrayList();
    private final ObservableList<Owner> ownersImmutable = FXCollections.unmodifiableObservableList(owners);
    private final ObservableList<Group> groups = FXCollections.observableArrayList();
    private final ObservableList<Group> groupsImmutable = FXCollections.unmodifiableObservableList(groups);

    /**
     * Creates a new browser model
     *
     * @param client  the client whose properties and lists should be listened
     */
    public BrowserModel(WebClient client) {
        UiUtilities.bindPropertyInUIThread(authenticated, client.getAuthenticated());
        UiUtilities.bindPropertyInUIThread(username, client.getUsername());
        UiUtilities.bindPropertyInUIThread(numberOfEntitiesLoading, client.getApisHandler().getNumberOfEntitiesLoading());
        UiUtilities.bindPropertyInUIThread(areOrphanedImagesLoading, client.getApisHandler().getOrphanedImagesLoading());
        UiUtilities.bindPropertyInUIThread(numberOfOrphanedImages, client.getApisHandler().getNumberOfOrphanedImages());
        UiUtilities.bindPropertyInUIThread(numberOfOrphanedImagesLoaded, client.getApisHandler().getNumberOfOrphanedImagesLoaded());
        UiUtilities.bindPropertyInUIThread(numberOfThumbnailsLoading, client.getApisHandler().getNumberOfThumbnailsLoading());
        UiUtilities.bindPropertyInUIThread(defaultUser, client.getServer().getDefaultUser());
        UiUtilities.bindPropertyInUIThread(defaultGroup, client.getServer().getDefaultGroup());

        UiUtilities.bindSetInUIThread(openedImagesURIs, client.getOpenedImagesURIs());

        UiUtilities.bindListInUIThread(owners, client.getServer().getOwners());
        UiUtilities.bindListInUIThread(groups, client.getServer().getGroups());
    }

    /**
     * See {@link WebClient#getAuthenticated()}.
     */
    public ReadOnlyBooleanProperty getAuthenticated() {
        return authenticated;
    }

    /**
     * See {@link WebClient#getUsername()}.
     */
    public ReadOnlyStringProperty getUsername() {
        return username;
    }

    /**
     * See {@link ApisHandler#getNumberOfEntitiesLoading() RequestsHandler.getNumberOfEntitiesLoading()}.
     */
    public ReadOnlyIntegerProperty getNumberOfEntitiesLoading() {
        return numberOfEntitiesLoading;
    }

    /**
     * See {@link ApisHandler#getOrphanedImagesLoading() RequestsHandler.getOrphanedImagesLoading()}.
     */
    public ReadOnlyBooleanProperty getOrphanedImagesLoading() {
        return areOrphanedImagesLoading;
    }

    /**
     * See {@link ApisHandler#getNumberOfOrphanedImages() RequestsHandler.getNumberOfOrphanedImages()}.
     */
    public ReadOnlyIntegerProperty getNumberOfOrphanedImages() {
        return numberOfOrphanedImages;
    }

    /**
     * See {@link ApisHandler#getNumberOfOrphanedImagesLoaded() RequestsHandler.getNumberOfOrphanedImagesLoaded()}.
     */
    public ReadOnlyIntegerProperty getNumberOfOrphanedImagesLoaded() {
        return numberOfOrphanedImagesLoaded;
    }

    /**
     * See {@link ApisHandler#getNumberOfThumbnailsLoading() RequestsHandler.getNumberOfThumbnailsLoading()}.
     */
    public ReadOnlyIntegerProperty getNumberOfThumbnailsLoading() {
        return numberOfThumbnailsLoading;
    }

    /**
     * See {@link Server#getDefaultUser()}.
     */
    public ReadOnlyObjectProperty<Owner> getDefaultUser() {
        return defaultUser;
    }

    /**
     * See {@link Server#getDefaultGroup()}.
     */
    public ReadOnlyObjectProperty<Group> getDefaultGroup() {
        return defaultGroup;
    }

    /**
     * See {@link WebClient#getOpenedImagesURIs()}.
     */
    public ObservableSet<URI> getOpenedImagesURIs() {
        return openedImagesURIsImmutable;
    }

    /**
     * See {@link Server#getOwners()}}.
     */
    public ObservableList<Owner> getOwners() {
        return ownersImmutable;
    }

    /**
     * See {@link Server#getGroups()}}.
     */
    public ObservableList<Group> getGroups() {
        return groupsImmutable;
    }
}
