package qupath.lib.images.servers.omero.browser.new_server;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import qupath.lib.images.servers.omero.common.api.clients.ClientsPreferencesManager;
import qupath.lib.images.servers.omero.common.gui.UiUtilities;

/**
 * Window that provides an input allowing the user to write a server URL.
 */
public class NewServerForm extends VBox {
    @FXML
    private TextField url;

    public NewServerForm() {
        UiUtilities.loadFXMLAndGetResources(this, getClass().getResource("new_server_form.fxml"));

        url.setText(ClientsPreferencesManager.getLastServerURI());
    }

    public String getURL() {
        return url.getText();
    }
}
