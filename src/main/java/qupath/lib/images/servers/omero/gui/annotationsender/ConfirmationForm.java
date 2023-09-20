package qupath.lib.images.servers.omero.gui.annotationsender;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import qupath.lib.images.servers.omero.gui.UiUtilities;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Form that can be used in a dialog to show the number of annotations
 * to be sent and the image URI to the user.
 */
class ConfirmationForm extends VBox {

    @FXML
    private Label information;
    @FXML
    private Label uri;

    /**
     * Creates the confirmation form.
     *
     * @param numberOfAnnotations  the number of annotations about to be sent
     * @param imageURI  the URI of the image
     * @throws IOException if an error occurs while creating the form
     */
    public ConfirmationForm(int numberOfAnnotations, String imageURI) throws IOException {
        UiUtilities.loadFXML(this, getClass().getResource("confirmation_form.fxml"));
        ResourceBundle resources = UiUtilities.getResources();

        if (numberOfAnnotations == 1) {
            information.setText(resources.getString("AnnotationsSender.ConfirmationForm.annotationWillBeSent"));
        } else {
            information.setText(MessageFormat.format(resources.getString("AnnotationsSender.ConfirmationForm.annotationsWillBeSent"), numberOfAnnotations));
        }

        uri.setText(imageURI);
    }
}
