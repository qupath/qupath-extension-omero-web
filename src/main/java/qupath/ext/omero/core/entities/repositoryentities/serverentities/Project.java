package qupath.ext.omero.core.entities.repositoryentities.serverentities;

import com.google.gson.annotations.SerializedName;
import javafx.collections.ObservableList;
import qupath.ext.omero.core.apis.ApisHandler;
import qupath.ext.omero.gui.UiUtilities;
import qupath.ext.omero.core.entities.repositoryentities.RepositoryEntity;

import java.util.ResourceBundle;

/**
 * Represents an OMERO project.
 * A project contains {@link Dataset Datasets}.
 */
public class Project extends ServerEntity {

    private static final ResourceBundle resources = UiUtilities.getResources();
    private static final String[] ATTRIBUTES = new String[] {
            resources.getString("Web.Entities.Project.name"),
            resources.getString("Web.Entities.Project.id"),
            resources.getString("Web.Entities.Project.description"),
            resources.getString("Web.Entities.Project.owner"),
            resources.getString("Web.Entities.Project.group"),
            resources.getString("Web.Entities.Project.nbDatasets")
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
        return "project";
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
            case 3 -> getOwner().getFullName();
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
        return String.format("Project %s of ID %d", name, id);
    }

    /**
     * Indicates if an OMERO entity type refers to a project
     *
     * @param type  the OMERO entity type
     * @return whether this type refers to a project
     */
    public static boolean isProject(String type) {
        return "http://www.openmicroscopy.org/Schemas/OME/2016-06#Project".equalsIgnoreCase(type) || "Project".equalsIgnoreCase(type);
    }

    /**
     * Set the APIs handler for this dataset. This is needed to populate its children.
     *
     * @param apisHandler the APIs handler of this browser
     */
    public void setApisHandler(ApisHandler apisHandler) {
        this.apisHandler = apisHandler;
    }

    private void populateChildren() {
        if (apisHandler == null) {
            throw new IllegalStateException(
                    "The APIs handler has not been set on this project. See the setApisHandler(ApisHandler) function."
            );
        } else {
            apisHandler.getDatasets(getId()).thenAccept(this.children::addAll);
        }
    }
}
