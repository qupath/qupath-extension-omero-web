package qupath.lib.images.servers.omero.gui.browser.serverbrowser.advancedsearch;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import qupath.lib.images.servers.omero.web.WebClient;
import qupath.lib.images.servers.omero.web.WebUtilities;
import qupath.lib.images.servers.omero.web.entities.search.SearchQuery;
import qupath.lib.images.servers.omero.web.entities.search.SearchResult;
import qupath.lib.images.servers.omero.gui.browser.serverbrowser.advancedsearch.cellfactories.LinkCellFactory;
import qupath.lib.images.servers.omero.gui.browser.serverbrowser.advancedsearch.cellfactories.TypeCellFactory;
import qupath.lib.images.servers.omero.web.entities.permissions.Group;
import qupath.lib.images.servers.omero.web.entities.permissions.Owner;
import qupath.lib.images.servers.omero.gui.UiUtilities;
import qupath.lib.images.servers.omero.gui.browser.serverbrowser.BrowserModel;

import java.io.IOException;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * <p>Window allowing to perform a search on Omero entities.</p>
 * <p>
 *     It displays a table that uses cell factories of the
 *     {@link qupath.lib.images.servers.omero.gui.browser.serverbrowser.advancedsearch.cellfactories cell factories} package.
 * </p>
 */
public class AdvancedSearch extends Stage {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    private static final ResourceBundle resources = UiUtilities.getResources();
    private final WebClient client;
    private final BrowserModel browserModel;
    @FXML
    private TextField query;
    @FXML
    private CheckBox name;
    @FXML
    private CheckBox description;
    @FXML
    private GridPane objectsContainer;
    @FXML
    private CheckBox images;
    @FXML
    private CheckBox datasets;
    @FXML
    private CheckBox projects;
    @FXML
    private CheckBox wells;
    @FXML
    private CheckBox plates;
    @FXML
    private CheckBox screens;
    @FXML
    private ComboBox<Owner> owner;
    @FXML
    private ComboBox<Group> group;
    @FXML
    private Button search;
    @FXML
    private Button importImage;
    @FXML
    private TableView<SearchResult> results;
    @FXML
    private TableColumn<SearchResult, SearchResult> typeColumn;
    @FXML
    private TableColumn<SearchResult, String> nameColumn;
    @FXML
    private TableColumn<SearchResult, String> acquiredColumn;
    @FXML
    private TableColumn<SearchResult, String> importedColumn;
    @FXML
    private TableColumn<SearchResult, String> groupColumn;
    @FXML
    private TableColumn<SearchResult, SearchResult> linkColumn;

    /**
     * Creates the advanced search window.
     *
     * @param ownerWindow  the stage who should own this window
     * @param client  the client on which the search will be performed
     * @param browserModel  the browser model of the browser
     * @throws IOException if an error occurs while creating the window
     */
    public AdvancedSearch(Stage ownerWindow, WebClient client, BrowserModel browserModel) throws IOException {
        this.client = client;
        this.browserModel = browserModel;

        initUI(ownerWindow);
        setUpListeners();
    }

    @FXML
    private void onResetClicked(ActionEvent ignoredEvent) {
        query.setText("");

        for (Node object: objectsContainer.getChildren()) {
            ((CheckBox) object).setSelected(true);
        }

        owner.getSelectionModel().selectFirst();
        group.getSelectionModel().selectFirst();

        results.getItems().clear();
    }

    @FXML
    private void onSearchClicked(ActionEvent ignoredEvent) {
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(30, 30);
        progressIndicator.setMinSize(30, 30);

        search.setGraphic(progressIndicator);
        search.setText(null);
        results.getItems().clear();

        client.getApisHandler().getSearchResults(new SearchQuery(
                query.getText(),
                name.isSelected(),
                description.isSelected(),
                images.isSelected(),
                datasets.isSelected(),
                projects.isSelected(),
                wells.isSelected(),
                plates.isSelected(),
                screens.isSelected(),
                group.getSelectionModel().getSelectedItem(),
                owner.getSelectionModel().getSelectedItem()
        )).thenAccept(searchResults -> Platform.runLater(() -> {
            search.setGraphic(null);
            search.setText(resources.getString("Browser.Browser.AdvancedSearch.search"));

            results.getItems().setAll(searchResults);
        }));
    }

    @FXML
    private void onImportButtonClicked(ActionEvent ignoredEvent) {
        importSelectedImages();
    }

    private void initUI(Stage ownerWindow) throws IOException {
        UiUtilities.loadFXML(this, AdvancedSearch.class.getResource("advanced_search.fxml"));

        owner.setItems(FXCollections.observableList(client.getServer().getOwners()));
        owner.setConverter(client.getServer().getOwnerStringConverter());
        if (client.getServer().getDefaultOwner().isPresent()) {
            owner.getSelectionModel().select(client.getServer().getDefaultOwner().get());
        } else {
            owner.getSelectionModel().selectFirst();
        }

        group.setItems(FXCollections.observableList(client.getServer().getGroups()));
        if (client.getServer().getDefaultGroup().isPresent()) {
            group.getSelectionModel().select(client.getServer().getDefaultGroup().get());
        } else {
            group.getSelectionModel().selectFirst();
        }

        results.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        typeColumn.setCellValueFactory(n -> new ReadOnlyObjectWrapper<>(n.getValue()));
        nameColumn.setCellValueFactory(n -> new ReadOnlyStringWrapper(n.getValue().getName()));
        acquiredColumn.setCellValueFactory(n -> new ReadOnlyStringWrapper(DATE_FORMAT.format(n.getValue().getDateAcquired())));
        importedColumn.setCellValueFactory(n -> new ReadOnlyStringWrapper(DATE_FORMAT.format(n.getValue().getDateImported())));
        groupColumn.setCellValueFactory(n -> new ReadOnlyStringWrapper(n.getValue().getGroup()));
        linkColumn.setCellValueFactory(n -> new ReadOnlyObjectWrapper<>(n.getValue()));

        typeColumn.setCellFactory(n -> new TypeCellFactory(client));
        linkColumn.setCellFactory(n -> new LinkCellFactory());

        initOwner(ownerWindow);
        show();
    }

    private void setUpListeners() {
        addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                close();
            }
        });

        importImage.textProperty().bind(Bindings.createStringBinding(
                () -> results.getSelectionModel().getSelectedItems().size() == 1 ?
                        resources.getString("Browser.Browser.AdvancedSearch.import") + " " + results.getSelectionModel().getSelectedItems().get(0).getType() :
                        resources.getString("Browser.Browser.AdvancedSearch.importObjects"),
                results.getSelectionModel().getSelectedItems()
        ));
        importImage.disableProperty().bind(results.getSelectionModel().selectedItemProperty().isNull());

        results.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getClickCount() == 2) {
                importSelectedImages();
            }
        });

        typeColumn.prefWidthProperty().bind(results.widthProperty().multiply(0.16));
        nameColumn.prefWidthProperty().bind(results.widthProperty().multiply(0.16));
        acquiredColumn.prefWidthProperty().bind(results.widthProperty().multiply(0.16));
        importedColumn.prefWidthProperty().bind(results.widthProperty().multiply(0.16));
        groupColumn.prefWidthProperty().bind(results.widthProperty().multiply(0.16));
        linkColumn.prefWidthProperty().bind(results.widthProperty().multiply(0.16));
    }

    private void importSelectedImages() {
        UiUtilities.openImages(results.getSelectionModel().getSelectedItems().stream()
                .map(item -> WebUtilities.createURI(item.getLink()))
                .flatMap(Optional::stream)
                .map(URI::toString)
                .toArray(String[]::new)
        );
    }
}
