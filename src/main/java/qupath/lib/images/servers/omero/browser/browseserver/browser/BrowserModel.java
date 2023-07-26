package qupath.lib.images.servers.omero.browser.browseserver.browser;

import javafx.beans.property.*;
import javafx.collections.*;
import qupath.lib.images.servers.omero.common.api.clients.WebClient;
import qupath.lib.images.servers.omero.common.gui.UiUtilities;
import qupath.lib.images.servers.omero.common.omeroentities.permissions.Group;
import qupath.lib.images.servers.omero.common.omeroentities.permissions.Owner;
import qupath.lib.images.servers.omero.common.omeroentities.repositoryentities.Server;

import java.net.URI;

/**
 * <p>
 *     The model of the browser. It contains properties and list which determine
 *     parts of the UI rendered by the browser.
 * </p>
 * <p>
 *     In effect, this class acts as an intermediate between a browser and a
 *     {@link qupath.lib.images.servers.omero.common.api.clients.WebClient WebClient}.
 *     Properties and lists of a WebClient can be updated from any thread but the browser can
 *     only be accessed from the UI thread, so this class propagates changes made to these elements
 *     from any thread to the UI thread.
 * </p>
 */
public class BrowserModel {
    private final BooleanProperty authenticated = new SimpleBooleanProperty();
    private final StringProperty username = new SimpleStringProperty();
    private final ObservableSet<URI> openedImagesURIs = FXCollections.observableSet();
    private final ObservableSet<URI> openedImagesURIsImmutable = FXCollections.unmodifiableObservableSet(openedImagesURIs);
    private final IntegerProperty numberOfEntitiesLoading = new SimpleIntegerProperty();
    private final BooleanProperty areOrphanedImagesLoading = new SimpleBooleanProperty(false);
    private final IntegerProperty numberOfOrphanedImages = new SimpleIntegerProperty();
    private final IntegerProperty numberOfOrphanedImagesLoaded = new SimpleIntegerProperty(0);
    private final IntegerProperty numberOfThumbnailsLoading = new SimpleIntegerProperty(0);
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
        UiUtilities.listenToPropertyInUIThread(authenticated, client.getAuthenticated());
        UiUtilities.listenToPropertyInUIThread(username, client.getUsername());
        UiUtilities.listenToPropertyInUIThread(numberOfEntitiesLoading, client.getRequestsHandler().getNumberOfEntitiesLoading());
        UiUtilities.listenToPropertyInUIThread(areOrphanedImagesLoading, client.getRequestsHandler().getOrphanedImagesLoading());
        UiUtilities.listenToPropertyInUIThread(numberOfOrphanedImages, client.getRequestsHandler().getNumberOfOrphanedImages());
        UiUtilities.listenToPropertyInUIThread(numberOfOrphanedImagesLoaded, client.getRequestsHandler().getNumberOfOrphanedImagesLoaded());
        UiUtilities.listenToPropertyInUIThread(numberOfThumbnailsLoading, client.getRequestsHandler().getNumberOfThumbnailsLoading());

        UiUtilities.listenToSetInUIThread(openedImagesURIs, client.getOpenedImagesURIs());

        UiUtilities.listenToListInUIThread(owners, client.getServer().getOwners());
        UiUtilities.listenToListInUIThread(groups, client.getServer().getGroups());
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
     * See {@link WebClient#getOpenedImagesURIs()}.
     */
    public ObservableSet<URI> getOpenedImagesURIs() {
        return openedImagesURIsImmutable;
    }

    /**
     * See {@link qupath.lib.images.servers.omero.common.api.requests.RequestsHandler#getNumberOfEntitiesLoading() RequestsHandler.getNumberOfEntitiesLoading()}.
     */
    public ReadOnlyIntegerProperty getNumberOfEntitiesLoading() {
        return numberOfEntitiesLoading;
    }

    /**
     * See {@link qupath.lib.images.servers.omero.common.api.requests.RequestsHandler#getOrphanedImagesLoading() RequestsHandler.getOrphanedImagesLoading()}.
     */
    public ReadOnlyBooleanProperty getOrphanedImagesLoading() {
        return areOrphanedImagesLoading;
    }

    /**
     * See {@link qupath.lib.images.servers.omero.common.api.requests.RequestsHandler#getNumberOfOrphanedImages() RequestsHandler.getNumberOfOrphanedImages()}.
     */
    public ReadOnlyIntegerProperty getNumberOfOrphanedImages() {
        return numberOfOrphanedImages;
    }

    /**
     * See {@link qupath.lib.images.servers.omero.common.api.requests.RequestsHandler#getNumberOfOrphanedImagesLoaded() RequestsHandler.getNumberOfOrphanedImagesLoaded()}.
     */
    public ReadOnlyIntegerProperty getNumberOfOrphanedImagesLoaded() {
        return numberOfOrphanedImagesLoaded;
    }

    /**
     * See {@link qupath.lib.images.servers.omero.common.api.requests.RequestsHandler#getNumberOfThumbnailsLoading() RequestsHandler.getNumberOfThumbnailsLoading()}.
     */
    public ReadOnlyIntegerProperty getNumberOfThumbnailsLoading() {
        return numberOfThumbnailsLoading;
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
