package qupath.ext.omero.gui.annotationsender;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.VBox;
import qupath.ext.omero.gui.UiUtilities;

import java.io.IOException;
import java.util.ResourceBundle;

/**
 * Form that can be used in a dialog to let the user choose
 * some parameters when sending annotations to an OMERO server.
 */
class AnnotationForm extends VBox {

    private static final ResourceBundle resources = UiUtilities.getResources();
    private static final String ONLY_SELECTED_ANNOTATIONS = resources.getString("AnnotationsSender.AnnotationForm.onlySelectedAnnotations");
    private static final String ALL_ANNOTATIONS = resources.getString("AnnotationsSender.AnnotationForm.allAnnotations");
    @FXML
    private ChoiceBox<String> selectedChoice;
    @FXML
    private CheckBox deleteExistingData;

    /**
     * Creates the annotation form.
     *
     * @throws IOException if an error occurs while creating the form
     */
    public AnnotationForm() throws IOException {
        UiUtilities.loadFXML(this, AnnotationForm.class.getResource("annotation_form.fxml"));

        selectedChoice.getItems().setAll(ONLY_SELECTED_ANNOTATIONS, ALL_ANNOTATIONS);
        selectedChoice.getSelectionModel().select(ALL_ANNOTATIONS);
    }

    /**
     * @return whether only selected annotations should be sent to the server
     */
    public boolean areOnlySelectedAnnotationsSelected() {
        return selectedChoice.getSelectionModel().getSelectedItem().equals(ONLY_SELECTED_ANNOTATIONS);
    }

    /**
     * @return whether existing data on the OMERO server should be deleted
     */
    public boolean deleteExistingDataSelected() {
        return deleteExistingData.isSelected();
    }
}
