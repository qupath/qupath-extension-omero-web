package qupath.ext.omero.core.entities.permissions;

import com.google.gson.annotations.SerializedName;
import qupath.ext.omero.gui.UiUtilities;

import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An OMERO owner represents a person that own OMERO entities.
 */
public record Owner(@SerializedName(value = "@id", alternate = "id") int id,
                    @SerializedName(value = "FirstName") String firstName,
                    @SerializedName(value = "MiddleName") String middleName,
                    @SerializedName(value = "LastName") String lastName,
                    @SerializedName(value = "Email") String emailAddress,
                    @SerializedName(value = "Institution") String institution,
                    @SerializedName(value = "UserName") String username) implements Comparable<Owner> {

    private static final ResourceBundle resources = UiUtilities.getResources();
    private static final Owner ALL_MEMBERS = new Owner(-1, resources.getString("Web.Entities.Permissions.Owner.allMembers"), "", "", "", "", "");

    @Override
    public String toString() {
        return Stream.of("Owner: " + getFullName(), emailAddress, institution, username)
                .filter(Objects::nonNull)
                .filter(str -> !str.isEmpty())
                .collect(Collectors.joining(", "));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof Owner owner))
            return false;
        return owner.id == this.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public int compareTo(Owner other) {
        int lastNameComparison = lastName().compareToIgnoreCase(other.lastName());
        if (lastNameComparison == 0) {
            return firstName().compareToIgnoreCase(other.firstName());
        } else {
            return lastNameComparison;
        }
    }

    /**
     * @return the ID of the owner, or 0 if not found
     */
    @Override
    public int id() {
        return id;
    }

    /**
     * @return the first name of the owner, or an empty String if not found
     */
    @Override
    public String firstName() {
        return Objects.toString(firstName, "");
    }

    /**
     * @return the middle name of the owner, or an empty String if not found
     */
    @Override
    public String middleName() {
        return Objects.toString(middleName, "");
    }

    /**
     * @return the last name of the owner, or an empty String if not found
     */
    @Override
    public String lastName() {
        return Objects.toString(lastName, "");
    }

    /**
     * @return the email address of the owner, or an empty String if not found
     */
    @Override
    public String emailAddress() {
        return Objects.toString(emailAddress, "");
    }

    /**
     * @return the institution of the owner, or an empty String if not found
     */
    @Override
    public String institution() {
        return Objects.toString(institution, "");
    }

    /**
     * @return the username of the owner, or an empty String if not found
     */
    @Override
    public String username() {
        return Objects.toString(username, "");
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
    public String getFullName() {
        return (firstName().isEmpty() ? "" : firstName() + " ") +
                (middleName().isEmpty() ? "" : middleName() + " ") +
                lastName();
    }
}
