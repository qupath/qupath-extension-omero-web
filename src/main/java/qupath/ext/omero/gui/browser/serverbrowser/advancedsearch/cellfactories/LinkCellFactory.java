package qupath.ext.omero.gui.browser.serverbrowser.advancedsearch.cellfactories;

import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import qupath.ext.omero.core.entities.search.SearchResult;
import qupath.ext.omero.gui.UiUtilities;
import qupath.lib.gui.QuPathGUI;

import java.util.ResourceBundle;

/**
 * Cell factory that displays a button that opens the link of a search result in a browser.
 */
public class LinkCellFactory extends TableCell<SearchResult, SearchResult> {

    private static final ResourceBundle resources = UiUtilities.getResources();
    private final Button button;

    public LinkCellFactory() {
        button = new Button(resources.getString("Browser.ServerBrowser.AdvancedSearch.link"));
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
