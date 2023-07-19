package qupath.lib.images.servers.omero.browser.browseserver.browser.hierarchy;

import javafx.beans.binding.Bindings;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import qupath.lib.images.servers.omero.common.api.clients.WebClient;
import qupath.lib.images.servers.omero.common.omeroentities.repositoryentities.*;
import qupath.lib.images.servers.omero.common.omeroentities.repositoryentities.serverentities.Dataset;
import qupath.lib.images.servers.omero.common.omeroentities.repositoryentities.serverentities.Project;
import qupath.lib.images.servers.omero.common.omeroentities.repositoryentities.serverentities.image.Image;
import qupath.lib.images.servers.omero.common.gui.UiUtilities;

import java.util.ResourceBundle;

/**
 * <p>
 *     Cell factory of the hierarchy of a {@link javafx.scene.control.TreeView TreeView} containing
 *     {@link qupath.lib.images.servers.omero.common.omeroentities.repositoryentities.RepositoryEntity RepositoryEntity} elements .</p>
 * <p>
 *     It displays the name of each OMERO entity, a corresponding icon, the number of children (if any),
 *     and additional information with a tooltip.
 * </p>
 * <p>
 *     If the entity is an {@link qupath.lib.images.servers.omero.common.omeroentities.repositoryentities.serverentities.image.Image Image},
 *     a complex tooltip described in {@link qupath.lib.images.servers.omero.browser.browseserver.browser.hierarchy.ImageTooltip ImageTooltip}
 *     is used.
 * </p>
 */
public class HierarchyCellFactory extends TreeCell<RepositoryEntity> {
    private static final ResourceBundle resources = UiUtilities.getResources();
    private final WebClient client;

    /**
     * Creates the cell factory.
     *
     * @param client  the client from which icons and additional information will be retrieved
     */
    public HierarchyCellFactory(WebClient client) {
        this.client = client;
    }

    @Override
    public void updateItem(RepositoryEntity repositoryEntity, boolean empty) {
        super.updateItem(repositoryEntity, empty);

        textProperty().unbind();
        setText(null);
        setGraphic(null);
        setTooltip(null);
        setOpacity(1);

        if (!empty && repositoryEntity != null) {
            setIcon(repositoryEntity.getClass());

            Tooltip tooltip = new Tooltip();

            if (repositoryEntity instanceof OrphanedFolder orphanedFolder) {
                textProperty().bind(
                        Bindings.when(client.getRequestsHandler().getOrphanedImagesLoading())
                                .then(Bindings.concat(
                                        orphanedFolder.getName(),
                                        " (",
                                        resources.getString("Browser.Browser.Hierarchy.loading"),
                                        "...)")
                                )
                                .otherwise(Bindings.concat(orphanedFolder.getName(), " (", orphanedFolder.getNumberOfChildren(), ")")));

                tooltip.setText(orphanedFolder.getName());
            } else if (repositoryEntity instanceof Project || repositoryEntity instanceof Dataset) {
                String title = repositoryEntity.getName() + " (" + repositoryEntity.getNumberOfChildren() + ")";

                setText(title);
                tooltip.setText(title);
            } else if (repositoryEntity instanceof Image image) {
                setText(repositoryEntity.getName());
                if (!image.isSupported()) {
                    setOpacity(0.5);
                }
                tooltip.setGraphic(new ImageTooltip(image, client));
            }

            setTooltip(tooltip);
        }
    }

    private void setIcon(Class<? extends RepositoryEntity> type) {
        var optionalIcon = client.getOmeroIcon(type);
        if (optionalIcon.isPresent()) {
            Canvas iconCanvas = new Canvas(15, 15);
            UiUtilities.paintBufferedImageOnCanvas(optionalIcon.get(), iconCanvas);
            setGraphic(iconCanvas);
        }
    }
}
