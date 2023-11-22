package qupath.ext.omero.gui.browser.serverbrowser.hierarchy;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
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
    private static final char VALID_CHARACTER = 10003;
    private static final char ERROR_CHARACTER = 10007;
    @FXML
    private Canvas canvas;
    @FXML
    private Label name;
    @FXML
    private HBox errorLine;
    @FXML
    private Label uint8;
    @FXML
    private Label has3Channels;

    /**
     * Creates the ImageTooltip.
     *
     * @param image  the image to describe
     * @param client  the corresponding client, used to retrieve the thumbnail
     * @throws IOException if an error occurs while creating the tooltip
     */
    public ImageTooltip(Image image, WebClient client) throws IOException {
        UiUtilities.loadFXML(this, ImageTooltip.class.getResource("image_tooltip.fxml"));

        name.setText(image.getName());

        setErrorLine(image);
        image.isSupported().addListener(change -> Platform.runLater(() -> setErrorLine(image)));

        client.getApisHandler().getThumbnail(image.getId()).thenAccept(thumbnail -> Platform.runLater(() ->
                thumbnail.ifPresent(bufferedImage -> UiUtilities.paintBufferedImageOnCanvas(bufferedImage, canvas)))
        );
    }

    private void setSupportedToLabel(boolean supported, Label label, String feature) {
        if (supported) {
            label.getStyleClass().remove(INVALID_CLASS_NAME);
            label.setText(feature + VALID_CHARACTER);
        } else {
            label.getStyleClass().add(INVALID_CLASS_NAME);
            label.setText(feature + ERROR_CHARACTER);
        }
    }

    private void setErrorLine(Image image) {
        if (image.isSupported().get()) {
            getChildren().remove(errorLine);
        } else {
            setSupportedToLabel(image.isUint8(), uint8, "- " + resources.getString("Browser.ServerBrowser.Hierarchy.uint8") + " ");
            setSupportedToLabel(image.has3Channels(), has3Channels, "- 3 " + resources.getString("Browser.ServerBrowser.Hierarchy.channels") + " ");
        }
    }
}
