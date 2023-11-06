package qupath.ext.omero.gui.browser.serverbrowser;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.*;
import qupath.ext.omero.core.WebClient;
import qupath.ext.omero.core.apis.ApisHandler;
import qupath.ext.omero.gui.UiUtilities;
import qupath.ext.omero.core.entities.permissions.Group;
import qupath.ext.omero.core.entities.permissions.Owner;

import java.net.URI;

/**
 * <p>
 *     The model of the browser. It contains properties and list which determine
 *     parts of the UI rendered by the browser.
 * </p>
 * <p>
 *     Part of the goal of this class is to act as an intermediate between a browser and a
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
    private final ObservableSet<URI> openedImagesURIs = FXCollections.observableSet();
    private final ObservableSet<URI> openedImagesURIsImmutable = FXCollections.unmodifiableObservableSet(openedImagesURIs);
    private final ObjectProperty<Owner> selectedOwner;
    private final ObjectProperty<Group> selectedGroup;

    /**
     * Creates a new browser model
     *
     * @param client  the client whose properties and lists should be listened
     */
    public BrowserModel(WebClient client) {
        UiUtilities.bindPropertyInUIThread(authenticated, client.getAuthenticated());
        UiUtilities.bindPropertyInUIThread(username, client.getUsername());
        UiUtilities.bindPropertyInUIThread(numberOfEntitiesLoading, client.getApisHandler().getNumberOfEntitiesLoading());
        UiUtilities.bindPropertyInUIThread(areOrphanedImagesLoading, client.getApisHandler().areOrphanedImagesLoading());
        UiUtilities.bindPropertyInUIThread(numberOfOrphanedImagesLoaded, client.getApisHandler().getNumberOfOrphanedImagesLoaded());
        UiUtilities.bindPropertyInUIThread(numberOfThumbnailsLoading, client.getApisHandler().getNumberOfThumbnailsLoading());

        UiUtilities.bindSetInUIThread(openedImagesURIs, client.getOpenedImagesURIs());

        if (client.getServer().getDefaultOwner().isPresent()) {
            selectedOwner = new SimpleObjectProperty<>(client.getServer().getDefaultOwner().get());
        } else if (!client.getServer().getOwners().isEmpty()) {
            selectedOwner = new SimpleObjectProperty<>(client.getServer().getOwners().get(0));
        } else {
            selectedOwner = new SimpleObjectProperty<>(null);
        }

        if (client.getServer().getDefaultGroup().isPresent()) {
            selectedGroup = new SimpleObjectProperty<>(client.getServer().getDefaultGroup().get());
        } else if (!client.getServer().getGroups().isEmpty()) {
            selectedGroup = new SimpleObjectProperty<>(client.getServer().getGroups().get(0));
        } else {
            selectedGroup = new SimpleObjectProperty<>(null);
        }

        client.getApisHandler().getNumberOfOrphanedImages().thenAccept(numberOfOrphanedImages -> Platform.runLater(() ->
                this.numberOfOrphanedImages.set(numberOfOrphanedImages))
        );
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
     * See {@link ApisHandler#getNumberOfEntitiesLoading()}.
     */
    public ReadOnlyIntegerProperty getNumberOfEntitiesLoading() {
        return numberOfEntitiesLoading;
    }

    /**
     * See {@link ApisHandler#areOrphanedImagesLoading()}.
     */
    public ReadOnlyBooleanProperty areOrphanedImagesLoading() {
        return areOrphanedImagesLoading;
    }

    /**
     * See {@link ApisHandler#getNumberOfOrphanedImages()}.
     */
    public ReadOnlyIntegerProperty getNumberOfOrphanedImages() {
        return numberOfOrphanedImages;
    }

    /**
     * See {@link ApisHandler#getNumberOfOrphanedImagesLoaded()}.
     */
    public ReadOnlyIntegerProperty getNumberOfOrphanedImagesLoaded() {
        return numberOfOrphanedImagesLoaded;
    }

    /**
     * See {@link ApisHandler#getNumberOfThumbnailsLoading()}.
     */
    public ReadOnlyIntegerProperty getNumberOfThumbnailsLoading() {
        return numberOfThumbnailsLoading;
    }

    /**
     * See {@link WebClient#getOpenedImagesURIs()}.
     */
    public ObservableSet<URI> getOpenedImagesURIs() {
        return openedImagesURIsImmutable;
    }

    /**
     * @return the currently selected owner of the browser
     */
    public ObjectProperty<Owner> getSelectedOwner() {
        return selectedOwner;
    }

    /**
     * @return the currently selected group of the browser
     */
    public ObjectProperty<Group> getSelectedGroup() {
        return selectedGroup;
    }
}
