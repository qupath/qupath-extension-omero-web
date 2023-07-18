package qupath.lib.images.servers.omero.browser.browse_server.browser.advanced_information;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import qupath.lib.gui.tools.PaneTools;
import qupath.lib.images.servers.omero.common.gui.UiUtilities;

class InformationPane extends TitledPane {
    @FXML
    private GridPane content;

    public InformationPane(String title) {
        UiUtilities.loadFXMLAndGetResources(this, getClass().getResource("information_pane.fxml"));

        setText(title);
    }

    public void addRow(String value, String tooltip) {
        PaneTools.addGridRow(content, content.getRowCount(), 0, tooltip, new Label(value));
    }

    public void addColum(Node colum) {
        content.add(colum, content.getColumnCount(), 0);
    }
}
