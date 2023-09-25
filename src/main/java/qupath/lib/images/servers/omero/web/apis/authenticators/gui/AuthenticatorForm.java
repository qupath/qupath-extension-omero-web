package qupath.lib.images.servers.omero.web.apis.authenticators.gui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import qupath.lib.images.servers.omero.gui.UiUtilities;

import java.io.IOException;

/**
 * Window prompting the user for a username and a password.
 */
class AuthenticatorForm extends GridPane {

    @FXML
    private Label prompt;
    @FXML
    private Label host;
    @FXML
    private TextField username;
    @FXML
    private PasswordField password;

    /**
     * <p>Creates the authenticator form.</p>
     * <p>All parameters can be null. If so, they won't be taken into account.</p>
     *
     * @param prompt  a message asking the user for credentials
     * @param host  the URI of the server, to remind the user
     * @param username  the username of the user, to pre-populate the username input
     * @throws IOException if an error occurs while creating the form
     */
    public AuthenticatorForm(String prompt, String host, String username) throws IOException {
        UiUtilities.loadFXML(this, AuthenticatorForm.class.getResource("authenticator_form.fxml"));

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

    /**
     * @return the username written by the user
     */
    public String getUsername() {
        return username.getText();
    }

    /**
     * <p>Returns the password written by the user.</p>
     * <p>
     *     A char array and not a string is used, for <a href="https://stackoverflow.com/a/8881376">security reasons</a>.
     *     It is the responsibility of the programmer using this function to clear the returned char array once processed.
     * </p>
     *
     * @return a char array containing the password written by the user
     */
    public char[] getPassword() {
        int passwordLength = password.getCharacters().length();
        char[] passwordContent = new char[passwordLength];

        for (int i = 0; i < passwordLength; i++) {
            passwordContent[i] = password.getCharacters().charAt(i);
        }

        return passwordContent;
    }

    /**
     * Clear all data given by the user.
     * This should be called whenever this form is not needed anymore
     * as it removes sensitive data.
     */
    public void clear() {
        username.setText("");
        password.setText("");
    }
}
