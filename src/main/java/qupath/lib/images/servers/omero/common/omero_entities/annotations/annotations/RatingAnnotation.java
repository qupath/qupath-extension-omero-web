package qupath.lib.images.servers.omero.common.omero_entities.annotations.annotations;

import com.google.gson.annotations.SerializedName;
import qupath.lib.images.servers.omero.common.gui.UiUtilities;
import qupath.lib.images.servers.omero.common.omero_entities.annotations.Annotation;

import java.util.ResourceBundle;

/**
 * An annotation containing a rating (from 0 to 5).
 */
public class RatingAnnotation extends Annotation {
    private static final ResourceBundle resources = UiUtilities.getResources();
    @SerializedName(value = "longValue") private short value;

    /**
     * @return a localized title for a map annotation
     */
    public static String getTitle() {
        return resources.getString("Common.OmeroEntities.Annotation.Rating.title");
    }

    /**
     * Indicates if an annotation type refers to a rating annotation.
     *
     * @param type  the annotation type
     * @return whether this annotation type refers to a rating annotation
     */
    public static boolean isOfType(String type) {
        return "LongAnnotationI".equalsIgnoreCase(type) || "rating".equalsIgnoreCase(type);
    }

    /**
     * @return the rating of the annotation (from 0 to 5)
     */
    public short getValue() {
        return value;
    }
}
