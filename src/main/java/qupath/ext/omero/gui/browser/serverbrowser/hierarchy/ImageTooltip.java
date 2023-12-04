package qupath.ext.omero.gui.browser.serverbrowser.hierarchy;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import qupath.ext.omero.core.WebClient;
import qupath.ext.omero.core.entities.repositoryentities.serverentities.image.Image;
import qupath.ext.omero.gui.UiUtilities;

import java.io.IOException;
import java.util.ResourceBundle;

/**
 * A pane that shows the name, a thumbnail, and whether an image is
 * supported by the extension.
 */
class ImageTooltip extends VBox {

    private static final ResourceBundle resources = UiUtilities.getResources();
    private static final String INVALID_CLASS_NAME = "invalid-image";
    @FXML
    private Canvas canvas;
    @FXML
    private Label name;
    @FXML
    private VBox errorContainer;
    @FXML
    private VBox errors;

    /**
     * Creates the ImageTooltip.
     *
     * @param image  the image to describe
     * @param client  the corresponding client, used to retrieve the thumbnail
     * @throws IOException if an error occurs while creating the tooltip
     */
    public ImageTooltip(Image image, WebClient client) throws IOException {
        UiUtilities.loadFXML(this, ImageTooltip.class.getResource("image_tooltip.fxml"));

        name.setText(image.getLabel().get());

        setErrorLine(image);
        image.isSupported().addListener(change -> Platform.runLater(() -> setErrorLine(image)));

        client.getApisHandler().getThumbnail(image.getId()).thenAccept(thumbnail -> Platform.runLater(() ->
                thumbnail.ifPresent(bufferedImage -> UiUtilities.paintBufferedImageOnCanvas(bufferedImage, canvas)))
        );
    }

    private void setErrorLine(Image image) {
        getChildren().remove(errorContainer);
        errors.getChildren().clear();

        if (!image.isSupported().get()) {
            getChildren().add(errorContainer);

            for (Image.UNSUPPORTED_REASON reason: image.getUnsupportedReasons()) {
                Label error = new Label(switch (reason) {
                    case NUMBER_OF_CHANNELS -> resources.getString("Browser.ServerBrowser.Hierarchy.numberOfChannels");
                    case PIXEL_TYPE -> resources.getString("Browser.ServerBrowser.Hierarchy.pixelType");
                    case PIXEL_API_UNAVAILABLE -> resources.getString("Browser.ServerBrowser.Hierarchy.pixelAPI");
                });
                error.getStyleClass().add(INVALID_CLASS_NAME);

                errors.getChildren().add(error);
            }
        }
    }
}
