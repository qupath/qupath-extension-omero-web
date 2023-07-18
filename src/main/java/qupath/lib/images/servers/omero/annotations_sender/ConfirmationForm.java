package qupath.lib.images.servers.omero.annotations_sender;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import qupath.lib.images.servers.omero.common.gui.UiUtilities;

import java.text.MessageFormat;
import java.util.ResourceBundle;

class ConfirmationForm extends VBox {
    @FXML
    private TextField information;
    @FXML
    private TextField uri;

    public ConfirmationForm(int numberOfAnnotations, String imageURI) {
        ResourceBundle resources = UiUtilities.loadFXMLAndGetResources(this, getClass().getResource("confirmation_form.fxml"));

        if (numberOfAnnotations == 1) {
            information.setText(resources.getString("AnnotationsSender.ConfirmationForm.annotationWillBeSent"));
        } else {
            information.setText(MessageFormat.format(resources.getString("AnnotationsSender.ConfirmationForm.annotationsWillBeSent"), numberOfAnnotations));
        }

        uri.setText(imageURI);
    }
}
