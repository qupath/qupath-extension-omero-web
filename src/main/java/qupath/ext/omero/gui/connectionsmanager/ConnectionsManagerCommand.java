package qupath.ext.omero.gui.connectionsmanager;

import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.omero.gui.UiUtilities;

import java.io.IOException;
import java.util.ResourceBundle;

/**
 * Command that starts a {@link ConnectionsManager}.
 */
public class ConnectionsManagerCommand implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(ConnectionsManagerCommand.class);
	private static final ResourceBundle resources = UiUtilities.getResources();
	private final Stage owner;
	private ConnectionsManager connectionsManager;

	/**
	 * Creates a new connection manager command.
	 *
	 * @param owner  the stage that should own the connection manager window
	 */
	public ConnectionsManagerCommand(Stage owner) {
		this.owner = owner;
	}

	@Override
	public void run() {
		if (connectionsManager == null) {
			try {
				connectionsManager = new ConnectionsManager(owner);
			} catch (IOException e) {
				logger.error("Error while creating the connection manager window", e);
			}
		} else {
			UiUtilities.showHiddenWindow(connectionsManager);
		}
	}

	/**
	 * @return the text that should appear on the menu starting this command
	 */
	public static String getMenuTitle() {
		return resources.getString("ConnectionsManager.Command.manageServerConnections");
	}
}
