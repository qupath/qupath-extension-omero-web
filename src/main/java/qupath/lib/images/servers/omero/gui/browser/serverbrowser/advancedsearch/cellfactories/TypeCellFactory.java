package qupath.lib.images.servers.omero.gui.browser.serverbrowser.advancedsearch.cellfactories;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import qupath.lib.images.servers.omero.web.WebClient;
import qupath.lib.images.servers.omero.web.entities.search.SearchResult;
import qupath.lib.images.servers.omero.web.entities.repositoryentities.serverentities.Dataset;
import qupath.lib.images.servers.omero.web.entities.repositoryentities.serverentities.Project;
import qupath.lib.images.servers.omero.gui.UiUtilities;

import java.awt.image.BufferedImage;
import java.util.Optional;

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
                Optional<BufferedImage> icon = client.getOmeroIcon(Project.class);
                if (icon.isPresent()) {
                    show(item, icon.get());
                } else {
                    hide();
                }
            } else if (item.getType().equalsIgnoreCase("dataset")) {
                Optional<BufferedImage> icon = client.getOmeroIcon(Dataset.class);
                if (icon.isPresent()) {
                    show(item, icon.get());
                } else {
                    hide();
                }
            } else {
                client.getThumbnail(item.getId()).thenAccept(thumbnail -> Platform.runLater(() -> {
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
