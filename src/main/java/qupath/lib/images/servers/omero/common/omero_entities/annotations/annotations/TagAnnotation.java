package qupath.lib.images.servers.omero.common.omero_entities.annotations.annotations;

import com.google.gson.annotations.SerializedName;
import qupath.lib.images.servers.omero.common.gui.UiUtilities;
import qupath.lib.images.servers.omero.common.omero_entities.annotations.Annotation;

import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Annotation containing a text tag
 */
public class TagAnnotation extends Annotation {
    private static final ResourceBundle resources = UiUtilities.getResources();
    @SerializedName(value = "textValue") private String value;

    /**
     * @return a localized title for a tag annotation
     */
    public static String getTitle() {
        return resources.getString("Common.OmeroEntities.Annotation.Tag.title");
    }

    /**
     * Indicates if an annotation type refers to a tag annotation
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
