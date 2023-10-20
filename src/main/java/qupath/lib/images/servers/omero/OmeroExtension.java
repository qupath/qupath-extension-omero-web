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

package qupath.lib.images.servers.omero;

import javafx.scene.control.SeparatorMenuItem;
import org.controlsfx.control.action.Action;

import qupath.lib.common.Version;
import qupath.lib.gui.actions.ActionTools;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.extensions.GitHubProject;
import qupath.lib.gui.extensions.QuPathExtension;
import qupath.lib.gui.tools.MenuTools;
import qupath.lib.images.servers.omero.gui.annotationimporter.AnnotationImporter;
import qupath.lib.images.servers.omero.gui.annotationsender.AnnotationSender;
import qupath.lib.images.servers.omero.gui.UiUtilities;
import qupath.lib.images.servers.omero.gui.connectionsmanager.ConnectionsManagerCommand;
import qupath.lib.images.servers.omero.gui.browser.BrowseMenu;

import java.util.ResourceBundle;

/**
 * <p>Install the OMERO extension.</p>
 * It adds 3 menus to the Extensions menu:
 * <ul>
 *     <li>A browse menu, described in {@link qupath.lib.images.servers.omero.gui.browser browser}.</li>
 *     <li>A connection manager action, described in {@link qupath.lib.images.servers.omero.gui.connectionsmanager connection manager}.</li>
 *     <li>An annotation sender action, described in {@link qupath.lib.images.servers.omero.gui.annotationsender annotation sender}.</li>
 * </ul>
 */
public class OmeroExtension implements QuPathExtension, GitHubProject {

	private static final ResourceBundle resources = UiUtilities.getResources();
	private static final Version EXTENSION_QUPATH_VERSION = Version.parse("v0.5.0");
	private static boolean alreadyInstalled = false;
	private static BrowseMenu browseMenu;

	@Override
	public void installExtension(QuPathGUI qupath) {
		if (!alreadyInstalled) {
			alreadyInstalled = true;

			browseMenu = new BrowseMenu();

			Action connectionManager = ActionTools.createAction(new ConnectionsManagerCommand(qupath.getStage()), ConnectionsManagerCommand.getMenuTitle());

			Action actionSendAnnotations = ActionTools.createAction(AnnotationSender::sendAnnotations, AnnotationSender.getMenuTitle());
			actionSendAnnotations.disabledProperty().bind(qupath.imageDataProperty().isNull());

			Action actionImportAnnotations = ActionTools.createAction(AnnotationImporter::importAnnotations, AnnotationImporter.getMenuTitle());
			actionImportAnnotations.disabledProperty().bind(qupath.imageDataProperty().isNull());

			MenuTools.addMenuItems(qupath.getMenu("Extensions", false),
					MenuTools.createMenu("OMERO",
							browseMenu,
							connectionManager,
							new SeparatorMenuItem(),
							actionSendAnnotations,
							actionImportAnnotations
					)
			);
		}
	}

	@Override
	public String getName() {
		return resources.getString("Extension.name");
	}

	@Override
	public String getDescription() {
		return resources.getString("Extension.description");
	}

	@Override
	public Version getQuPathVersion() {
		return EXTENSION_QUPATH_VERSION;
	}

	@Override
	public GitHubRepo getRepository() {
		return GitHubRepo.create(getName(), "qupath", "qupath-extension-omero");
	}

	public static BrowseMenu getBrowseMenu() {
		return browseMenu;
	}
}
