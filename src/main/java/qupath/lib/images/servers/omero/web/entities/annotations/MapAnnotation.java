package qupath.lib.images.servers.omero.web.entities.annotations;

import com.google.gson.annotations.SerializedName;
import qupath.lib.images.servers.omero.gui.UiUtilities;

import java.util.Map;
import java.util.ResourceBundle;

/**
 * Annotation containing several key-value pairs (e.g. license, release date).
 */
public class MapAnnotation extends Annotation {

    private static final ResourceBundle resources = UiUtilities.getResources();
    @SerializedName(value = "values") private Map<String, String> values;

    @Override
    public String toString() {
        return String.format("%s. Values: %s", super.toString(), values);
    }

    /**
     * @return a localized title for a map annotation
     */
    public static String getTitle() {
        return resources.getString("Web.Entities.Annotation.Map.title");
    }

    /**
     * Indicates if an annotation type refers to a map annotation.
     *
     * @param type  the annotation type
     * @return whether this annotation type refers to a map annotation
     */
    public static boolean isOfType(String type) {
        return "MapAnnotationI".equalsIgnoreCase(type) || "map".equalsIgnoreCase(type);
    }

    /**
     * @return the key-value pairs of this annotation
     */
    public Map<String, String> getValues() {
        return values == null ? Map.of() : values;
    }
}
