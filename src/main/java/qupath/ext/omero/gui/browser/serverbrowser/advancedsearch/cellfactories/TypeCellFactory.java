package qupath.ext.omero.gui.browser.serverbrowser.advancedsearch.cellfactories;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import qupath.ext.omero.core.WebClient;
import qupath.ext.omero.core.entities.repositoryentities.RepositoryEntity;
import qupath.ext.omero.core.entities.repositoryentities.serverentities.image.Image;
import qupath.ext.omero.core.entities.search.SearchResult;
import qupath.ext.omero.gui.UiUtilities;

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
            client.getApisHandler().getThumbnail(item.getId()).thenAccept(thumbnail -> Platform.runLater(() -> {
                if (thumbnail.isPresent()) {
                    show(item, thumbnail.get());
                } else {
                    setIcon(item);
                }
            }));
        }
    }

    private void show(SearchResult item, BufferedImage icon) {
        Canvas canvas = new Canvas(icon.getWidth(), icon.getHeight());
        var writableImage = UiUtilities.paintBufferedImageOnCanvas(icon, canvas);

        Tooltip tooltip = new Tooltip();
        if (item.getType().isPresent() && item.getType().get().equals(Image.class)) {
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

    private void setIcon(SearchResult item) {
        Optional<Class<? extends RepositoryEntity>> type = item.getType();

        if (type.isPresent()) {
            client.getApisHandler().getOmeroIcon(type.get()).thenAccept(icon -> Platform.runLater(() -> {
                if (icon.isPresent()) {
                    show(item, icon.get());
                } else {
                    hide();
                }
            }));
        } else {
            hide();
        }
    }

    private void hide() {
        setTooltip(null);
        setGraphic(null);
    }
}
