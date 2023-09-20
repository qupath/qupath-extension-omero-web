package qupath.lib.images.servers.omero.web.entities.repositoryentities.serverentities;

import com.google.gson.annotations.SerializedName;
import javafx.collections.ObservableList;
import qupath.lib.images.servers.omero.web.apis.ApisHandler;
import qupath.lib.images.servers.omero.gui.UiUtilities;
import qupath.lib.images.servers.omero.web.entities.repositoryentities.OrphanedFolder;
import qupath.lib.images.servers.omero.web.entities.repositoryentities.RepositoryEntity;

import java.util.ResourceBundle;

/**
 * Represents an OMERO dataset.
 * A dataset contains images (described in {@link qupath.lib.images.servers.omero.web.entities.repositoryentities.serverentities.image image}),
 * and is a child of a {@link Project} or an {@link OrphanedFolder OrphanedFolder}.
 */
public class Dataset extends ServerEntity {

    private static final ResourceBundle resources = UiUtilities.getResources();
    private static final String[] ATTRIBUTES = new String[] {
            resources.getString("Web.Entities.Dataset.name"),
            resources.getString("Web.Entities.Dataset.id"),
            resources.getString("Web.Entities.Dataset.description"),
            resources.getString("Web.Entities.Dataset.owner"),
            resources.getString("Web.Entities.Dataset.group"),
            resources.getString("Web.Entities.Dataset.nbImages")
    };
    private transient boolean childrenPopulated = false;
    private transient ApisHandler apisHandler;
    @SerializedName(value = "Description") private String description;
    @SerializedName(value = "omero:childCount") private int childCount;

    @Override
    public int getNumberOfChildren() {
        return childCount;
    }

    /**
     * @throws IllegalStateException when the APIs handler has not been set (see {@link #setApisHandler(ApisHandler)})
     */
    @Override
    public ObservableList<RepositoryEntity> getChildren() {
        if (!childrenPopulated) {
            populateChildren();
            childrenPopulated = true;
        }
        return childrenImmutable;
    }

    @Override
    public String getType() {
        return "dataset";
    }

    @Override
    public String getAttributeInformation(int informationIndex) {
        if (informationIndex < ATTRIBUTES.length) {
            return ATTRIBUTES[informationIndex];
        } else {
            return "";
        }
    }

    @Override
    public String getValueInformation(int informationIndex) {
        return switch (informationIndex) {
            case 0 -> name == null || name.isEmpty() ? "-" : name;
            case 1 -> String.valueOf(getId());
            case 2 -> description == null || description.isEmpty() ? "-" : description;
            case 3 -> getOwner().getName();
            case 4 -> getGroup().getName();
            case 5 -> String.valueOf(getNumberOfChildren());
            default -> "";
        };
    }

    @Override
    public int getNumberOfAttributes() {
        return ATTRIBUTES.length;
    }

    @Override
    public String toString() {
        return String.format("Dataset %s of ID %d: %s", name, id, description);
    }

    /**
     * Indicates if an OMERO entity type refers to a dataset
     *
     * @param type  the OMERO entity type
     * @return whether this type refers to a dataset
     */
    public static boolean isOfType(String type) {
        return "http://www.openmicroscopy.org/Schemas/OME/2016-06#Dataset".equalsIgnoreCase(type) || "Dataset".equalsIgnoreCase(type);
    }

    /**
     * Set the APIs handler for this dataset. This is needed to populate its children.
     *
     * @param apisHandler the request handler of this browser
     */
    public void setApisHandler(ApisHandler apisHandler) {
        this.apisHandler = apisHandler;
    }

    private void populateChildren() {
        if (apisHandler == null) {
            throw new IllegalStateException(
                    "The APIs handler has not been set on this dataset. See the setApisHandler(ApisHandler) function."
            );
        } else {
            apisHandler.getImages(getId()).thenAccept(this.children::addAll);
        }
    }
}
