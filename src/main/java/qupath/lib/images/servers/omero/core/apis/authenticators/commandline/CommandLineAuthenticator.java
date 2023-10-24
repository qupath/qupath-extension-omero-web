package qupath.lib.images.servers.omero.core.apis.authenticators.commandline;

import qupath.lib.images.servers.omero.gui.UiUtilities;

import java.net.PasswordAuthentication;
import java.util.ResourceBundle;

/**
 * {@link java.net.Authenticator Authenticator} that reads username and password information
 * from the command line.
 */
public class CommandLineAuthenticator extends java.net.Authenticator {

    private static final ResourceBundle resources = UiUtilities.getResources();

    @Override
    public PasswordAuthentication getPasswordAuthentication() {
        System.out.println(getRequestingPrompt() + ": " + getRequestingHost());
        System.out.print(resources.getString("Web.Apis.Authenticators.username") + ": ");
        String username = System.console().readLine();
        System.out.print(resources.getString("Web.Apis.Authenticators.password") + ": ");
        char[] pass = System.console().readPassword();
        return new PasswordAuthentication(username, pass);
    }
}
