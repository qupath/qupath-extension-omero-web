package qupath.lib.images.servers.omero.core.apis.authenticators.gui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.fx.dialogs.Dialogs;
import qupath.lib.images.servers.omero.core.ClientsPreferencesManager;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.ResourceBundle;

/**
 * <p>
 *     {@link java.net.Authenticator Authenticator} that reads username and password information
 *     from a dialog window.
 * </p>
 * <p>
 *     The dialog window is described in {@link AuthenticatorForm}.
 * </p>
 * <p>
 *     Even though this class uses UI elements, it can be called from any thread.
 *     However, make sure the UI thread is not blocked when calling this class, otherwise
 *     it will block the entire application.
 * </p>
 */
public class GuiAuthenticator extends Authenticator {

    private static final Logger logger = LoggerFactory.getLogger(GuiAuthenticator.class);
    private static final ResourceBundle resources = ResourceBundle.getBundle("qupath.lib.images.servers.omero.strings");

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        try {
            AuthenticatorForm authenticatorForm = new AuthenticatorForm(getRequestingPrompt(), getRequestingHost(), ClientsPreferencesManager.getLastUsername());
            boolean loginConfirmed = Dialogs.showConfirmDialog(resources.getString("Web.Apis.Authenticator.login"), authenticatorForm);

            String username = authenticatorForm.getUsername();
            char[] password = authenticatorForm.getPassword();
            authenticatorForm.clear();

            if (loginConfirmed) {
                PasswordAuthentication authentication = new PasswordAuthentication(username, password);
                ClientsPreferencesManager.setLastUsername(authentication.getUserName());
                return authentication;
            } else {
                return null;
            }
        } catch (IOException e) {
            logger.error("Error while creating the authenticator form", e);
            return null;
        }
    }
}
