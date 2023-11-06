package qupath.ext.omero.core.entities.repositoryentities;

import javafx.collections.ObservableList;
import qupath.ext.omero.core.entities.permissions.Group;
import qupath.ext.omero.core.entities.permissions.Owner;

/**
 * An element belonging to the OMERO entity hierarchy.
 */
public interface RepositoryEntity {

    /**
     * @return the number of children of this entity
     */
    int getNumberOfChildren();

    /**
     * <p>Returns the list of children of this element.</p>
     * <p>
     *     Usually, the initial call to this function returns an empty list but
     *     starts populating it in the background, so changes to this list should
     *     be listened. The {@link #isPopulatingChildren()} function indicates
     *     if the populating process is currently happening.
     * </p>
     * <p>This list may be updated from any thread.</p>
     *
     * @return an unmodifiable list of children of this element
     */
    ObservableList<? extends RepositoryEntity> getChildren();

    /**
     * @return a text describing the entity, or an empty String if the name was not found
     */
    String getName();

    /**
     * Indicates if this entity belongs to the provided group, the provided owner,
     * and (if the entity is a project) match the provided name.
     *
     * @param groupFilter  the group the entity should belong to
     * @param ownerFilter  the owner the entity should belong to
     * @param nameFilter  the name the entity should contain (if the entity is a project)
     * @return whether this entity matches all the filters
     */
    boolean isFilteredByGroupOwnerName(Group groupFilter, Owner ownerFilter, String nameFilter);

    /**
     * @return whether this entity is currently populating its children
     */
    boolean isPopulatingChildren();
}