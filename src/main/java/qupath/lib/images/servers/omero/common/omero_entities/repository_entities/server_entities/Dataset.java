package qupath.lib.images.servers.omero.common.omero_entities.repository_entities.server_entities;

import com.google.gson.annotations.SerializedName;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import qupath.lib.images.servers.omero.common.api.requests.RequestsHandler;
import qupath.lib.images.servers.omero.common.gui.UiUtilities;
import qupath.lib.images.servers.omero.common.omero_entities.repository_entities.RepositoryEntity;

import java.util.ResourceBundle;

/**
 * Represents an OMERO dataset.
 * A dataset contains images, and is a child of a project or an orphaned folder.
 */
public class Dataset extends ServerEntity {
    private static final ResourceBundle resources = UiUtilities.getResources();
    private static final String[] ATTRIBUTES = new String[] {
            resources.getString("Common.OmeroEntities.Dataset.name"),
            resources.getString("Common.OmeroEntities.Dataset.id"),
            resources.getString("Common.OmeroEntities.Dataset.description"),
            resources.getString("Common.OmeroEntities.Dataset.owner"),
            resources.getString("Common.OmeroEntities.Dataset.group"),
            resources.getString("Common.OmeroEntities.Dataset.nbImages")
    };
    private boolean childrenPopulated = false;
    private RequestsHandler requestsHandler;
    @SerializedName(value = "Description") private String description;
    @SerializedName(value = "omero:childCount") private int childCount;

    @Override
    public int getNumberOfChildren() {
        return childCount;
    }

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
     * Set the request handler for this dataset. This is needed to populate its children.
     *
     * @param requestsHandler the request handler of this browser
     */
    public void setRequestsHandler(RequestsHandler requestsHandler) {
        this.requestsHandler = requestsHandler;
    }

    private void populateChildren() {
        if (requestsHandler != null) {
            requestsHandler.getImages(this).thenAccept(children -> Platform.runLater(() ->
                    this.children.addAll(children)
            ));
        }
    }
}
