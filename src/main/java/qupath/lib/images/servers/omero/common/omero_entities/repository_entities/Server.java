package qupath.lib.images.servers.omero.common.omero_entities.repository_entities;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.StringConverter;
import qupath.lib.images.servers.omero.common.api.requests.RequestsHandler;
import qupath.lib.images.servers.omero.common.gui.UiUtilities;
import qupath.lib.images.servers.omero.common.omero_entities.permissions.Group;
import qupath.lib.images.servers.omero.common.omero_entities.permissions.Owner;
import qupath.lib.images.servers.omero.common.omero_entities.repository_entities.server_entities.ServerEntity;

import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A server is the top element in the OMERO entity hierarchy.
 * It contains one {@link qupath.lib.images.servers.omero.common.omero_entities.repository_entities.OrphanedFolder OrphanedFolder}
 * and zero or more projects (described in
 * {@link qupath.lib.images.servers.omero.common.omero_entities.repository_entities.server_entities server_entities}).
 */
public class Server extends RepositoryEntity {
    private static final ResourceBundle resources = UiUtilities.getResources();
    private OrphanedFolder orphanedFolder;
    private final ObservableList<Owner> owners = FXCollections.observableArrayList(Owner.getAllMembersOwner());
    private final ObservableList<Owner> ownersImmutable = FXCollections.unmodifiableObservableList(owners);
    private final ObservableList<Group> groups = FXCollections.observableArrayList(Group.getAllGroupsGroup());
    private final ObservableList<Group> groupsImmutable = FXCollections.unmodifiableObservableList(groups);
    private final StringConverter<Owner> ownerStringConverter = new StringConverter<>() {
        @Override
        public String toString(Owner owner) {
            return owner == null ? null : owner.getName();
        }

        @Override
        public Owner fromString(String string) {
            return owners.stream()
                    .filter(owner -> owner.getName().equals(string))
                    .findAny()
                    .orElse(null);
        }
    };
    private int numberOfChildren = 1;       // the server has at least an orphaned folder as a child
    private Group defaultGroup = null;
    private int defaultUserId = -1;

    @Override
    public int getNumberOfChildren() {
        return numberOfChildren;
    }

    @Override
    public ObservableList<RepositoryEntity> getChildren() {
        return childrenImmutable;
    }

    @Override
    public String getName() {
        return resources.getString("Common.OmeroEntities.Server.server");
    }

    @Override
    public boolean isFilteredByGroupOwnerName(Group groupFilter, Owner ownerFilter, String nameFilter) {
        return true;
    }

    /**
     * Initializes the server. This creates the orphaned folder and
     * populates the children (projects) of the server.
     *
     * @param requestsHandler  the request handler of the browser
     */
    public void initialize(RequestsHandler requestsHandler) {
        orphanedFolder = new OrphanedFolder(requestsHandler);

        populate(requestsHandler);
    }

    /**
     * Set the default group of this server. This is usually the group
     * of the connected user.
     *
     * @param group  the new default group
     */
    public void setDefaultGroup(Group group) {
        defaultGroup = group;

        if (!groups.contains(group)) {
            groups.add(group);
        }
    }

    /**
     * @return the default group of this server. This is usually the group
     * of the connected user.
     */
    public Group getDefaultGroup() {
        return defaultGroup == null ? Group.getAllGroupsGroup() : defaultGroup;
    }

    /**
     * Set the ID of the default owner of this server. This is usually the connected user.
     *
     * @param userId  the ID of the default owner of this server
     */
    public void setDefaultOwnerId(int userId) {
        this.defaultUserId = userId;
    }

    /**
     * @return the default user of this server. This is usually the connected user
     */
    public Owner getDefaultUser() {
        return owners.stream()
                .filter(owner -> owner.getId() == defaultUserId)
                .findAny()
                .orElse(Owner.getAllMembersOwner());
    }

    /**
     * @return a converter to switch from/to owner to/from string
     */
    public StringConverter<Owner> getOwnerStringConverter() {
        return ownerStringConverter;
    }

    /**
     * Returns a list of owners of this server.
     * It is populated by looking at the owners of all projects.
     *
     * @return an unmodifiable list of owners of this server
     */
    public ObservableList<Owner> getOwners() {
        return ownersImmutable;
    }

    /**
     * Returns a list of groups of this server.
     * It is populated by looking at the groups of all projects.
     *
     * @return an unmodifiable list of groups of this server
     */
    public ObservableList<Group> getGroups() {
        return groupsImmutable;
    }

    private void populate(RequestsHandler requestsHandler) {
        requestsHandler.getProjects().thenAccept(children -> Platform.runLater(() -> {
            this.children.addAll(children);
            this.children.add(orphanedFolder);

            numberOfChildren = children.size() + 1;

            groups.addAll(children.stream()
                    .map(ServerEntity::getGroup)
                    .filter(distinctByName(Group::getName))
                    .filter(group -> !groups.contains(group))
                    .toList());

            owners.addAll(children.stream()
                    .map(ServerEntity::getOwner)
                    .filter(distinctByName(Owner::getName))
                    .filter(owner -> !owners.contains(owner))
                    .toList());
        }));
    }

    /**
     * See {@link "<a href="https://stackoverflow.com/questions/23699371/java-8-distinct-by-property">...</a>"}
     */
    private <T> Predicate<T> distinctByName(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }
}
