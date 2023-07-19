package qupath.lib.images.servers.omero.browser.browseserver.browser.hierarchy;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import qupath.lib.images.servers.omero.common.api.clients.WebClient;
import qupath.lib.images.servers.omero.common.omeroentities.repositoryentities.serverentities.image.Image;
import qupath.lib.images.servers.omero.common.gui.UiUtilities;

import java.util.ResourceBundle;

/**
 * A pane that shows the name, a thumbnail, and whether an image is
 * supported by the extension.
 */
class ImageTooltip extends VBox {
    private final static String INVALID_CLASS_NAME = "invalid-image";
    private final static char VALID_CHARACTER = 10003;
    private final static char ERROR_CHARACTER = 10007;
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
     */
    public ImageTooltip(Image image, WebClient client) {
        ResourceBundle resources = UiUtilities.loadFXMLAndGetResources(this, getClass().getResource("image_tooltip.fxml"));

        name.setText(image.getName());

        if (image.isSupported()) {
            getChildren().remove(errorLine);
        } else {
            setSupportedToLabel(image.isUint8(), uint8, "- " + resources.getString("Browser.Browser.Hierarchy.uint8") + " ");
            setSupportedToLabel(image.has3Channels(), has3Channels, "- 3 " + resources.getString("Browser.Browser.Hierarchy.channels") + " ");
        }

        client.getThumbnail(image.getId()).thenAccept(thumbnail -> Platform.runLater(() ->
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
}
