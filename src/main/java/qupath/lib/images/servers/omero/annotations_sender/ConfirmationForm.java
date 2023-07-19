package qupath.lib.images.servers.omero.annotations_sender;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import qupath.lib.images.servers.omero.common.gui.UiUtilities;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Form that can be used in a dialog to show the number of annotations
 * to be sent and the image URI to the user.
 */
class ConfirmationForm extends VBox {
    @FXML
    private TextField information;
    @FXML
    private TextField uri;

    /**
     * Creates the confirmation form.
     *
     * @param numberOfAnnotations  the number of annotations about to be sent
     * @param imageURI  the URI of the image
     */
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
