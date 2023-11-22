package qupath.ext.omero.core.entities.repositoryentities;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.omero.core.apis.ApisHandler;
import qupath.ext.omero.gui.UiUtilities;
import qupath.ext.omero.core.entities.permissions.Group;
import qupath.ext.omero.core.entities.permissions.Owner;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * A server is the top element in the OMERO entity hierarchy.
 * It contains one {@link OrphanedFolder}, and zero or more projects and orphaned datasets (described in
 * {@link qupath.ext.omero.core.entities.repositoryentities.serverentities server entities}).
 */
public class Server implements RepositoryEntity {

    private static final Logger logger = LoggerFactory.getLogger(Server.class);
    private static final ResourceBundle resources = UiUtilities.getResources();
    private final ObservableList<RepositoryEntity> children = FXCollections.observableArrayList();
    private final ObservableList<RepositoryEntity> childrenImmutable = FXCollections.unmodifiableObservableList(children);
    private final List<Owner> owners = new ArrayList<>();
    private final List<Group> groups = new ArrayList<>();
    private Owner defaultOwner = null;
    private Group defaultGroup = null;
    private boolean isPopulating = false;

    private Server() {
        owners.add(Owner.getAllMembersOwner());
        groups.add(Group.getAllGroupsGroup());
    }

    @Override
    public String toString() {
        return String.format("Server containing the following children: %s", children);
    }

    @Override
    public int getNumberOfChildren() {
        return children.size();
    }

    @Override
    public ObservableList<? extends RepositoryEntity> getChildren() {
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

    @Override
    public boolean isPopulatingChildren() {
        return isPopulating;
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
        Server server = new Server();

        return apisHandler.getGroups().thenCompose(groups -> {
            server.groups.addAll(groups);

            if (groups.isEmpty() || (defaultGroup != null && !groups.contains(defaultGroup))) {
                if (groups.isEmpty()) {
                    logger.error("The server didn't return any group");
                } else {
                    logger.error("The default group was not found in the list returned by the server");
                }
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
                        .filter(owner -> owner.id() == defaultUserId)
                        .findAny()
                        .orElse(null);

                serverCreated = serverCreated && defaultOwner != null;
            }

            if (serverCreated) {
                server.populate(apisHandler);
                server.defaultOwner = defaultOwner;
                return Optional.of(server);
            } else {
                if (owners.isEmpty()) {
                    logger.error("The server didn't return any owner");
                } else {
                    logger.error("The provided owner was not found in the list returned by the server");
                }
                return Optional.empty();
            }
        });
    }

    /**
     * <p>Get the default group of this server. This is usually the group of the connected user.</p>
     *
     * @return the default group of this server, or an empty Optional if no default group was set
     * (usually when the user is not authenticated)
     */
    public Optional<Group> getDefaultGroup() {
        return Optional.ofNullable(defaultGroup);
    }

    /**
     * <p>Get the default owner of this server. This is usually the connected user.</p>
     *
     * @return the default owner of this server, or an empty Optional if no default owner was set
     * (usually when the user is not authenticated)
     */
    public Optional<Owner> getDefaultOwner() {
        return Optional.ofNullable(defaultOwner);
    }

    /**
     * @return an unmodifiable list of groups of this server. This includes
     * the default group
     */
    public List<Group> getGroups() {
        return Collections.unmodifiableList(groups);
    }

    /**
     * @return an unmodifiable list of owners of this server. This includes
     * the default owner
     */
    public List<Owner> getOwners() {
        return Collections.unmodifiableList(owners);
    }

    private void populate(ApisHandler apisHandler) {
        isPopulating = true;

        apisHandler.getProjects().thenCompose(projects -> {
            children.addAll(projects);

            return apisHandler.getOrphanedDatasets();
        }).thenCompose(orphanedDatasets -> {
            children.addAll(orphanedDatasets);

            return OrphanedFolder.create(apisHandler);
        }).thenAccept(orphanedFolder -> {
            children.add(orphanedFolder);

            isPopulating = false;
        });
    }
}
