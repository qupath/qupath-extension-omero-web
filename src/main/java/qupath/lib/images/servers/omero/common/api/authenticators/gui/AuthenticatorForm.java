package qupath.lib.images.servers.omero.common.api.authenticators.gui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import qupath.lib.images.servers.omero.common.gui.UiUtilities;

class AuthenticatorForm extends GridPane {
    @FXML
    private Label prompt;
    @FXML
    private Label host;
    @FXML
    private TextField username;
    @FXML
    private PasswordField password;

    public AuthenticatorForm(String prompt, String host, String username) {
        UiUtilities.loadFXMLAndGetResources(this, getClass().getResource("authenticator_form.fxml"));

        if (prompt != null) {
            this.prompt.setText(prompt);
        }
        if (host != null) {
            this.host.setText(host);
        }
        if (username != null) {
            this.username.setText(username);
        }
    }

    public String getUsername() {
        return username.getText();
    }

    public char[] getPassword() {
        int passwordLength = password.getCharacters().length();
        char[] passwordContent = new char[passwordLength];

        for (int i = 0; i < passwordLength; i++) {
            passwordContent[i] = password.getCharacters().charAt(i);
        }

        return passwordContent;
    }
}
