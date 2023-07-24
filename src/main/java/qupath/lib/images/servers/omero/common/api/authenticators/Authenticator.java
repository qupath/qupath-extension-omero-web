package qupath.lib.images.servers.omero.common.api.authenticators;

import qupath.lib.images.servers.omero.common.api.authenticators.commandline.CommandLineAuthenticator;
import qupath.lib.images.servers.omero.common.api.authenticators.gui.GuiAuthenticator;
import qupath.lib.images.servers.omero.common.gui.UiUtilities;

import java.net.PasswordAuthentication;
import java.util.ResourceBundle;

/**
 * <p>Utility class that uses a {@link java.net.Authenticator Authenticator} to ask the user for credentials.</p>
 * <p>If the GUI is used, this is a window, otherwise the command line is used.</p>
 */
public class Authenticator {
    private final static ResourceBundle resources = UiUtilities.getResources();

    /**
     * <p>Returns authentication information given by the user to authenticate to the URL.</p>
     * <p>
     *     If the GUI is used and this function is called from a different thread than the UI thread,
     *     make sure that the UI thread is not blocked. Otherwise, the whole application will stop responding.
     * </p>
     *
     * @param url  the URL used to authenticate
     * @return authentication information given by the user
     */
    public static PasswordAuthentication getPasswordAuthentication(String url) {
        var authenticator = UiUtilities.usingGUI() ? new GuiAuthenticator() : new CommandLineAuthenticator();

        return authenticator.requestPasswordAuthenticationInstance(
                url,
                null,
                0,
                null,
                resources.getString("Common.Api.Authenticator.enterLoginDetails"),
                null,
                null,
                null
        );
    }
}
