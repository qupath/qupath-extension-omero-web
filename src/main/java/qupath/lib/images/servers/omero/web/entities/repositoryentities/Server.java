package qupath.lib.images.servers.omero.web.entities.repositoryentities;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.StringConverter;
import qupath.lib.images.servers.omero.web.apis.ApisHandler;
import qupath.lib.images.servers.omero.gui.UiUtilities;
import qupath.lib.images.servers.omero.web.entities.permissions.Group;
import qupath.lib.images.servers.omero.web.entities.permissions.Owner;
import qupath.lib.images.servers.omero.web.entities.repositoryentities.serverentities.ServerEntity;

import java.util.List;
import java.util.ResourceBundle;

/**
 * A server is the top element in the OMERO entity hierarchy.
 * It contains one {@link OrphanedFolder}, and zero or more projects and orphaned datasets (described in
 * {@link qupath.lib.images.servers.omero.web.entities.repositoryentities.serverentities server entities}).
 */
public class Server extends RepositoryEntity {

    private static final ResourceBundle resources = UiUtilities.getResources();
    private final ObservableList<Owner> owners = FXCollections.observableArrayList(Owner.getAllMembersOwner());
    private final ObservableList<Owner> ownersImmutable = FXCollections.unmodifiableObservableList(owners);
    private final ObjectProperty<Owner> defaultUser = new SimpleObjectProperty<>(owners.get(0));
    private final ObservableList<Group> groups = FXCollections.observableArrayList(Group.getAllGroupsGroup());
    private final ObservableList<Group> groupsImmutable = FXCollections.unmodifiableObservableList(groups);
    private final ObjectProperty<Group> defaultGroup = new SimpleObjectProperty<>(groups.get(0));
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
    private int defaultUserId;

    @Override
    public String toString() {
        return String.format("Server containing %s", children);
    }

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
        return resources.getString("Web.Entities.RepositoryEntities.Server.server");
    }

    @Override
    public boolean isFilteredByGroupOwnerName(Group groupFilter, Owner ownerFilter, String nameFilter) {
        return true;
    }

    /**
     * Initializes the server. This creates the orphaned folder and
     * populates the children (projects and orphaned datasets) of the server,
     * so this function must be called after that the connection to the server
     * is established.
     *
     * @param apisHandler  the request handler of the browser
     */
    public void initialize(ApisHandler apisHandler) {
        children.add(new OrphanedFolder(apisHandler));

        populate(apisHandler);
    }

    /**
     * Set the default group of this server. This is usually the group
     * of the connected user.
     *
     * @param group  the new default group
     */
    public synchronized void setDefaultGroup(Group group) {
        defaultGroup.set(group);

        if (!groups.contains(group)) {
            groups.add(group);
        }
    }

    /**
     * <p>Get the default group of this server. This is usually the group of the connected user.</p>
     * <p>This property may be updated from any thread.</p>
     *
     * @return the default group of this server
     */
    public ReadOnlyObjectProperty<Group> getDefaultGroup() {
        return defaultGroup;
    }

    /**
     * Set the ID of the default owner of this server. This is usually the connected user.
     *
     * @param userId  the ID of the default owner of this server
     */
    public synchronized void setDefaultUserId(int userId) {
        defaultUserId = userId;
        updateDefaultUser();
    }

    /**
     * <p>Get the default user of this server. This is usually the connected user.</p>
     * <p>This property may be updated from any thread.</p>
     *
     * @return the default user of this server
     */
    public ReadOnlyObjectProperty<Owner> getDefaultUser() {
        return defaultUser;
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

    private void populate(ApisHandler apisHandler) {
        apisHandler.getProjects().thenCompose(projects -> {
            addChildren(projects);

            return apisHandler.getOrphanedDatasets();
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
        updateDefaultUser();
    }

    private synchronized void updateDefaultUser() {
        owners.stream()
                .filter(owner -> owner.getId() == defaultUserId)
                .findAny()
                .ifPresent(defaultUser::set);
    }
}
