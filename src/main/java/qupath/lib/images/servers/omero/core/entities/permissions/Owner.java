package qupath.lib.images.servers.omero.core.entities.permissions;

import com.google.gson.annotations.SerializedName;
import qupath.lib.images.servers.omero.gui.UiUtilities;

import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An OMERO owner represents a person that own OMERO entities.
 */
public class Owner implements Comparable<Owner> {

    private static final ResourceBundle resources = UiUtilities.getResources();
    private static final Owner ALL_MEMBERS = new Owner(-1, resources.getString("Web.Entities.Permissions.Owner.allMembers"), "", "", "", "", "");
    @SerializedName(value = "@id", alternate = "id") private final int id;
    @SerializedName(value = "FirstName") private final String firstName;
    @SerializedName(value = "MiddleName") private final String middleName;
    @SerializedName(value = "LastName") private final String lastName;
    @SerializedName(value = "Email") private final String emailAddress;
    @SerializedName(value = "Institution") private final String institution;
    @SerializedName(value = "UserName") private final String username;

    private Owner(int id, String firstName, String middleName, String lastName, String emailAddress, String institution, String username) {
        this.id = id;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.emailAddress = emailAddress;
        this.institution = institution;
        this.username = username;
    }

    @Override
    public String toString() {
        return Stream.of("Owner: " + getName(), emailAddress, institution, username)
                .filter(Objects::nonNull)
                .filter(str -> !str.isEmpty())
                .collect(Collectors.joining(", "));
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof Owner))
            return false;
        return ((Owner)obj).id == this.id;
    }

    @Override
    public int compareTo(Owner other) {
        int lastNameComparison = getLastName().compareToIgnoreCase(other.getLastName());
        if (lastNameComparison == 0) {
            return getFirstName().compareToIgnoreCase(other.getFirstName());
        } else {
            return lastNameComparison;
        }
    }

    /**
     * @return a special owner that represents all owners
     */
    public static Owner getAllMembersOwner() {
        return ALL_MEMBERS;
    }

    /**
     * @return the full name (first, middle and last name) of the group, or an empty String if not found
     */
    public String getName() {
        return getFirstName() + " " + (getMiddleName().isEmpty() ? "" : getMiddleName() + " ") + getLastName();
    }

    /**
     * @return the ID of the owner, or 0 if not found
     */
    public int getId() {
        return id;
    }

    private String getFirstName() {
        return firstName == null ? "" : firstName;
    }

    private String getMiddleName() {
        return middleName == null ? "" : middleName;
    }

    private String getLastName() {
        return lastName == null ? "" : lastName;
    }
}
