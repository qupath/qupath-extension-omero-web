package qupath.ext.omero.gui.browser.serverbrowser.advancedinformation;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import qupath.ext.omero.gui.UiUtilities;
import qupath.fx.utils.GridPaneUtils;

import java.io.IOException;

/**
 * Pane showing a table. It is suited to display data organized in lines or in columns.
 */
class InformationPane extends TitledPane {

    @FXML
    private GridPane content;

    /**
     * Creates a new InformationPane.
     *
     * @param title  the title the pane should have
     * @throws IOException if an error occurs while creating the pane
     */
    public InformationPane(String title) throws IOException {
        UiUtilities.loadFXML(this, InformationPane.class.getResource("information_pane.fxml"));

        setText(title);
    }

    /**
     * Add a row to the pane in the first column.
     *
     * @param value  the text that should appear in the row
     * @param tooltip  the text that should appear when the user hovers over this row
     */
    public void addRow(String value, String tooltip) {
        GridPaneUtils.addGridRow(content, content.getRowCount(), 0, tooltip, new Label(value));
    }

    /**
     * Add a column to the pane in the first row.
     *
     * @param colum  the content of the column
     */
    public void addColum(Node colum) {
        content.add(colum, content.getColumnCount(), 0);
    }
}
