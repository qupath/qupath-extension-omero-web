package qupath.lib.images.servers.omero.common.omeroentities.repositoryentities;

import javafx.collections.ObservableList;
import qupath.lib.images.servers.omero.common.api.requests.RequestsHandler;
import qupath.lib.images.servers.omero.common.gui.UiUtilities;
import qupath.lib.images.servers.omero.common.omeroentities.permissions.Group;
import qupath.lib.images.servers.omero.common.omeroentities.permissions.Owner;

import java.util.ResourceBundle;

/**
 * An orphaned folder is a container for orphaned images (described in
 * {@link qupath.lib.images.servers.omero.common.omeroentities.repositoryentities.serverentities server_entities}).
 */
public class OrphanedFolder extends RepositoryEntity {
    private static final ResourceBundle resources = UiUtilities.getResources();
    private final RequestsHandler requestsHandler;
    private boolean childrenPopulated = false;
    private int numberOfImages = 0;

    /**
     * Creates a new orphaned folder.
     * This will load the number of orphaned images in the background.
     *
     * @param requestsHandler  the request handler of the browser
     */
    public OrphanedFolder(RequestsHandler requestsHandler) {
        this.requestsHandler = requestsHandler;

        setNumberOfChildren();
    }

    @Override
    public int getNumberOfChildren() {
        return numberOfImages;
    }

    @Override
    public ObservableList<RepositoryEntity> getChildren() {
        if (!childrenPopulated) {
            childrenPopulated = true;

            requestsHandler.populateOrphanedImagesIntoList(children);
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

    private void setNumberOfChildren() {
        requestsHandler.getOrphanedImagesURIs().thenAccept(orphanedImagesURIs ->
                numberOfImages = orphanedImagesURIs.size()
        );
    }
}
