package qupath.lib.images.servers.omero.common.omeroentities.repositoryentities;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.StringConverter;
import qupath.lib.images.servers.omero.common.api.requests.RequestsHandler;
import qupath.lib.images.servers.omero.common.gui.UiUtilities;
import qupath.lib.images.servers.omero.common.omeroentities.permissions.Group;
import qupath.lib.images.servers.omero.common.omeroentities.permissions.Owner;
import qupath.lib.images.servers.omero.common.omeroentities.repositoryentities.serverentities.ServerEntity;

import java.util.List;
import java.util.ResourceBundle;

/**
 * A server is the top element in the OMERO entity hierarchy.
 * It contains one {@link OrphanedFolder}, and zero or more projects and orphaned datasets (described in
 * {@link qupath.lib.images.servers.omero.common.omeroentities.repositoryentities.serverentities server_entities}).
 */
public class Server extends RepositoryEntity {
    private static final ResourceBundle resources = UiUtilities.getResources();
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
     * populates the children (projects and orphaned datasets) of the server.
     *
     * @param requestsHandler  the request handler of the browser
     */
    public void initialize(RequestsHandler requestsHandler) {
        children.add(new OrphanedFolder(requestsHandler));

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
     * <p>
     *     Returns a list of owners of this server.
     *     It is populated by looking at the owners of all projects.
     * </p>
     * <p>This list may be updated from any thread.</p>
     *
     * @return an unmodifiable list of owners of this server
     */
    public ObservableList<Owner> getOwners() {
        return ownersImmutable;
    }

    /**
     * <p>
     *     Returns a list of groups of this server.
     *     It is populated by looking at the groups of all projects.
     * </p>
     * <p>This list may be updated from any thread.</p>
     *
     * @return an unmodifiable list of groups of this server
     */
    public ObservableList<Group> getGroups() {
        return groupsImmutable;
    }

    private void populate(RequestsHandler requestsHandler) {
        requestsHandler.getProjects().thenCompose(projects -> {
            addChildren(projects);

            return requestsHandler.getOrphanedDatasets();
        }).thenAccept(this::addChildren);
    }

    private void addChildren(List<ServerEntity> serverEntities) {
        children.addAll(serverEntities);

        numberOfChildren += serverEntities.size();

        groups.addAll(serverEntities.stream()
                .map(ServerEntity::getGroup)
                .distinct()
                .filter(group -> !groups.contains(group))
                .toList());

        owners.addAll(serverEntities.stream()
                .map(ServerEntity::getOwner)
                .distinct()
                .filter(owner -> !owners.contains(owner))
                .toList());
    }
}
