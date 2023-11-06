package qupath.ext.omero.core.entities.annotations.annotationsentities;

import com.google.gson.annotations.SerializedName;
import qupath.ext.omero.core.entities.permissions.Owner;

import java.util.Objects;
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

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof Link link))
            return false;
        return Objects.equals(link.owner, owner);
    }

    @Override
    public int hashCode() {
        return owner.hashCode();
    }

    /**
     * @return the owner who linked the OMERO entity, or an empty Optional if not found
     */
    public Optional<Owner> getOwner() {
        return Optional.ofNullable(owner);
    }
}
