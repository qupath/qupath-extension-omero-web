package qupath.lib.images.servers.omero.common.api.authenticators.command_line;

import qupath.lib.images.servers.omero.common.gui.UiUtilities;

import java.net.PasswordAuthentication;
import java.util.ResourceBundle;

/**
 * {@link java.net.Authenticator Authenticator} that reads username and password information
 * from the command line.
 */
public class CommandLineAuthenticator extends java.net.Authenticator {
    private final static ResourceBundle resources = UiUtilities.getResources();

    @Override
    public PasswordAuthentication getPasswordAuthentication() {
        System.out.println(getRequestingPrompt() + ": " + getRequestingHost());
        System.out.print(resources.getString("Common.Api.Authenticators.username") + ": ");
        String username = System.console().readLine();
        System.out.print(resources.getString("Common.Api.Authenticators.password") + ": ");
        char[] pass = System.console().readPassword();
        return new PasswordAuthentication(username, pass);
    }
}
