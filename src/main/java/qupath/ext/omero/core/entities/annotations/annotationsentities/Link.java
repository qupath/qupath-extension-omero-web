package qupath.ext.omero.core.entities.annotations.annotationsentities;

import com.google.gson.annotations.SerializedName;
import qupath.ext.omero.core.entities.permissions.Owner;

import java.util.Optional;

/**
 * An OMERO link indicates which owner added an OMERO annotation to an OMERO entity.
 */
public class Link {

    @SerializedName(value = "owner") private Owner owner;

    public Link(Owner owner) {
        this.owner = owner;
    }

    @Override
    public String toString() {
        return String.format("Link of owner %s", owner);
    }

    /**
     * @return the owner who linked the OMERO entity, or an empty Optional if not found
     */
    public Optional<Owner> getOwner() {
        return Optional.ofNullable(owner);
    }
}
