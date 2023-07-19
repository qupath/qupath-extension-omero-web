package qupath.lib.images.servers.omero.common.omeroentities.repositoryentities.serverentities;

import com.google.gson.annotations.SerializedName;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import qupath.lib.images.servers.omero.common.api.requests.RequestsHandler;
import qupath.lib.images.servers.omero.common.gui.UiUtilities;
import qupath.lib.images.servers.omero.common.omeroentities.repositoryentities.RepositoryEntity;

import java.util.ResourceBundle;

/**
 * Represents an OMERO project.
 * A project contains {@link qupath.lib.images.servers.omero.common.omeroentities.repositoryentities.serverentities.Dataset Datasets}.
 */
public class Project extends ServerEntity {
    private static final ResourceBundle resources = UiUtilities.getResources();
    private static final String[] ATTRIBUTES = new String[] {
            resources.getString("Common.OmeroEntities.Project.name"),
            resources.getString("Common.OmeroEntities.Project.id"),
            resources.getString("Common.OmeroEntities.Project.description"),
            resources.getString("Common.OmeroEntities.Project.owner"),
            resources.getString("Common.OmeroEntities.Project.group"),
            resources.getString("Common.OmeroEntities.Project.nbDatasets")
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
     * Indicates if an OMERO entity type refers to a project
     *
     * @param type  the OMERO entity type
     * @return whether this type refers to a project
     */
    public static boolean isOfType(String type) {
        return "http://www.openmicroscopy.org/Schemas/OME/2016-06#Project".equalsIgnoreCase(type) || "Project".equalsIgnoreCase(type);
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
            requestsHandler.getDatasets(this).thenAccept(children -> Platform.runLater(() ->
                    this.children.addAll(children)
            ));
        }
    }
}
