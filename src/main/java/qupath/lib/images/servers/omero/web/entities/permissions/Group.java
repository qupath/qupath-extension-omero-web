package qupath.lib.images.servers.omero.web.entities.permissions;

import com.google.gson.annotations.SerializedName;
import qupath.lib.images.servers.omero.gui.UiUtilities;

import java.util.List;
import java.util.ResourceBundle;

/**
 * An OMERO group represents a set of persons that own OMERO entities.
 */
public class Group {

    private static final ResourceBundle resources = UiUtilities.getResources();
    private static final Group ALL_GROUPS = new Group(-1, resources.getString("Web.Entities.Permissions.Group.allGroups"), "");
    @SerializedName(value = "@id", alternate={"groupId"}) private final int id;
    @SerializedName(value = "Name", alternate={"groupName"}) private final String name;
    @SerializedName(value = "url:experimenters") private final String experimentersLink;
    private List<Owner> owners;

    private Group(int id, String name, String experimentersLink) {
        this.id = id;
        this.name = name;
        this.experimentersLink = experimentersLink;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof Group))
            return false;
        return ((Group)obj).id == this.id;
    }

    /**
     * @return a special group that represents all groups
     */
    public static Group getAllGroupsGroup() {
        return ALL_GROUPS;
    }

    /**
     * @return the ID of the group, or 0 if not found
     */
    public int getId() {
        return id;
    }

    /**
     * @return the name of the group, or an empty String if not found
     */
    public String getName() {
        return name == null ? "" : name;
    }

    /**
     * @return the link to get all experimenters of this group, or an empty String if not found
     */
    public String getExperimentersLink() {
        return experimentersLink == null ? "" : experimentersLink;
    }

    /**
     * @return the owners belonging to this group
     */
    public List<Owner> getOwners() {
        return owners == null ? List.of() : owners;
    }

    /**
     * Set the owners belonging to this group
     *
     * @param owners  the owners of this group
     */
    public void setOwners(List<Owner> owners) {
        this.owners = List.copyOf(owners);
    }
}
