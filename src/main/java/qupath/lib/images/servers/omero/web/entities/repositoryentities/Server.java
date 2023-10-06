package qupath.lib.images.servers.omero.web.entities.repositoryentities;

import javafx.collections.ObservableList;
import javafx.util.StringConverter;
import qupath.lib.images.servers.omero.web.apis.ApisHandler;
import qupath.lib.images.servers.omero.gui.UiUtilities;
import qupath.lib.images.servers.omero.web.entities.permissions.Group;
import qupath.lib.images.servers.omero.web.entities.permissions.Owner;
import qupath.lib.images.servers.omero.web.entities.repositoryentities.serverentities.ServerEntity;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * A server is the top element in the OMERO entity hierarchy.
 * It contains one {@link OrphanedFolder}, and zero or more projects and orphaned datasets (described in
 * {@link qupath.lib.images.servers.omero.web.entities.repositoryentities.serverentities server entities}).
 */
public class Server extends RepositoryEntity {

    private static final ResourceBundle resources = UiUtilities.getResources();
    private final List<Owner> owners = new ArrayList<>();
    private final List<Group> groups = new ArrayList<>();
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
    private Owner defaultOwner = null;
    private Group defaultGroup = null;
    private int numberOfChildren = 1;       // the server has at least an orphaned folder as a child

    private Server(ApisHandler apisHandler) {
        owners.add(Owner.getAllMembersOwner());
        groups.add(Group.getAllGroupsGroup());

        children.add(new OrphanedFolder(apisHandler));
    }

    @Override
    public String toString() {
        return String.format("Server containing %d children", numberOfChildren);
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
     * <p>
     *     Create the server. This creates the orphaned folder and populates the
     *     children (projects and orphaned datasets) of the server.
     * </p>
     * <p>
     *     Call {@link #create(ApisHandler, Group, int)} if you want to specify a default group
     *     and a default user.
     * </p>
     *
     * @param apisHandler  the APIs handler of the browser
     * @return the new server, or an empty Optional if the creation failed
     */
    public static CompletableFuture<Optional<Server>> create(ApisHandler apisHandler) {
        return create(apisHandler, null, -1);
    }

    /**
     * Same as {@link #create(ApisHandler)}, but by specifying a default group
     * and a default user.
     *
     * @param apisHandler  the APIs handler of the browser
     * @param defaultGroup  the default group of this server. This is usually the group
     *                      of the connected user
     * @param defaultUserId  the ID of the default owner of this server. This is usually the connected user
     * @return the new server, or an empty Optional if the creation failed
     */
    public static CompletableFuture<Optional<Server>> create(ApisHandler apisHandler, Group defaultGroup, int defaultUserId) {
        Server server = new Server(apisHandler);

        return apisHandler.getGroups().thenCompose(groups -> {
            server.groups.addAll(groups);

            if (groups.isEmpty() || (defaultGroup != null && !groups.contains(defaultGroup))) {
                return CompletableFuture.completedFuture(List.of());
            } else {
                server.defaultGroup = defaultGroup;
                return apisHandler.getOwners();
            }
        }).thenApply(owners -> {
            server.owners.addAll(owners);

            boolean serverCreated = !owners.isEmpty();
            Owner defaultOwner = null;
            if (defaultUserId > -1) {
                defaultOwner = server.owners.stream()
                        .filter(owner -> owner.getId() == defaultUserId)
                        .findAny()
                        .orElse(null);

                serverCreated = serverCreated && defaultOwner != null;
            }

            if (serverCreated) {
                server.populate(apisHandler);
                server.defaultOwner = defaultOwner;
                return Optional.of(server);
            } else {
                return Optional.empty();
            }
        });
    }

    /**
     * <p>Get the default group of this server. This is usually the group of the connected user.</p>
     *
     * @return the default group of this server, or an empty Optional if no default group was set
     */
    public Optional<Group> getDefaultGroup() {
        return Optional.ofNullable(defaultGroup);
    }

    /**
     * <p>Get the default owner of this server. This is usually the connected user.</p>
     *
     * @return the default owner of this server, or an empty Optional if no default owner was set
     */
    public Optional<Owner> getDefaultOwner() {
        return Optional.ofNullable(defaultOwner);
    }

    /**
     * @return a converter to switch from/to owner to/from string
     */
    public StringConverter<Owner> getOwnerStringConverter() {
        return ownerStringConverter;
    }

    /**
     * @return an unmodifiable list of owners of this server
     */
    public List<Owner> getOwners() {
        return Collections.unmodifiableList(owners);
    }

    /**
     * @return an unmodifiable list of groups of this server
     */
    public List<Group> getGroups() {
        return Collections.unmodifiableList(groups);
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
    }
}
