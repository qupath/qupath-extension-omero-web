package qupath.ext.omero.gui.browser.serverbrowser.advancedsearch.cellfactories;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import qupath.ext.omero.core.WebClient;
import qupath.ext.omero.core.entities.repositoryentities.serverentities.Dataset;
import qupath.ext.omero.core.entities.repositoryentities.serverentities.Project;
import qupath.ext.omero.core.entities.search.SearchResult;
import qupath.ext.omero.gui.UiUtilities;

import java.awt.image.BufferedImage;

/**
 * Cell factory that displays an image representing the search result in the cell and in a tooltip.
 */
public class TypeCellFactory extends TableCell<SearchResult, SearchResult> {

    private final WebClient client;

    public TypeCellFactory(WebClient client) {
        this.client = client;
    }

    @Override
    protected void updateItem(SearchResult item, boolean empty) {
        super.updateItem(item, empty);

        if (item == null || empty) {
            hide();
        } else {
            if (item.getType().equalsIgnoreCase("project")) {
                client.getApisHandler().getOmeroIcon(Project.class).thenAccept(icon -> Platform.runLater(() -> {
                    if (icon.isPresent()) {
                        show(item, icon.get());
                    } else {
                        hide();
                    }
                }));
            } else if (item.getType().equalsIgnoreCase("dataset")) {
                client.getApisHandler().getOmeroIcon(Dataset.class).thenAccept(icon -> Platform.runLater(() -> {
                    if (icon.isPresent()) {
                        show(item, icon.get());
                    } else {
                        hide();
                    }
                }));
            } else {
                client.getApisHandler().getThumbnail(item.getId()).thenAccept(thumbnail -> Platform.runLater(() -> {
                    if (thumbnail.isPresent()) {
                        show(item, thumbnail.get());
                    } else {
                        hide();
                    }
                }));
            }
        }
    }

    private void hide() {
        setTooltip(null);
        setGraphic(null);
    }

    private void show(SearchResult item, BufferedImage icon) {
        Canvas canvas = new Canvas(icon.getWidth(), icon.getHeight());
        Image writableImage = UiUtilities.paintBufferedImageOnCanvas(icon, canvas);

        Tooltip tooltip = new Tooltip();
        if (item.getType().equalsIgnoreCase("image")) {
            ImageView imageView = new ImageView(writableImage);
            imageView.setFitHeight(250);
            imageView.setPreserveRatio(true);
            tooltip.setGraphic(imageView);
        } else {
            tooltip.setText(item.getName());
        }

        setTooltip(tooltip);
        setGraphic(canvas);
    }
}
