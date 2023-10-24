package qupath.ext.omero.core.entities.annotations;

import com.google.gson.annotations.SerializedName;
import qupath.ext.omero.gui.UiUtilities;

import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Annotation containing a text comment.
 */
public class CommentAnnotation extends Annotation {

    private static final ResourceBundle resources = UiUtilities.getResources();
    @SerializedName(value = "textValue") private String value;

    @Override
    public String toString() {
        return String.format("%s. Value: %s", super.toString(), value);
    }

    /**
     * @return a localized title for a comment annotation
     */
    public static String getTitle() {
        return resources.getString("Web.Entities.Annotation.Comment.title");
    }

    /**
     * Indicates if an annotation type refers to a comment annotation.
     *
     * @param type  the annotation type
     * @return whether this annotation type refers to a comment annotation
     */
    public static boolean isOfType(String type) {
        return "CommentAnnotationI".equalsIgnoreCase(type) || "comment".equalsIgnoreCase(type);
    }

    /**
     * @return the actual comment of the annotation
     */
    public Optional<String> getValue() {
        return Optional.ofNullable(value);
    }
}
