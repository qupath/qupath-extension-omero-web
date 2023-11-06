package qupath.ext.omero.core.entities.annotations;

import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.omero.core.entities.annotations.annotationsentities.Experimenter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An annotation group represents a set of {@link Annotation annotations}
 * attached to an OMERO entity.
 */
public class AnnotationGroup {

    private static final Logger logger = LoggerFactory.getLogger(AnnotationGroup.class);
    private final Map<Class<? extends Annotation>, List<Annotation>> annotations;

    /**
     * Creates an annotation group from a JSON object.
     *
     * @param json the JSON supposed to contain the annotation group.
     */
    public AnnotationGroup(JsonObject json) {
        this.annotations = createAnnotations(json, createExperimenters(json));
    }

    @Override
    public String toString() {
        return String.format("Annotation group containing %s", annotations);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof AnnotationGroup annotationGroup))
            return false;
        return annotationGroup.annotations.equals(annotations);
    }

    @Override
    public int hashCode() {
        return annotations.hashCode();
    }

    /**
     * Returns all annotations contained in this annotation group.
     * They are organized by type of annotation (e.g. all comment annotations form one group,
     * all file annotations form another group, etc).
     *
     * @return the annotations of this annotation group
     */
    public Map<Class<? extends Annotation>, List<Annotation>> getAnnotations() {
        return annotations;
    }

    private static Map<Class<? extends Annotation>, List<Annotation>> createAnnotations(JsonObject json, List<Experimenter> experimenters) {
        Gson gson = new GsonBuilder().registerTypeAdapter(Annotation.class, new Annotation.GsonOmeroAnnotationDeserializer()).setLenient().create();

        Map<Class<? extends Annotation>, List<Annotation>> annotations = new HashMap<>();

        JsonElement annotationsJSON = json.get("annotations");
        if (annotationsJSON != null) {
            try {
                JsonArray annotationsArray = annotationsJSON.getAsJsonArray();

                for (var jsonAnn: annotationsArray) {
                    try {
                        Annotation annotation = gson.fromJson(jsonAnn, Annotation.class);
                        if (annotation != null) {
                            annotation.updateAdderAndOwner(experimenters);

                            if (annotations.containsKey(annotation.getClass())) {
                                annotations.get(annotation.getClass()).add(annotation);
                            } else {
                                List<Annotation> annotationsForClass = new ArrayList<>();
                                annotationsForClass.add(annotation);
                                annotations.put(annotation.getClass(), annotationsForClass);
                            }
                        }
                    } catch (JsonSyntaxException e) {
                        logger.error("Error when reading " + jsonAnn, e);
                    }
                }
            } catch (IllegalStateException e) {
                logger.error("Could not convert " + annotationsJSON + " to JSON array", e);
            }
        }

        return annotations;
    }

    private static List<Experimenter> createExperimenters(JsonObject json) {
        Gson gson = new GsonBuilder().registerTypeAdapter(Annotation.class, new Annotation.GsonOmeroAnnotationDeserializer()).setLenient().create();

        List<Experimenter> experimenters = new ArrayList<>();

        JsonElement experimentersJSON = json.get("experimenters");
        if (experimentersJSON != null) {
            try {
                JsonArray experimentersArray = experimentersJSON.getAsJsonArray();

                for (var jsonExp: experimentersArray) {
                    try {
                        Experimenter experimenter = gson.fromJson(jsonExp, Experimenter.class);
                        if (experimenter != null) {
                            experimenters.add(experimenter);
                        }
                    } catch (JsonSyntaxException e) {
                        logger.error("Error when reading " + jsonExp, e);
                    }
                }
            } catch (IllegalStateException e) {
                logger.error("Could not convert " + experimentersJSON + " to JSON array", e);
            }
        }

        return experimenters;
    }
}
