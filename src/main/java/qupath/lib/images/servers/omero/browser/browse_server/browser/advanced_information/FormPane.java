package qupath.lib.images.servers.omero.browser.browse_server.browser.advanced_information;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import qupath.lib.gui.tools.PaneTools;
import qupath.lib.images.servers.omero.common.gui.UiUtilities;

class FormPane extends TitledPane {
    @FXML
    private GridPane content;

    public FormPane(String title) {
        UiUtilities.loadFXMLAndGetResources(this, getClass().getResource("form_pane.fxml"));

        setText(title);
    }

    public void addRow(String key, String value, String tooltip) {
        if (content.getRowCount() > 0) {
            content.add(new Separator(), 0, content.getRowCount(), content.getColumnCount(), 1);
        }

        PaneTools.addGridRow(content, content.getRowCount(), 0, tooltip, new Label(key), UiUtilities.createSelectableLabel(value));
    }

    public void addRow(String key, String value) {
        addRow(key, value, key);
    }
}
