package qupath.ext.omero.core.entities.repositoryentities.serverentities;

import com.google.gson.annotations.SerializedName;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
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
    private final transient ObservableList<Dataset> children = FXCollections.observableArrayList();
    private final transient ObservableList<Dataset> childrenImmutable = FXCollections.unmodifiableObservableList(children);
    private transient boolean childrenPopulated = false;
    private transient ApisHandler apisHandler;
    private transient boolean isPopulating = false;
    @SerializedName(value = "Description") private String description;
    @SerializedName(value = "omero:childCount") private int childCount;

    /**
     * Creates an empty project.
     */
    public Project() {
        // This constructor is declared because otherwise transient fields
        // of this class are not declared when it is created through JSON
    }

    /**
     * Creates an empty project only defined by its ID.
     */
    public Project(long id) {
        this.id = id;
    }

    @Override
    public boolean hasChildren() {
        return childCount > 0;
    }

    /**
     * @throws IllegalStateException when the APIs handler has not been set (see {@link #setApisHandler(ApisHandler)})
     */
    @Override
    public ObservableList<? extends RepositoryEntity> getChildren() {
        if (!childrenPopulated) {
            populateChildren();
            childrenPopulated = true;
        }
        return childrenImmutable;
    }

    @Override
    public ReadOnlyStringProperty getLabel() {
        String name = this.name == null ? "" : this.name;
        return new SimpleStringProperty(name + " (" + childCount + ")");
    }

    @Override
    public boolean isPopulatingChildren() {
        return isPopulating;
    }

    @Override
    public String getAttributeName(int informationIndex) {
        if (informationIndex < ATTRIBUTES.length) {
            return ATTRIBUTES[informationIndex];
        } else {
            return "";
        }
    }

    @Override
    public String getAttributeValue(int informationIndex) {
        return switch (informationIndex) {
            case 0 -> name == null || name.isEmpty() ? "-" : name;
            case 1 -> String.valueOf(getId());
            case 2 -> description == null || description.isEmpty() ? "-" : description;
            case 3 -> getOwner().getFullName();
            case 4 -> getGroup().getName();
            case 5 -> String.valueOf(childCount);
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
     * Set the APIs handler for this project. This is needed to populate its children.
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
            isPopulating = true;
            apisHandler.getDatasets(getId()).thenAccept(datasets -> {
                children.addAll(datasets);
                isPopulating = false;
            });
        }
    }
}
