package qupath.ext.omero.core.entities.annotations;

import com.google.gson.annotations.SerializedName;
import qupath.ext.omero.gui.UiUtilities;

import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Annotation containing a text tag.
 */
public class TagAnnotation extends Annotation {

    private static final ResourceBundle resources = UiUtilities.getResources();
    @SerializedName(value = "textValue") private String value;

    @Override
    public String toString() {
        return String.format("%s. Value: %s", super.toString(), value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof TagAnnotation tagAnnotation))
            return false;
        return Objects.equals(tagAnnotation.value, value);
    }

    @Override
    public int hashCode() {
        return (value == null ? "" : value).hashCode();
    }

    /**
     * @return a localized title for a tag annotation
     */
    public static String getTitle() {
        return resources.getString("Web.Entities.Annotation.Tag.title");
    }

    /**
     * Indicates if an annotation type refers to a tag annotation.
     *
     * @param type  the annotation type
     * @return whether this annotation type refers to a tag annotation
     */
    public static boolean isOfType(String type) {
        return "TagAnnotationI".equalsIgnoreCase(type) || "tag".equalsIgnoreCase(type);
    }

    /**
     * @return the actual tag of the annotation
     */
    public Optional<String> getValue() {
        return Optional.ofNullable(value);
    }
}
