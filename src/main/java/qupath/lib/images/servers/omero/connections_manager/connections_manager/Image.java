package qupath.lib.images.servers.omero.connections_manager.connections_manager;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import qupath.lib.images.servers.omero.common.api.requests.Requests;
import qupath.lib.images.servers.omero.common.gui.UiUtilities;

import java.net.URI;
import java.util.ResourceBundle;

class Image extends Label {
    private static ResourceBundle resources;

    public Image(URI imageUri) {
        resources = UiUtilities.loadFXMLAndGetResources(this, getClass().getResource("image.fxml"));

        setText("../" + imageUri.getQuery());

        Requests.isLinkReachable(imageUri).thenAccept(success -> Platform.runLater(() ->
                setStatus(success ? imageUri.toString() : resources.getString("ConnectionsManager.Image.unreachableImage"), success))
        );
    }

    private void setStatus(String text, boolean isActive) {
        setTooltip(new Tooltip(text));
        setGraphic(UiUtilities.createStateNode(isActive));
    }
}
