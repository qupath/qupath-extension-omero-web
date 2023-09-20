package qupath.lib.images.servers.omero.web.entities.permissions;

import com.google.gson.annotations.SerializedName;
import qupath.lib.images.servers.omero.gui.UiUtilities;

import java.util.ResourceBundle;

/**
 * An OMERO group represents a set of persons that own OMERO entities.
 */
public class Group {

    private static final ResourceBundle resources = UiUtilities.getResources();
    private static final Group ALL_GROUPS = new Group(-1, resources.getString("Web.Entities.Permissions.Group.allGroups"));
    @SerializedName(value = "@id", alternate={"groupId"}) private final int id;
    @SerializedName(value = "Name", alternate={"groupName"}) private final String name;

    private Group(int id, String name) {
        this.id = id;
        this.name = name;
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
     * @return the name of the group, or an empty String if not found
     */
    public String getName() {
        return name == null ? "" : name;
    }


    /**
     * @return the ID of the group, or 0 if not found
     */
    public int getId() {
        return id;
    }
}
