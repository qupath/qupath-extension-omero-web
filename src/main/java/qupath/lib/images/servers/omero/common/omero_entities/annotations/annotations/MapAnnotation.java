package qupath.lib.images.servers.omero.common.omero_entities.annotations.annotations;

import com.google.gson.annotations.SerializedName;
import qupath.lib.images.servers.omero.common.gui.UiUtilities;
import qupath.lib.images.servers.omero.common.omero_entities.annotations.Annotation;

import java.util.Map;
import java.util.ResourceBundle;

/**
 * Annotation containing several key-value pairs (e.g. license, release date)
 */
public class MapAnnotation extends Annotation {
    private static final ResourceBundle resources = UiUtilities.getResources();
    @SerializedName(value = "values") private Map<String, String> values;

    /**
     * @return a localized title for a map annotation
     */
    public static String getTitle() {
        return resources.getString("Common.OmeroEntities.Annotation.Map.title");
    }

    /**
     * Indicates if an annotation type refers to a map annotation
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
