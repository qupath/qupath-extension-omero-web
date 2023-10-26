package qupath.ext.omero.gui.browser.serverbrowser;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.omero.gui.browser.serverbrowser.hierarchy.HierarchyCellFactory;
import qupath.ext.omero.gui.browser.serverbrowser.hierarchy.HierarchyItem;
import qupath.lib.gui.QuPathGUI;
import qupath.fx.dialogs.Dialogs;
import qupath.ext.omero.gui.browser.serverbrowser.advancedsearch.AdvancedSearch;
import qupath.ext.omero.core.WebClient;
import qupath.ext.omero.gui.browser.serverbrowser.advancedinformation.AdvancedInformation;
import qupath.ext.omero.core.entities.permissions.Group;
import qupath.ext.omero.core.entities.permissions.Owner;
import qupath.ext.omero.core.entities.repositoryentities.RepositoryEntity;
import qupath.ext.omero.core.entities.repositoryentities.serverentities.Dataset;
import qupath.ext.omero.core.entities.repositoryentities.serverentities.Project;
import qupath.ext.omero.core.entities.repositoryentities.serverentities.ServerEntity;
import qupath.ext.omero.core.entities.repositoryentities.serverentities.image.Image;
import qupath.ext.omero.core.entities.repositoryentities.OrphanedFolder;
import qupath.ext.omero.gui.UiUtilities;
import qupath.ext.omero.core.pixelapis.PixelAPI;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * <p>
 *     Window allowing the user to browse an OMERO server,
 *     get information about OMERO entities and open OMERO images.
 * </p>
 * <p>
 *     It displays a hierarchy of OMERO entities using classes of
 *     {@link qupath.ext.omero.gui.browser.serverbrowser.hierarchy hierarchy}.
 * </p>
 * <p>
 *     It can launch a window showing details on an OMERO entity, described in
 *     {@link qupath.ext.omero.gui.browser.serverbrowser.advancedinformation advanced_information}.
 * </p>
 * <p>
 *     It can launch a window that performs a search on OMERO entities, described in
 *     {@link qupath.ext.omero.gui.browser.serverbrowser.advancedsearch advanced_search}.
 * </p>
 * <p>
 *     It uses a {@link BrowserModel} to update its state.
 * </p>
 */
public class Browser extends Stage {

    private static final Logger logger = LoggerFactory.getLogger(Browser.class);
    private static final float DESCRIPTION_ATTRIBUTE_PROPORTION = 0.25f;
    private static final ResourceBundle resources = UiUtilities.getResources();
    private final WebClient client;
    private final BrowserModel browserModel;
    @FXML
    private Label serverHost;
    @FXML
    private Label username;
    @FXML
    private Label numberOpenImages;
    @FXML
    private Label rawPixelAccess;
    @FXML
    private ComboBox<PixelAPI> pixelAPI;
    @FXML
    private Label loadingObjects;
    @FXML
    private Label loadingOrphaned;
    @FXML
    private Label loadingThumbnail;
    @FXML
    private MenuButton groupOwner;
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
     * @throws IOException if an error occurs while creating the browser
     */
    public Browser(WebClient client) throws IOException {
        this.client = client;
        this.browserModel = new BrowserModel(client);

        UiUtilities.loadFXML(this, Browser.class.getResource("browser.fxml"));

        initUI();
        setUpListeners();
    }

    @FXML
    private void onImagesTreeClicked(MouseEvent event) {
        if (event.getClickCount() == 2) {
            var selectedItem = hierarchy.getSelectionModel().getSelectedItem();
            RepositoryEntity selectedObject = selectedItem == null ? null : selectedItem.getValue();

            if (selectedObject instanceof Image image && image.isSupported().get()) {
                UiUtilities.openImages(client.getApisHandler().getItemURI(image));
            }
        }
    }

    @FXML
    private void onMoreInformationMenuClicked(ActionEvent ignoredEvent) {
        var selectedItem = hierarchy.getSelectionModel().getSelectedItem();
        if (selectedItem != null && selectedItem.getValue() instanceof ServerEntity serverEntity) {
            client.getApisHandler().getAnnotations(serverEntity).thenAccept(annotations -> Platform.runLater(() -> {
                if (annotations.isPresent()) {
                    try {
                        new AdvancedInformation(this, serverEntity, annotations.get());
                    } catch (IOException e) {
                        logger.error("Error while creating the advanced information window", e);
                    }
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
            QuPathGUI.openInBrowser(client.getApisHandler().getItemURI(serverEntity));
        }
    }

    @FXML
    private void onCopyToClipboardMenuClicked(ActionEvent ignoredEvent) {
        List<String> URIs = hierarchy.getSelectionModel().getSelectedItems().stream()
                .map(item -> {
                    if (item.getValue() instanceof ServerEntity serverEntity) {
                        return client.getApisHandler().getItemURI(serverEntity);
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
        try {
            new AdvancedSearch(this, client);
        } catch (IOException e) {
            logger.error("Error while creating the advanced search window", e);
        }
    }

    @FXML
    private void onImportButtonClicked(ActionEvent ignoredEvent) {
        UiUtilities.openImages(
                hierarchy.getSelectionModel().getSelectedItems().stream()
                        .map(TreeItem::getValue)
                        .map(repositoryEntity -> {
                            if (repositoryEntity instanceof ServerEntity serverEntity) {
                                return serverEntity;
                            } else {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .map(repositoryEntity -> client.getApisHandler().getItemURI(repositoryEntity))
                        .toArray(String[]::new)
        );
    }

    private void initUI() {
        serverHost.setText(client.getServerURI().getHost());

        if (client.getSelectedPixelAPI().get().canAccessRawPixels()) {
            rawPixelAccess.setText(resources.getString("Browser.Browser.accessRawPixels"));
            rawPixelAccess.setGraphic(UiUtilities.createStateNode(true));
        } else {
            rawPixelAccess.setText(resources.getString("Browser.Browser.noAccessRawPixels"));
            rawPixelAccess.setGraphic(UiUtilities.createStateNode(false));
        }

        pixelAPI.getItems().addAll(client.getAvailablePixelAPIs());
        pixelAPI.setConverter(new StringConverter<>() {
            @Override
            public String toString(PixelAPI pixelAPI) {
                return pixelAPI.getName();
            }
            @Override
            public PixelAPI fromString(String string) {
                return null;
            }
        });
        pixelAPI.getSelectionModel().select(client.getSelectedPixelAPI().get());

        groupOwner.getItems().addAll(client.getServer().getGroups().stream()
                .map(group -> {
                    List<Owner> owners = group.equals(Group.getAllGroupsGroup()) ?
                            client.getServer().getOwners() :
                            group.getOwners();

                    if (!owners.isEmpty()) {
                        Menu menu = new Menu(group.getName());
                        menu.getItems().addAll(
                                owners.stream()
                                        .map(owner -> {
                                            MenuItem ownerItem = new MenuItem(owner.getFullName());
                                            ownerItem.setOnAction(ignoredEvent -> {
                                                browserModel.getSelectedGroup().set(group);
                                                browserModel.getSelectedOwner().set(owner);
                                            });
                                            return ownerItem;
                                        })
                                        .toList()
                        );
                        return menu;
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList()
        );

        hierarchy.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        hierarchy.setRoot(new HierarchyItem(
                client.getServer(),
                browserModel.getSelectedOwner(),
                browserModel.getSelectedGroup(),
                nameFilter.textProperty()
        ));
        hierarchy.setCellFactory(n -> new HierarchyCellFactory(client, browserModel));

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
        username.textProperty().bind(Bindings.when(browserModel.getAuthenticated())
                .then(browserModel.getUsername())
                .otherwise("public")
        );

        numberOpenImages.textProperty().bind(Bindings.size(browserModel.getOpenedImagesURIs()).asString());

        client.getSelectedPixelAPI().addListener(change -> {
            if (client.getSelectedPixelAPI().get().canAccessRawPixels()) {
                rawPixelAccess.setText(resources.getString("Browser.Browser.accessRawPixels"));
                rawPixelAccess.setGraphic(UiUtilities.createStateNode(true));
            } else {
                rawPixelAccess.setText(resources.getString("Browser.Browser.noAccessRawPixels"));
                rawPixelAccess.setGraphic(UiUtilities.createStateNode(false));
            }
        });

        client.getSelectedPixelAPI().bind(pixelAPI.valueProperty());

        loadingObjects.visibleProperty().bind(Bindings.notEqual(browserModel.getNumberOfEntitiesLoading(), 0));

        loadingOrphaned.textProperty().bind(Bindings.concat(
                resources.getString("Browser.Browser.loadingOrphanedImages"),
                " (",
                browserModel.getNumberOfOrphanedImagesLoaded(),
                "/",
                browserModel.getNumberOfOrphanedImages(),
                ")"
        ));
        loadingOrphaned.visibleProperty().bind(browserModel.getOrphanedImagesLoading());

        loadingThumbnail.visibleProperty().bind(Bindings.notEqual(browserModel.getNumberOfThumbnailsLoading(), 0));

        groupOwner.textProperty().bind(Bindings.createStringBinding(
                () -> String.format("%s     %s", browserModel.getSelectedGroup().get().getName(), browserModel.getSelectedOwner().get().getFullName()),
                browserModel.getSelectedGroup(), browserModel.getSelectedOwner()
        ));

        hierarchy.getSelectionModel().selectedItemProperty().addListener((v, o, n) -> {
            updateCanvas();
            updateDescription();
            updateImportButton();
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

        client.getSelectedPixelAPI().addListener(change -> updateImportButton());
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

    private void updateImportButton() {
        var importableEntities = hierarchy.getSelectionModel().getSelectedItems().stream()
                .map(TreeItem::getValue)
                .filter(repositoryEntity -> {
                    if (repositoryEntity instanceof Image image) {
                        return client.getSelectedPixelAPI().get().canReadImage(image.isUint8(), image.has3Channels());
                    } else {
                        return repositoryEntity instanceof Dataset || repositoryEntity instanceof Project;
                    }
                })
                .toList();

        importImage.setDisable(importableEntities.isEmpty());

        if (importableEntities.isEmpty()) {
            importImage.setText(resources.getString("Browser.Browser.cantImportSelectedToQuPath"));
        } else if (importableEntities.size() == 1) {
            if (importableEntities.get(0) instanceof Image) {
                importImage.setText(resources.getString("Browser.Browser.importImageToQuPath"));
            } else if (importableEntities.get(0) instanceof Dataset) {
                importImage.setText(resources.getString("Browser.Browser.importDatasetToQuPath"));
            } else if (importableEntities.get(0) instanceof Project) {
                importImage.setText(resources.getString("Browser.Browser.importProjectToQuPath"));
            }
        } else {
            importImage.setText(resources.getString("Browser.Browser.importSelectedToQuPath"));
        }
    }
}
