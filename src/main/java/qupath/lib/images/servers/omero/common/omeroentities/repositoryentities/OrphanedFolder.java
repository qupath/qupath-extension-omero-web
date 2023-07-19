package qupath.lib.images.servers.omero.common.omeroentities.repositoryentities;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import qupath.lib.images.servers.omero.common.api.requests.RequestsHandler;
import qupath.lib.images.servers.omero.common.gui.UiUtilities;
import qupath.lib.images.servers.omero.common.omeroentities.permissions.Group;
import qupath.lib.images.servers.omero.common.omeroentities.permissions.Owner;

import java.util.ResourceBundle;

/**
 * An orphaned folder is a container for orphaned images and orphaned datasets (described in
 * {@link qupath.lib.images.servers.omero.common.omeroentities.repositoryentities.serverentities server_entities}).
 */
public class OrphanedFolder extends RepositoryEntity {
    private static final ResourceBundle resources = UiUtilities.getResources();
    private final RequestsHandler requestsHandler;
    private boolean childrenPopulated = false;
    private int numberOfDatasets = 0;
    private int numberOfImages = 0;

    /**
     * Creates a new orphaned folder.
     * This will load orphaned datasets and the number of orphaned images in the background.
     *
     * @param requestsHandler  the request handler of the browser
     */
    public OrphanedFolder(RequestsHandler requestsHandler) {
        this.requestsHandler = requestsHandler;

        populateDatasets();
        populateNumberOfImages();
    }

    @Override
    public int getNumberOfChildren() {
        return numberOfImages + numberOfDatasets;
    }

    @Override
    public ObservableList<RepositoryEntity> getChildren() {
        if (!childrenPopulated) {
            requestsHandler.populateOrphanedImagesIntoList(this.children);
            childrenPopulated = true;
        }
        return childrenImmutable;
    }

    @Override
    public String getName() {
        return resources.getString("Common.OmeroEntities.OrphanedFolder.orphanedImages");
    }

    @Override
    public boolean isFilteredByGroupOwnerName(Group groupFilter, Owner ownerFilter, String nameFilter) {
        return true;
    }

    private void populateDatasets() {
        requestsHandler.getOrphanedDatasets().thenAccept(children -> Platform.runLater(() -> {
            numberOfDatasets = children.size();
            this.children.addAll(children);
        }));
    }

    private void populateNumberOfImages() {
        requestsHandler.getOrphanedImagesURIs().thenAccept(children -> Platform.runLater(() -> numberOfImages = children.size()));
    }
}
