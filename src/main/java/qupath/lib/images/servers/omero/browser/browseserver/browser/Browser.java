package qupath.lib.images.servers.omero.browser.browseserver.browser;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.stage.Stage;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.dialogs.Dialogs;
import qupath.lib.images.servers.omero.common.api.clients.WebClient;
import qupath.lib.images.servers.omero.browser.browseserver.browser.hierarchy.HierarchyCellFactory;
import qupath.lib.images.servers.omero.common.omeroentities.permissions.Group;
import qupath.lib.images.servers.omero.common.omeroentities.permissions.Owner;
import qupath.lib.images.servers.omero.common.omeroentities.repositoryentities.RepositoryEntity;
import qupath.lib.images.servers.omero.common.omeroentities.repositoryentities.serverentities.ServerEntity;
import qupath.lib.images.servers.omero.common.omeroentities.repositoryentities.serverentities.image.Image;
import qupath.lib.images.servers.omero.common.omeroentities.repositoryentities.OrphanedFolder;
import qupath.lib.images.servers.omero.common.gui.UiUtilities;
import qupath.lib.images.servers.omero.browser.browseserver.browser.advancedinformation.AdvancedInformation;
import qupath.lib.images.servers.omero.browser.browseserver.browser.advancedsearch.AdvancedSearch;
import qupath.lib.images.servers.omero.browser.browseserver.browser.hierarchy.HierarchyItem;

import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * <p>
 *     Window allowing the user to browse an OMERO server,
 *     get information about OMERO entities and open OMERO images.
 * </p>
 * <p>
 *     It displays a hierarchy of OMERO entities using classes of
 *     {@link qupath.lib.images.servers.omero.browser.browseserver.browser.hierarchy hierarchy}.
 * </p>
 * <p>
 *     It can launch a window showing details on an OMERO entity, described in
 *     {@link qupath.lib.images.servers.omero.browser.browseserver.browser.advancedinformation advanced_information}.
 * </p>
 * <p>
 *     It can launch a window that performs a search on OMERO entities, described in
 *     {@link qupath.lib.images.servers.omero.browser.browseserver.browser.advancedsearch advanced_search}.
 * </p>
 */
public class Browser extends Stage {
    private final ResourceBundle resources;
    private static final float DESCRIPTION_ATTRIBUTE_PROPORTION = 0.25f;
    private final WebClient client;
    @FXML
    private Label serverHost;
    @FXML
    private Label username;
    @FXML
    private Label numberOpenImages;
    @FXML
    private Label loadingObjects;
    @FXML
    private Label loadingOrphaned;
    @FXML
    private Label loadingThumbnail;
    @FXML
    private ComboBox<Group> groupFilter;
    @FXML
    private ComboBox<Owner> ownerFilter;
    @FXML
    private TreeView<RepositoryEntity> hierarchy;
    @FXML
    private MenuItem moreInfo;
    @FXML
    private MenuItem openBrowser;
    @FXML
    private MenuItem copyToClipboard;
    @FXML
    private MenuItem collapseAllItems;
    @FXML
    private TextField nameFilter;
    @FXML
    private Button advanced;
    @FXML
    private Button importImage;
    @FXML
    private Canvas canvas;
    @FXML
    private TableView<Integer> description;
    @FXML
    private TableColumn<Integer, String> attributeColumn;
    @FXML
    private TableColumn<Integer, String> valueColumn;

    /**
     * Create the browser window.
     *
     * @param client  the web client which will be used by this browser to retrieve data from the corresponding OMERO server
     */
    public Browser(WebClient client) {
        this.client = client;

        resources = UiUtilities.loadFXMLAndGetResources(this, getClass().getResource("browser.fxml"));

        initUI();
        setUpListeners();
    }

    @FXML
    private void onImagesTreeClicked(MouseEvent event) {
        if (event.getClickCount() == 2) {
            var selectedItem = hierarchy.getSelectionModel().getSelectedItem();
            RepositoryEntity selectedObject = selectedItem == null ? null : selectedItem.getValue();

            if (selectedObject instanceof Image image && image.isSupported()) {
                UiUtilities.openImages(client.getRequestsHandler().getItemURI(image));
            }
        }
    }

    @FXML
    private void onMoreInformationMenuClicked(ActionEvent ignoredEvent) {
        var selectedItem = hierarchy.getSelectionModel().getSelectedItem();
        if (selectedItem != null && selectedItem.getValue() instanceof ServerEntity serverEntity) {
            client.getRequestsHandler().getAnnotations(serverEntity).thenAccept(annotations -> Platform.runLater(() -> {
                if (annotations.isPresent()) {
                    new AdvancedInformation(this, serverEntity, annotations.get());
                } else {
                    Dialogs.showErrorMessage(
                            resources.getString("Browser.Browser.cantDisplayInformation"),
                            MessageFormat.format(resources.getString("Browser.Browser.errorWhenFetchingInformation"), serverEntity.getName())
                    );
                }
            }));
        }
    }

    @FXML
    private void onOpenInBrowserMenuClicked(ActionEvent ignoredEvent) {
        var selectedItem = hierarchy.getSelectionModel().getSelectedItem();
        if (selectedItem != null && selectedItem.getValue() instanceof ServerEntity serverEntity) {
            QuPathGUI.launchBrowserWindow(client.getRequestsHandler().getItemURI(serverEntity));
        }
    }

    @FXML
    private void onCopyToClipboardMenuClicked(ActionEvent ignoredEvent) {
        List<String> URIs = hierarchy.getSelectionModel().getSelectedItems().stream()
                .map(item -> {
                    if (item.getValue() instanceof ServerEntity serverEntity) {
                        return client.getRequestsHandler().getItemURI(serverEntity);
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();

        if (URIs.size() > 0) {
            ClipboardContent content = new ClipboardContent();
            if (URIs.size() == 1) {
                content.putString(URIs.get(0));
            } else {
                content.putString("[" + String.join(", ", URIs) + "]");
            }
            Clipboard.getSystemClipboard().setContent(content);

            Dialogs.showInfoNotification(
                    resources.getString("Browser.Browser.copyURIToClipboard"),
                    resources.getString("Browser.Browser.uriSuccessfullyCopied")
            );
        } else {
            Dialogs.showWarningNotification(
                    resources.getString("Browser.Browser.copyURIToClipboard"),
                    resources.getString("Browser.Browser.itemNeedsSelected")
            );
        }
    }

    @FXML
    private void onCollapseAllItemsMenuClicked(ActionEvent ignoredEvent) {
        collapseTreeView(hierarchy.getRoot());
    }

    @FXML
    private void onAdvancedClicked(ActionEvent ignoredEvent) {
        new AdvancedSearch(this, client);
    }

    @FXML
    private void onImportButtonClicked(ActionEvent ignoredEvent) {
        UiUtilities.openImages(
                hierarchy.getSelectionModel().getSelectedItems().stream()
                        .flatMap(item -> Stream.concat(Stream.of(item.getValue()), item.getValue().getEntityAndDescendants()))
                        .map(repositoryEntity -> {
                            if (repositoryEntity instanceof Image image && image.isSupported()) {
                                return image;
                            } else {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .map(repositoryEntity -> client.getRequestsHandler().getItemURI(repositoryEntity))
                        .toArray(String[]::new)
        );
    }

    private void initUI() {
        serverHost.setText(client.getServerURI().getHost());

        ownerFilter.setItems(client.getServer().getOwners());
        ownerFilter.setConverter(client.getServer().getOwnerStringConverter());
        ownerFilter.getSelectionModel().select(client.getServer().getDefaultUser());

        groupFilter.setItems(client.getServer().getGroups());
        groupFilter.getSelectionModel().select(client.getServer().getDefaultGroup());

        hierarchy.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        hierarchy.setRoot(new HierarchyItem(
                client.getServer(),
                ownerFilter.getSelectionModel().selectedItemProperty(),
                groupFilter.getSelectionModel().selectedItemProperty(),
                nameFilter.textProperty()
        ));
        hierarchy.setCellFactory(n -> new HierarchyCellFactory(client));

        attributeColumn.setCellValueFactory(cellData -> {
            var selectedItems = hierarchy.getSelectionModel().getSelectedItems();
            if (cellData != null && selectedItems.size() == 1 && selectedItems.get(0).getValue() instanceof ServerEntity serverEntity) {
                return new ReadOnlyObjectWrapper<>(serverEntity.getAttributeInformation(cellData.getValue()));
            } else {
                return new ReadOnlyObjectWrapper<>("");
            }
        });

        valueColumn.setCellValueFactory(cellData -> {
            var selectedItems = hierarchy.getSelectionModel().getSelectedItems();
            if (cellData != null && selectedItems.size() == 1 && selectedItems.get(0).getValue() instanceof ServerEntity serverEntity) {
                return new ReadOnlyObjectWrapper<>(serverEntity.getValueInformation(cellData.getValue()));
            } else {
                return new ReadOnlyObjectWrapper<>("");
            }
        });
        valueColumn.setCellFactory(n -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item);
                    setTooltip(new Tooltip(item));
                }
            }
        });

        initOwner(QuPathGUI.getInstance().getStage());
        show();
    }

    private void setUpListeners() {
        username.textProperty().bind(Bindings.when(client.getAuthenticated())
                .then(client.getUsername())
                .otherwise("public")
        );

        numberOpenImages.textProperty().bind(Bindings.size(client.getOpenedImagesURIs()).asString());

        loadingObjects.visibleProperty().bind(Bindings.notEqual(client.getRequestsHandler().getNumberOfEntitiesLoading(), 0));

        loadingOrphaned.textProperty().bind(Bindings.concat(
                resources.getString("Browser.Browser.loadingOrphanedImages"),
                " (",
                client.getRequestsHandler().getNumberOfOrphanedImagesLoaded(),
                "/",
                client.getRequestsHandler().getNumberOfOrphanedImages(),
                ")"
        ));
        loadingOrphaned.visibleProperty().bind(client.getRequestsHandler().getOrphanedImagesLoading());

        loadingThumbnail.visibleProperty().bind(Bindings.notEqual(client.getRequestsHandler().getNumberOfThumbnailsLoading(), 0));

        ownerFilter.getItems().addListener((ListChangeListener<? super Owner>) change ->
                ownerFilter.getSelectionModel().selectFirst()
        );

        groupFilter.getItems().addListener((ListChangeListener<? super Group>) change ->
                groupFilter.getSelectionModel().selectFirst()
        );

        hierarchy.getSelectionModel().selectedItemProperty().addListener((v, o, n) -> {
            updateCanvas();
            updateDescription();
        });

        BooleanBinding isSelectedItemOrphanedFolderBinding = Bindings.createBooleanBinding(() ->
                        hierarchy.getSelectionModel().getSelectedItem() != null && hierarchy.getSelectionModel().getSelectedItem().getValue() instanceof OrphanedFolder,
                hierarchy.getSelectionModel().selectedItemProperty()
        );
        moreInfo.disableProperty().bind(isSelectedItemOrphanedFolderBinding);
        openBrowser.disableProperty().bind(isSelectedItemOrphanedFolderBinding);
        copyToClipboard.disableProperty().bind(isSelectedItemOrphanedFolderBinding);

        attributeColumn.prefWidthProperty().bind(description.widthProperty().multiply(DESCRIPTION_ATTRIBUTE_PROPORTION));
        valueColumn.prefWidthProperty().bind(description.widthProperty().multiply(1 - DESCRIPTION_ATTRIBUTE_PROPORTION));

        description.placeholderProperty().bind(Bindings.when(Bindings.isEmpty(hierarchy.getSelectionModel().getSelectedItems()))
                .then(new Label(resources.getString("Browser.Browser.noElementSelected")))
                .otherwise(new Label(resources.getString("Browser.Browser.multipleElementsSelected")))
        );

        canvas.managedProperty().bind(Bindings.createBooleanBinding(() ->
                        hierarchy.getSelectionModel().getSelectedItems().size() == 1 &&
                                hierarchy.getSelectionModel().getSelectedItems().get(0).getValue() instanceof Image,
                hierarchy.getSelectionModel().getSelectedItems()));
    }

    private static void collapseTreeView(TreeItem<RepositoryEntity> item){
        if (item != null) {
            for (var child : item.getChildren()) {
                child.setExpanded(false);
            }
        }
    }

    private void updateCanvas() {
        canvas.getGraphicsContext2D().clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        var selectedItems = hierarchy.getSelectionModel().getSelectedItems();
        if (selectedItems.size() == 1 && selectedItems.get(0) != null && selectedItems.get(0).getValue() instanceof Image image) {
            client.getThumbnail(image.getId()).thenAccept(thumbnail -> Platform.runLater(() ->
                    thumbnail.ifPresent(bufferedImage -> UiUtilities.paintBufferedImageOnCanvas(bufferedImage, canvas))
            ));
        }
    }

    private void updateDescription() {
        description.getItems().clear();

        var selectedItems = hierarchy.getSelectionModel().getSelectedItems();
        if (selectedItems.size() == 1 && selectedItems.get(0) != null && selectedItems.get(0).getValue() instanceof ServerEntity serverEntity) {
            description.getItems().setAll(
                    IntStream.rangeClosed(0, serverEntity.getNumberOfAttributes()).boxed().collect(Collectors.toList())
            );
        } else {
            description.getItems().clear();
        }
    }
}
