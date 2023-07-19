package qupath.lib.images.servers.omero.browser.browseserver.browser.advancedsearch.cellfactories;

import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.images.servers.omero.common.api.requests.entities.search.SearchResult;
import qupath.lib.images.servers.omero.common.gui.UiUtilities;

import java.util.ResourceBundle;

/**
 * Cell factory that displays a button that opens the link of a search result in a browser.
 */
public class LinkCellFactory extends TableCell<SearchResult, SearchResult> {
    private final static ResourceBundle resources = UiUtilities.getResources();
    private final Button button;

    public LinkCellFactory() {
        button = new Button(resources.getString("Browser.Browser.AdvancedSearch.link"));
    }

    @Override
    protected void updateItem(SearchResult item, boolean empty) {
        super.updateItem(item, empty);

        setGraphic(null);

        if (item != null && !empty) {
            button.setOnAction(e -> QuPathGUI.launchBrowserWindow(item.getLink()));
            setGraphic(button);
        }
    }
}
