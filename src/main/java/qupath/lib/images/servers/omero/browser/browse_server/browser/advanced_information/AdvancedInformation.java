package qupath.lib.images.servers.omero.browser.browse_server.browser.advanced_information;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TitledPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.tools.GuiTools;
import qupath.lib.images.servers.omero.common.omero_entities.annotations.*;
import qupath.lib.images.servers.omero.common.omero_entities.annotations.annotations.*;
import qupath.lib.images.servers.omero.common.omero_entities.repository_entities.server_entities.ServerEntity;
import qupath.lib.images.servers.omero.common.omero_entities.repository_entities.server_entities.image.Image;
import qupath.lib.images.servers.omero.common.gui.UiUtilities;

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
 *     {@link FormPane FormPane}, while others use an {@link InformationPane InformationPane}.
 * </p>
 */
public class AdvancedInformation extends Stage {
    private ResourceBundle resources;
    private final ServerEntity serverEntity;
    @FXML
    private VBox content;

    /**
     * Create the advanced information window.
     *
     * @param owner  the stage who should own this window.
     * @param serverEntity  the OMERO entity whose information should be displayed.
     * @param annotationGroup  the OMERO annotations who belong to the OMERO entity.
     */
    public AdvancedInformation(Stage owner, ServerEntity serverEntity, AnnotationGroup annotationGroup) {
        this.serverEntity = serverEntity;

        initUI(owner, annotationGroup);
    }

    private void initUI(Stage owner, AnnotationGroup annotationGroup) {
        resources = UiUtilities.loadFXMLAndGetResources(this, getClass().getResource("advanced_information.fxml"));

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

    private Node createObjectDetailPane() {
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

    private void setAnnotationPanes(AnnotationGroup annotationGroup) {
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
                pane = createRatingPane(annotationGroup, annotationList);
            }

            if (pane != null) {
                pane.setExpanded(annotationList.size() > 0);
                content.getChildren().add(pane);
            }
        }
    }

    private TitledPane createTagPane(AnnotationGroup annotationGroup, List<Annotation> annotationList) {
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

    private TitledPane createMapPane(AnnotationGroup annotationGroup, List<Annotation> annotationList) {
        FormPane mapPane = new FormPane(MapAnnotation.getTitle() + " (" + annotationList.size() + ")");

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
        }
        return mapPane;
    }

    private TitledPane createFilePane(AnnotationGroup annotationGroup, List<Annotation> annotationList) {
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

    private TitledPane createCommentPane(AnnotationGroup annotationGroup, List<Annotation> annotationList) {
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

    private TitledPane createRatingPane(AnnotationGroup annotationGroup, List<Annotation> annotationList) {
        InformationPane ratingPane = new InformationPane(RatingAnnotation.getTitle() + " (" + annotationList.size() + ")");

        int rating = 0;
        for (Annotation annotation : annotationList) {
            RatingAnnotation ratingAnnotation = (RatingAnnotation) annotation;
            rating += ratingAnnotation.getValue();
        }
        Glyph glyph = new Glyph("FontAwesome", FontAwesome.Glyph.STAR).size(QuPathGUI.TOOLBAR_ICON_SIZE);
        Glyph star = GuiTools.ensureDuplicatableGlyph(glyph);
        for (int i = 0; i < Math.round((float) rating / annotationGroup.getAnnotations().size()); i++) {
            ratingPane.addColum(star.duplicate());
        }
        return ratingPane;
    }
}
