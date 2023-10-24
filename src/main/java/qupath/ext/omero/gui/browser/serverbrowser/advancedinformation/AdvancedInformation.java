package qupath.ext.omero.gui.browser.serverbrowser.advancedinformation;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TitledPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;
import qupath.ext.omero.core.entities.annotations.*;
import qupath.ext.omero.core.entities.repositoryentities.serverentities.ServerEntity;
import qupath.ext.omero.core.entities.repositoryentities.serverentities.image.Image;
import qupath.ext.omero.gui.UiUtilities;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.tools.GuiTools;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;

/**
 * <p>
 *     Window displaying OMERO annotations of an OMERO entity.
 *     OMERO annotations are <b>not</b> similar to QuPath annotations. Rather, they represent metadata
 *     associated with OMERO entities.
 * </p>
 * <p>
 *     Annotations are displayed within several panes. Some annotations use a
 *     {@link FormPane}, while others use an {@link InformationPane}.
 * </p>
 */
public class AdvancedInformation extends Stage {

    private static final ResourceBundle resources = UiUtilities.getResources();
    private final ServerEntity serverEntity;
    @FXML
    private VBox content;

    /**
     * Create the advanced information window.
     *
     * @param owner  the stage who should own this window.
     * @param serverEntity  the OMERO entity whose information should be displayed.
     * @param annotationGroup  the OMERO annotations who belong to the OMERO entity.
     * @throws IOException if an error occurs while creating the window
     */
    public AdvancedInformation(Stage owner, ServerEntity serverEntity, AnnotationGroup annotationGroup) throws IOException {
        this.serverEntity = serverEntity;

        UiUtilities.loadFXML(this, AdvancedInformation.class.getResource("advanced_information.fxml"));

        setTitle(serverEntity.getName());

        content.getChildren().add(createObjectDetailPane());
        setAnnotationPanes(annotationGroup);

        addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                close();
            }
        });

        initOwner(owner);
        show();
    }

    private Node createObjectDetailPane() throws IOException {
        FormPane formPane = new FormPane(resources.getString("Browser.Browser.AdvancedInformation.details"));
        
        formPane.addRow(resources.getString("Browser.Browser.AdvancedInformation.id"), String.valueOf(serverEntity.getId()));
        formPane.addRow(resources.getString("Browser.Browser.AdvancedInformation.owner"), serverEntity.getOwner().getName());
        formPane.addRow(resources.getString("Browser.Browser.AdvancedInformation.group"), serverEntity.getGroup().getName());

        if (serverEntity instanceof Image image) {
            int startingIndex = 4;  // The first image attributes are already used before

            for (int i=startingIndex; i<image.getNumberOfAttributes(); ++i) {
                formPane.addRow(image.getAttributeInformation(i), image.getValueInformation(i));
            }
        }

        return formPane;
    }

    private void setAnnotationPanes(AnnotationGroup annotationGroup) throws IOException {
        for (var omeroAnnotationType: annotationGroup.getAnnotations().keySet()) {
            List<Annotation> annotationList = annotationGroup.getAnnotations().get(omeroAnnotationType);
            TitledPane pane = null;

            if (omeroAnnotationType.equals(TagAnnotation.class)) {
                pane = createTagPane(annotationGroup, annotationList);
            } else if (omeroAnnotationType.equals(MapAnnotation.class)) {
                pane = createMapPane(annotationGroup, annotationList);
            } else if (omeroAnnotationType.equals(FileAnnotation.class)) {
                pane = createFilePane(annotationGroup, annotationList);
            } else if (omeroAnnotationType.equals(CommentAnnotation.class)) {
                pane = createCommentPane(annotationGroup, annotationList);
            } else if (omeroAnnotationType.equals(RatingAnnotation.class)) {
                pane = createRatingPane(annotationList);
            }

            if (pane != null) {
                pane.setExpanded(annotationList.size() > 0);
                content.getChildren().add(pane);
            }
        }
    }

    private TitledPane createTagPane(AnnotationGroup annotationGroup, List<Annotation> annotationList) throws IOException {
        InformationPane tagPane = new InformationPane(TagAnnotation.getTitle() + " (" + annotationList.size() + ")");

        for (Annotation annotation : annotationList) {
            TagAnnotation tagAnnotation = (TagAnnotation) annotation;

            tagPane.addRow(tagAnnotation.getValue().orElse(""), MessageFormat.format(
                    resources.getString("Browser.Browser.AdvancedInformation.addedCreated"),
                    tagAnnotation.getAdder(annotationGroup),
                    tagAnnotation.getCreator(annotationGroup)
            ));
        }
        return tagPane;
    }

    private TitledPane createMapPane(AnnotationGroup annotationGroup, List<Annotation> annotationList) throws IOException {
        FormPane mapPane = new FormPane();
        int numberOfValues = 0;

        for (Annotation annotation : annotationList) {
            MapAnnotation mapAnnotation = (MapAnnotation) annotation;
            for (var value : mapAnnotation.getValues().entrySet()) {
                mapPane.addRow(
                        value.getKey(),
                        value.getValue().isEmpty() ? "-" : value.getValue(),
                        MessageFormat.format(
                                resources.getString("Browser.Browser.AdvancedInformation.addedCreated"),
                                mapAnnotation.getAdder(annotationGroup),
                                mapAnnotation.getCreator(annotationGroup)
                        )
                );
            }
            numberOfValues+= mapAnnotation.getValues().size();
        }
        mapPane.setTitle(MapAnnotation.getTitle() + " (" + numberOfValues + ")");

        return mapPane;
    }

    private TitledPane createFilePane(AnnotationGroup annotationGroup, List<Annotation> annotationList) throws IOException {
        InformationPane attachmentPane = new InformationPane(FileAnnotation.getTitle() + " (" + annotationList.size() + ")");

        for (Annotation annotation : annotationList) {
            FileAnnotation fileAnnotation = (FileAnnotation) annotation;
            attachmentPane.addRow(
                    fileAnnotation.getFilename().orElse("") +
                            " (" + fileAnnotation.getFileSize().orElse(0L) + " " + resources.getString("Browser.Browser.AdvancedInformation.bytes") + ")",
                    MessageFormat.format(
                            resources.getString("Browser.Browser.AdvancedInformation.addedCreatedType"),
                            fileAnnotation.getAdder(annotationGroup),
                            fileAnnotation.getCreator(annotationGroup),
                            fileAnnotation.getMimeType().orElse("")
                    )
            );
        }
        return attachmentPane;
    }

    private TitledPane createCommentPane(AnnotationGroup annotationGroup, List<Annotation> annotationList) throws IOException {
        InformationPane commentPane = new InformationPane(CommentAnnotation.getTitle() + " (" + annotationList.size() + ")");

        for (Annotation annotation : annotationList) {
            CommentAnnotation commentAnnotation = (CommentAnnotation) annotation;
            commentPane.addRow(commentAnnotation.getValue().orElse(""),
                    MessageFormat.format(
                            resources.getString("Browser.Browser.AdvancedInformation.added"),
                            commentAnnotation.getAdder(annotationGroup)
                    )
            );
        }
        return commentPane;
    }

    private TitledPane createRatingPane(List<Annotation> annotationList) throws IOException {
        InformationPane ratingPane = new InformationPane(RatingAnnotation.getTitle() + " (" + annotationList.size() + ")");

        int averageRating = 0;
        for (Annotation annotation : annotationList) {
            RatingAnnotation ratingAnnotation = (RatingAnnotation) annotation;
            averageRating += ratingAnnotation.getValue();
        }
        if (annotationList.size() != 0) {
            averageRating /= annotationList.size();

        }

        Glyph fullStarGlyph = new Glyph("FontAwesome", FontAwesome.Glyph.STAR).size(QuPathGUI.TOOLBAR_ICON_SIZE);
        Glyph fullStar = GuiTools.ensureDuplicatableGlyph(fullStarGlyph);
        for (int i = 0; i < averageRating; i++) {
            ratingPane.addColum(fullStar.duplicate());
        }

        Glyph emptyStarGlyph = fullStarGlyph.color(new Color(0, 0, 0, .2));
        Glyph emptyStar = GuiTools.ensureDuplicatableGlyph(emptyStarGlyph);
        for (int i = 0; i < RatingAnnotation.getMaxValue() - averageRating; i++) {
            ratingPane.addColum(emptyStar.duplicate());
        }

        return ratingPane;
    }
}
