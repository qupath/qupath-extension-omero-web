package qupath.ext.omero.core.apis.authenticators.commandline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.omero.gui.UiUtilities;

import java.net.PasswordAuthentication;
import java.util.ResourceBundle;

/**
 * {@link java.net.Authenticator Authenticator} that reads username and password information
 * from the command line.
 */
public class CommandLineAuthenticator extends java.net.Authenticator {

    private static final Logger logger = LoggerFactory.getLogger(CommandLineAuthenticator.class);
    private static final ResourceBundle resources = UiUtilities.getResources();

    @Override
    public PasswordAuthentication getPasswordAuthentication() {
        if (System.console() == null) {
            logger.error("Can't prompt user for credentials because the console is inaccessible");
            return null;
        } else {
            System.out.println(getRequestingPrompt() + ": " + getRequestingHost());
            System.out.print(resources.getString("Web.Apis.Authenticators.username") + ": ");
            String username = System.console().readLine();
            System.out.print(resources.getString("Web.Apis.Authenticators.password") + ": ");
            char[] pass = System.console().readPassword();
            return new PasswordAuthentication(username, pass);
        }
    }
}
