package qupath.ext.omero.core.entities.annotations;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.omero.core.entities.annotations.annotationsentities.Experimenter;
import qupath.ext.omero.core.entities.permissions.Owner;
import qupath.ext.omero.core.entities.annotations.annotationsentities.Link;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

/**
 * <p>
 *     An OMERO annotation is <b>not</b> similar to a QuPath annotation.
 *     It represents metadata attached to OMERO entities.
 * </p>
 */
public abstract class Annotation {

    private static final Logger logger = LoggerFactory.getLogger(Annotation.class);
    @SerializedName(value = "owner") private Owner owner;
    @SerializedName(value = "link") private Link link;

    @Override
    public String toString() {
        return String.format("Annotation owned by %s and added by %s", owner, link);
    }

    /**
     * <p>
     *     Update information about the adder and the creator of this annotation
     *     based on the provided list of experimenters. If the list of
     *     experimenters doesn't contain an experimenter corresponding to
     *     the current adder/owner, the adder/owner is not updated.
     * </p>
     * <p>
     *     This function is useful when the JSON creating this annotation lacks information
     *     on the adder and the owner.
     * </p>
     *
     * @param experimenters  the list of experimenters having information on
     *                       the adder and the owner of this annotation
     */
    public void updateAdderAndOwner(List<Experimenter> experimenters) {
        updateOwner(experimenters, owner).ifPresent(owner -> this.owner = owner);
        updateOwner(experimenters, link == null ? null : link.getOwner().orElse(null)).ifPresent(owner -> link = new Link(owner));
    }

    /**
     * Get the full name of the experimenter that <b>added</b> this annotation.
     * This is not necessarily the owner of the annotation.
     *
     * @return the full name of the adder, or an empty String it not found
     */
    public String getAdderFullName() {
        Optional<Owner> owner = link == null ? Optional.empty() : link.getOwner();
        if (owner.isPresent()) {
            return owner.get().getFullName();
        } else {
            return "";
        }
    }

    /**
     * Get the full name of the experimenter that <b>owns</b> this annotation.
     * This is not necessarily the adder of the annotation.
     *
     * @return the full name of the owner, or an empty String it not found
     */
    public String getOwnerFullName() {
        return owner == null ?  "" : owner.getFullName();
    }

    /**
     * Class that deserializes a JSON to an annotation.
     */
    public static class GsonOmeroAnnotationDeserializer implements JsonDeserializer<Annotation> {
        @Override
        public Annotation deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            if (json.isJsonObject() && json.getAsJsonObject().get("class") != null) {
                String type = json.getAsJsonObject().get("class").getAsString();

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
                    logger.warn("Unsupported type: {}", type);
                    return null;
                }
            } else {
                logger.warn("\"class\" attribute of the annotation missing");
                return null;
            }
        }
    }

    private static Optional<Owner> updateOwner(List<Experimenter> experimenters, Owner owner) {
        return experimenters.stream()
                .filter(experimenter -> owner != null && experimenter.getId() == owner.id())
                .findAny()
                .map(experimenter -> new Owner(
                        experimenter.getId(),
                        experimenter.getFirstName(),
                        owner.middleName(),
                        experimenter.getLastName(),
                        owner.emailAddress(),
                        owner.institution(),
                        owner.username()
                ));
    }
}
