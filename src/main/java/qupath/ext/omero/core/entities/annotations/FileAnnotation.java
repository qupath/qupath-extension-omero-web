package qupath.ext.omero.core.entities.annotations;

import com.google.gson.annotations.SerializedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.omero.gui.UiUtilities;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Annotation containing information on a file attached to an OMERO entity.
 */
public class FileAnnotation extends Annotation {

    private static final ResourceBundle resources = UiUtilities.getResources();
    private static final Logger logger = LoggerFactory.getLogger(FileAnnotation.class);
    @SerializedName(value = "file") private Map<String, String> map;

    @Override
    public String toString() {
        return String.format("%s. Map: %s", super.toString(), map);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof FileAnnotation fileAnnotation))
            return false;
        return Objects.equals(fileAnnotation.map, map);
    }

    @Override
    public int hashCode() {
        return (map == null ? "" : map).hashCode();
    }

    /**
     * @return a localized title for a file annotation
     */
    public static String getTitle() {
        return resources.getString("Web.Entities.Annotation.File.title");
    }

    /**
     * Indicates if an annotation type refers to a file annotation.
     *
     * @param type  the annotation type
     * @return whether this annotation type refers to a file annotation
     */
    public static boolean isOfType(String type) {
        return "FileAnnotationI".equalsIgnoreCase(type) || "file".equalsIgnoreCase(type);
    }

    /**
     * @return the name of the attached file, or an empty Optional if it was not found
     */
    public Optional<String> getFilename() {
        if (map == null) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(map.get("name"));
        }
    }

    /**
     * @return the MIME type of the attached file, or an empty Optional if it was not found
     */
    public Optional<String> getMimeType() {
        if (map == null) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(map.get("mimetype"));
        }
    }

    /**
     * @return the size of the attached file in bytes, or an empty Optional if it was not found
     */
    public Optional<Long> getFileSize() {
        if (map == null || map.get("size") == null) {
            return Optional.empty();
        } else {
            String size = map.get("size");
            try {
                return Optional.of(Long.parseLong(size));
            } catch (NumberFormatException e) {
                logger.warn("Cannot convert " + size + " to a number in file annotation", e);
                return Optional.empty();
            }
        }
    }
}
