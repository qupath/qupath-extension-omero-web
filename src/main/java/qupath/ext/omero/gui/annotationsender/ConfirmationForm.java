package qupath.ext.omero.gui.annotationsender;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import qupath.ext.omero.gui.UiUtilities;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Form that can be used in a dialog to ask the
 * user for confirmation.
 */
class ConfirmationForm extends VBox {

    @FXML
    private Label information;
    @FXML
    private Label uri;
    @FXML
    private Label existingAnnotationsDeleted;

    /**
     * Creates the confirmation form.
     *
     * @param numberOfAnnotations  the number of annotations about to be sent
     * @param imageURI  the URI of the image
     * @param existingDeleted  whether existing annotations should be deleted
     * @throws IOException if an error occurs while creating the form
     */
    public ConfirmationForm(int numberOfAnnotations, String imageURI, boolean existingDeleted) throws IOException {
        UiUtilities.loadFXML(this, ConfirmationForm.class.getResource("confirmation_form.fxml"));
        ResourceBundle resources = UiUtilities.getResources();

        if (numberOfAnnotations == 1) {
            information.setText(resources.getString("AnnotationsSender.ConfirmationForm.annotationWillBeSent"));
        } else {
            information.setText(MessageFormat.format(resources.getString("AnnotationsSender.ConfirmationForm.annotationsWillBeSent"), numberOfAnnotations));
        }

        uri.setText(imageURI);

        if (existingDeleted) {
            existingAnnotationsDeleted.setText(resources.getString("AnnotationsSender.ConfirmationForm.existingAnnotationsDeleted"));
        }
    }
}
