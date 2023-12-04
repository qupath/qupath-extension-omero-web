package qupath.ext.omero.gui.browser.serverbrowser.hierarchy;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.TreeItem;
import qupath.ext.omero.core.entities.permissions.Group;
import qupath.ext.omero.core.entities.permissions.Owner;
import qupath.ext.omero.core.entities.repositoryentities.RepositoryEntity;

import java.util.function.Predicate;

/**
 * <p>
 *     Item of the hierarchy of a {@link javafx.scene.control.TreeView TreeView} containing
 *     {@link RepositoryEntity RepositoryEntity} elements .</p>
 * <p>
 *     The items can be filtered by {@link Owner owner},
 *     {@link Group group}, and name.
 * </p>
 * <p>When an item is expanded, a web request is started to retrieve its children (if they don't already exist).</p>
 */
public class HierarchyItem extends TreeItem<RepositoryEntity> {

    private final ObservableList<TreeItem<RepositoryEntity>> children = FXCollections.observableArrayList();
    private final FilteredList<TreeItem<RepositoryEntity>> filteredChildren = new FilteredList<>(children);
    private boolean computed = false;

    /**
     * Creates a hierarchy item.
     *
     * @param repositoryEntity  the OMERO entity that will be displayed by this item
     * @param ownerBinding  the children of this item won't be shown if they are not owned by this owner
     * @param groupBinding  the children of this item won't be shown if they are not owned by this group
     * @param nameBinding  the children of this item won't be shown if they don't match this name
     */
    public HierarchyItem(
            RepositoryEntity repositoryEntity,
            ObservableValue<? extends Owner> ownerBinding,
            ObservableValue<? extends Group> groupBinding,
            ObservableValue<? extends String> nameBinding
    ) {
        super(repositoryEntity);

        Bindings.bindContent(getChildren(), filteredChildren);

        expandedProperty().addListener((p, o, n) -> {
            if (n && !computed) {
                computed = true;

                children.setAll(getValue().getChildren().stream().map(object -> new HierarchyItem(object, ownerBinding, groupBinding, nameBinding)).toList());
                getValue().getChildren().addListener((ListChangeListener<? super RepositoryEntity>) change -> Platform.runLater(() ->
                        children.setAll(change.getList().stream().map(object -> new HierarchyItem(object, ownerBinding, groupBinding, nameBinding)).toList())
                ));

                filteredChildren.predicateProperty().bind(Bindings.createObjectBinding(() ->
                                (Predicate<TreeItem<RepositoryEntity>>) item ->
                                        item.getValue().isFilteredByGroupOwnerName(groupBinding.getValue(), ownerBinding.getValue(), nameBinding.getValue()),
                        groupBinding, ownerBinding, nameBinding)
                );
            }
        });
    }

    @Override
    public boolean isLeaf() {
        return !getValue().hasChildren();
    }
}
