package qupath.lib.images.servers.omero.gui.connectionsmanager.connection;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import qupath.lib.images.servers.omero.web.RequestSender;
import qupath.lib.images.servers.omero.gui.UiUtilities;
import qupath.lib.images.servers.omero.web.WebClient;
import qupath.lib.images.servers.omero.web.WebUtilities;

import java.io.IOException;
import java.net.URI;
import java.util.ResourceBundle;

/**
 * Label showing whether an image is accessible.
 */
class Image extends HBox {

    private static final ResourceBundle resources = UiUtilities.getResources();
    @FXML
    private Label name;
    @FXML
    private Canvas thumbnail;

    /**
     * Creates an Image label.
     *
     * @param client  the client owning this image
     * @param imageUri  the URI of the image
     * @throws IOException if an error occurs while creating the window
     */
    public Image(WebClient client, URI imageUri) throws IOException {
        UiUtilities.loadFXML(this, getClass().getResource("image.fxml"));

        var imageID = WebUtilities.parseEntityId(imageUri);
        if (imageID.isPresent()) {
            client.getApisHandler().getImage(imageID.getAsInt()).thenAccept(image -> Platform.runLater(() ->
                    image.ifPresent(value -> name.setText(value.getName()))
            ));

            client.getApisHandler().getThumbnail(imageID.getAsInt(), (int) thumbnail.getWidth()).thenAccept(thumbnail -> Platform.runLater(() ->
                    thumbnail.ifPresent(bufferedImage -> UiUtilities.paintBufferedImageOnCanvas(bufferedImage, this.thumbnail))
            ));
        }

        RequestSender.isLinkReachable(imageUri).thenAccept(success -> Platform.runLater(() ->
                setStatus(success ? imageUri.toString() : resources.getString("ConnectionsManager.Image.unreachableImage"), success))
        );
    }

    private void setStatus(String text, boolean isActive) {
        name.setTooltip(new Tooltip(text));
        name.setGraphic(UiUtilities.createStateNode(isActive));
    }
}
