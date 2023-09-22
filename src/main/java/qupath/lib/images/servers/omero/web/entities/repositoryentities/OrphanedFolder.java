package qupath.lib.images.servers.omero.web.entities.repositoryentities;

import javafx.collections.ObservableList;
import qupath.lib.images.servers.omero.web.apis.ApisHandler;
import qupath.lib.images.servers.omero.gui.UiUtilities;
import qupath.lib.images.servers.omero.web.entities.permissions.Group;
import qupath.lib.images.servers.omero.web.entities.permissions.Owner;

import java.util.ResourceBundle;

/**
 * An orphaned folder is a container for orphaned images (which are described in
 * {@link qupath.lib.images.servers.omero.web.entities.repositoryentities.serverentities server entities}).
 */
public class OrphanedFolder extends RepositoryEntity {

    private static final ResourceBundle resources = UiUtilities.getResources();
    private final ApisHandler apisHandler;
    private boolean childrenPopulated = false;
    private int numberOfImages = 0;

    /**
     * Creates a new orphaned folder.
     * This will load the number of orphaned images in the background.
     *
     * @param apisHandler  the request handler of the browser
     */
    public OrphanedFolder(ApisHandler apisHandler) {
        this.apisHandler = apisHandler;

        setNumberOfChildren();
    }

    @Override
    public String toString() {
        return String.format("Orphaned folder containing %d children", numberOfImages);
    }

    @Override
    public int getNumberOfChildren() {
        return numberOfImages;
    }

    @Override
    public ObservableList<RepositoryEntity> getChildren() {
        if (!childrenPopulated) {
            childrenPopulated = true;

            apisHandler.populateOrphanedImagesIntoList(children);
        }
        return childrenImmutable;
    }

    @Override
    public String getName() {
        return resources.getString("Web.Entities.RepositoryEntities.OrphanedFolder.orphanedImages");
    }

    @Override
    public boolean isFilteredByGroupOwnerName(Group groupFilter, Owner ownerFilter, String nameFilter) {
        return true;
    }

    private void setNumberOfChildren() {
        apisHandler.getOrphanedImagesURIs().thenAccept(orphanedImagesURIs ->
                numberOfImages = orphanedImagesURIs.size()
        );
    }
}
