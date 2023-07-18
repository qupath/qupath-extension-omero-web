/*-
 * #%L
 * This file is part of QuPath.
 * %%
 * Copyright (C) 2018 - 2023 QuPath developers, The University of Edinburgh
 * %%
 * QuPath is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * QuPath is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QuPath.  If not, see <https://www.gnu.org/licenses/>.
 * #L%
 */

package qupath.lib.images.servers.omero.connections_manager;

import javafx.stage.Stage;
import qupath.lib.images.servers.omero.common.gui.UiUtilities;
import qupath.lib.images.servers.omero.connections_manager.connections_manager.ConnectionsManager;

import java.util.ResourceBundle;

/**
 * Command that starts a {@link qupath.lib.images.servers.omero.connections_manager.connections_manager.ConnectionsManager connection manager}.
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
