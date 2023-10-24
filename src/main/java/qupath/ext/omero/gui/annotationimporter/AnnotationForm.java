package qupath.ext.omero.gui.annotationimporter;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;
import qupath.ext.omero.gui.UiUtilities;

import java.io.IOException;

/**
 * Form that can be used in a dialog to let the user choose
 * some parameters when importing annotations from an OMERO server.
 */
class AnnotationForm extends VBox {

    @FXML
    private CheckBox deleteAnnotations;
    @FXML
    private CheckBox deleteDetections;

    /**
     * Creates the annotation form.
     *
     * @throws IOException if an error occurs while creating the form
     */
    public AnnotationForm() throws IOException {
        UiUtilities.loadFXML(this, AnnotationForm.class.getResource("annotation_form.fxml"));
    }

    /**
     * @return whether existing QuPath annotations should be deleted
     */
    public boolean deleteCurrentAnnotations() {
        return deleteAnnotations.isSelected();
    }

    /**
     * @return whether existing QuPath detections should be deleted
     */
    public boolean deleteCurrentDetections() {
        return deleteDetections.isSelected();
    }
}
