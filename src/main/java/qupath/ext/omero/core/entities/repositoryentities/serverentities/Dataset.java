package qupath.ext.omero.core.entities.repositoryentities.serverentities;

import com.google.gson.annotations.SerializedName;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import qupath.ext.omero.core.apis.ApisHandler;
import qupath.ext.omero.core.entities.repositoryentities.serverentities.image.Image;
import qupath.ext.omero.gui.UiUtilities;
import qupath.ext.omero.core.entities.repositoryentities.OrphanedFolder;
import qupath.ext.omero.core.entities.repositoryentities.RepositoryEntity;

import java.util.ResourceBundle;

/**
 * Represents an OMERO dataset.
 * A dataset contains images (described in {@link qupath.ext.omero.core.entities.repositoryentities.serverentities.image image}),
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
    private final transient ObservableList<Image> children = FXCollections.observableArrayList();
    private final transient ObservableList<Image> childrenImmutable = FXCollections.unmodifiableObservableList(children);
    private transient boolean childrenPopulated = false;
    private transient ApisHandler apisHandler;
    private transient boolean isPopulating = false;
    @SerializedName(value = "Description") private String description;
    @SerializedName(value = "omero:childCount") private int childCount;

    /**
     * Creates an empty dataset.
     */
    public Dataset() {
        // This constructor is declared because otherwise transient fields
        // of this class are not declared when it is created through JSON
    }

    /**
     * Creates an empty dataset only defined by its ID.
     */
    public Dataset(long id) {
        this.id = id;
    }

    @Override
    public int getNumberOfChildren() {
        return childCount;
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
        return String.format("Dataset %s of ID %d", name, id);
    }

    /**
     * Indicates if an OMERO entity type refers to a dataset
     *
     * @param type  the OMERO entity type
     * @return whether this type refers to a dataset
     */
    public static boolean isDataset(String type) {
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
            isPopulating = true;
            apisHandler.getImages(getId()).thenAccept(images -> {
                children.addAll(images);
                isPopulating = false;
            });
        }
    }
}
