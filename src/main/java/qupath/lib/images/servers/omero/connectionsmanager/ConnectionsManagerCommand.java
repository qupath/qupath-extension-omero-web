package qupath.lib.images.servers.omero.connectionsmanager;

import javafx.stage.Stage;
import qupath.lib.images.servers.omero.common.gui.UiUtilities;
import qupath.lib.images.servers.omero.connectionsmanager.connectionsmanager.ConnectionsManager;

import java.util.ResourceBundle;

/**
 * Command that starts a connection manager
 * (see the {@link qupath.lib.images.servers.omero.connectionsmanager.connectionsmanager connection manager} package).
 */
public class ConnectionsManagerCommand implements Runnable {
	private final static ResourceBundle resources = UiUtilities.getResources();
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
			connectionsManager = new ConnectionsManager(owner);
		} else {
			connectionsManager.show();
			connectionsManager.requestFocus();
		}
	}

	/**
	 * @return the text that should appear on the menu starting this command
	 */
	public static String getMenuTitle() {
		return resources.getString("ConnectionsManager.Command.manageServerConnections");
	}
}
