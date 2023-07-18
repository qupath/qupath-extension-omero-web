package qupath.lib.images.servers.omero.browser.browse_server.browser.hierarchy;

import javafx.beans.binding.Bindings;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import qupath.lib.images.servers.omero.common.api.clients.WebClient;
import qupath.lib.images.servers.omero.common.omero_entities.repository_entities.*;
import qupath.lib.images.servers.omero.common.omero_entities.repository_entities.server_entities.Dataset;
import qupath.lib.images.servers.omero.common.omero_entities.repository_entities.server_entities.Project;
import qupath.lib.images.servers.omero.common.omero_entities.repository_entities.server_entities.image.Image;
import qupath.lib.images.servers.omero.common.gui.UiUtilities;

import java.util.ResourceBundle;

/**
 * <p>Cell factory of the hierarchy of a {@link qupath.lib.images.servers.omero.browser.browse_server.browser.Browser browser}.</p>
 * <p>
 *     It displays the name of each OMERO entity, a corresponding icon, the number of children (if any),
 *     and additional information with a tooltip.
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
