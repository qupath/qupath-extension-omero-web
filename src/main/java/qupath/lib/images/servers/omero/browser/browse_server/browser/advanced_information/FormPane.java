package qupath.lib.images.servers.omero.browser.browse_server.browser.advanced_information;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import qupath.lib.gui.tools.PaneTools;
import qupath.lib.images.servers.omero.common.gui.UiUtilities;

/**
 * Pane showing a two-column table. It is suited to display key-value pairs.
 */
class FormPane extends TitledPane {
    @FXML
    private GridPane content;

    /**
     * Creates a new FormPane.
     *
     * @param title  the title the pane should have
     */
    public FormPane(String title) {
        UiUtilities.loadFXMLAndGetResources(this, getClass().getResource("form_pane.fxml"));

        setText(title);
    }

    /**
     * Add a row to the table.
     *
     * @param key  the text that should appear in the first column
     * @param value  the text that should appear in the second column
     * @param tooltip  the text that should appear when the user hovers over this row
     */
    public void addRow(String key, String value, String tooltip) {
        if (content.getRowCount() > 0) {
            content.add(new Separator(), 0, content.getRowCount(), content.getColumnCount(), 1);
        }

        PaneTools.addGridRow(content, content.getRowCount(), 0, tooltip, new Label(key), UiUtilities.createSelectableLabel(value));
    }

    /**
     * Add a row to the table.
     *
     * @param key  the text that should appear in the first column
     * @param value  the text that should appear in the second column
     */
    public void addRow(String key, String value) {
        addRow(key, value, key);
    }
}
