package qupath.lib.images.servers.omero.gui.browser.newserver;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import qupath.lib.images.servers.omero.web.ClientsPreferencesManager;
import qupath.lib.images.servers.omero.gui.UiUtilities;

import java.io.IOException;

/**
 * Window that provides an input allowing the user to write a server URL.
 */
public class NewServerForm extends VBox {

    @FXML
    private TextField url;

    /**
     * Creates the new server form.
     * @throws IOException if an error occurs while creating the form
     */
    public NewServerForm() throws IOException {
        UiUtilities.loadFXML(this, getClass().getResource("new_server_form.fxml"));

        url.setText(ClientsPreferencesManager.getLastServerURI());
    }

    public String getURL() {
        return url.getText();
    }
}
