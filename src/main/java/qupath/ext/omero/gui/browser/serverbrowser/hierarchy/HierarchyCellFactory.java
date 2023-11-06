package qupath.ext.omero.gui.browser.serverbrowser.hierarchy;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.omero.core.WebClient;
import qupath.ext.omero.gui.browser.serverbrowser.BrowserModel;
import qupath.ext.omero.core.entities.repositoryentities.OrphanedFolder;
import qupath.ext.omero.core.entities.repositoryentities.RepositoryEntity;
import qupath.ext.omero.core.entities.repositoryentities.serverentities.Dataset;
import qupath.ext.omero.core.entities.repositoryentities.serverentities.Project;
import qupath.ext.omero.core.entities.repositoryentities.serverentities.image.Image;
import qupath.ext.omero.gui.UiUtilities;

import java.io.IOException;
import java.util.ResourceBundle;

/**
 * <p>
 *     Cell factory of the hierarchy of a {@link javafx.scene.control.TreeView TreeView} containing
 *     {@link RepositoryEntity RepositoryEntity} elements .</p>
 * <p>
 *     It displays the name of each OMERO entity, a corresponding icon, the number of children (if any),
 *     and additional information with a tooltip.
 * </p>
 * <p>
 *     If the entity is an {@link Image Image},
 *     a complex tooltip described in {@link ImageTooltip} is used.
 * </p>
 */
public class HierarchyCellFactory extends TreeCell<RepositoryEntity> {

    private static final Logger logger = LoggerFactory.getLogger(HierarchyCellFactory.class);
    private static final ResourceBundle resources = UiUtilities.getResources();
    private final WebClient client;
    private final BrowserModel browserModel;

    /**
     * Creates the cell factory.
     *
     * @param client  the client from which icons and additional information will be retrieved
     * @param browserModel  the browser model of the browser
     */
    public HierarchyCellFactory(WebClient client, BrowserModel browserModel) {
        this.client = client;
        this.browserModel = browserModel;
    }

    @Override
    public void updateItem(RepositoryEntity repositoryEntity, boolean empty) {
        super.updateItem(repositoryEntity, empty);

        textProperty().unbind();
        setText(null);
        setGraphic(null);
        setTooltip(null);
        opacityProperty().unbind();
        setOpacity(1);

        if (!empty && repositoryEntity != null) {
            setIcon(repositoryEntity.getClass());

            Tooltip tooltip = new Tooltip();

            if (repositoryEntity instanceof OrphanedFolder orphanedFolder) {
                textProperty().bind(
                        Bindings.when(browserModel.areOrphanedImagesLoading())
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

                opacityProperty().bind(Bindings.when(image.isSupported())
                        .then(1.0)
                        .otherwise(0.5)
                );

                try {
                    tooltip.setGraphic(new ImageTooltip(image, client));
                } catch (IOException e) {
                    logger.error("Error while creating image tooltip", e);
                }
            }

            setTooltip(tooltip);
        }
    }

    private void setIcon(Class<? extends RepositoryEntity> type) {
        client.getApisHandler().getOmeroIcon(type).thenAccept(icon -> Platform.runLater(() -> {
            if (icon.isPresent()) {
                Canvas iconCanvas = new Canvas(15, 15);
                UiUtilities.paintBufferedImageOnCanvas(icon.get(), iconCanvas);
                setGraphic(iconCanvas);
            }
        }));
    }
}
