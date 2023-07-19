package qupath.lib.images.servers.omero.common.omeroentities.annotations;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.images.servers.omero.common.omeroentities.permissions.Owner;
import qupath.lib.images.servers.omero.common.omeroentities.annotations.annotations.*;
import qupath.lib.images.servers.omero.common.omeroentities.annotations.entities.Experimenter;
import qupath.lib.images.servers.omero.common.omeroentities.annotations.entities.Link;

import java.lang.reflect.Type;
import java.util.Optional;

/**
 * <p>
 *     An OMERO annotation is <b>not</b> similar to a QuPath annotation.
 *     It represents metadata attached to OMERO entities.
 * </p>
 * <p>
 *     Sub classes of this class can be found in the
 *     {@link qupath.lib.images.servers.omero.common.omeroentities.annotations.annotations annotations} package.
 * </p>
 */
public abstract class Annotation {
    private final static Logger logger = LoggerFactory.getLogger(Annotation.class);
    @SerializedName(value = "owner") private Owner owner;
    @SerializedName(value = "link") private Link link;

    /**
     * Get the full name of the experimenter that <b>added</b> this annotation.
     * This is not necessarily the owner of the annotation.
     *
     * @param annotationGroup  the annotationGroup this annotation belongs to
     * @return the full name of the adder, or an empty String it not found
     */
    public String getAdder(AnnotationGroup annotationGroup) {
        return annotationGroup.getExperimenters().stream()
                .filter(experimenter -> addedBy().filter(value -> experimenter.getId() == value.getId()).isPresent())
                .map(Experimenter::getFullName)
                .findAny()
                .orElse("");
    }

    /**
     * Get the full name of the experimenter that <b>created</b> this annotation.
     * This is not necessarily the adder of the annotation.
     *
     * @param annotationGroup  the annotationGroup this annotation belongs to
     * @return the full name of the creator, or an empty String it not found
     */
    public String getCreator(AnnotationGroup annotationGroup) {
        return annotationGroup.getExperimenters().stream()
                .filter(experimenter -> owner != null && experimenter.getId() == owner.getId())
                .map(Experimenter::getFullName)
                .findAny()
                .orElse("");
    }

    /**
     * Class that deserializes a JSON to an annotation.
     */
    static class GsonOmeroAnnotationDeserializer implements JsonDeserializer<Annotation> {
        @Override
        public Annotation deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            String type = ((JsonObject)json).get("class").getAsString();

            if (TagAnnotation.isOfType(type)) {
                return context.deserialize(json, TagAnnotation.class);
            } else if (MapAnnotation.isOfType(type)) {
                return context.deserialize(json, MapAnnotation.class);
            } else if (FileAnnotation.isOfType(type)) {
                return context.deserialize(json, FileAnnotation.class);
            } else if (CommentAnnotation.isOfType(type)) {
                return context.deserialize(json, CommentAnnotation.class);
            } else if (RatingAnnotation.isOfType(type)) {
                return context.deserialize(json, RatingAnnotation.class);
            } else {
                logger.warn("Unsupported type {}", type);
                return null;
            }
        }
    }

    private Optional<Owner> addedBy() {
        if (link == null) {
            return Optional.empty();
        } else {
            return link.getOwner();
        }
    }
}
