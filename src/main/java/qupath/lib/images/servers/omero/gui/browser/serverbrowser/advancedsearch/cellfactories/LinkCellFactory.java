package qupath.lib.images.servers.omero.gui.browser.serverbrowser.advancedsearch.cellfactories;

import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.images.servers.omero.web.entities.search.SearchResult;
import qupath.lib.images.servers.omero.gui.UiUtilities;

import java.util.ResourceBundle;

/**
 * Cell factory that displays a button that opens the link of a search result in a browser.
 */
public class LinkCellFactory extends TableCell<SearchResult, SearchResult> {

    private static final ResourceBundle resources = UiUtilities.getResources();
    private final Button button;

    public LinkCellFactory() {
        button = new Button(resources.getString("Browser.Browser.AdvancedSearch.link"));
    }

    @Override
    protected void updateItem(SearchResult item, boolean empty) {
        super.updateItem(item, empty);

        setGraphic(null);

        if (item != null && !empty) {
            button.setOnAction(e -> QuPathGUI.openInBrowser(item.getLink()));
            setGraphic(button);
        }
    }
}
