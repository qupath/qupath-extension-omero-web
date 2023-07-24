package qupath.lib.images.servers.omero.browser;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import qupath.lib.gui.dialogs.Dialogs;
import qupath.lib.images.servers.omero.common.api.clients.WebClient;
import qupath.lib.images.servers.omero.common.api.clients.WebClients;
import qupath.lib.images.servers.omero.browser.browseserver.BrowserCommand;
import qupath.lib.images.servers.omero.browser.newserver.NewServerForm;
import qupath.lib.images.servers.omero.common.gui.UiUtilities;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * <p>
 *     Menu allowing to create a connection with a new server
 *     (see the {@link qupath.lib.images.servers.omero.browser.newserver new server} package), or to browse
 *     an already connected server (see the {@link qupath.lib.images.servers.omero.browser.browseserver browse server} package).
 * </p>
 * <p>
 *     This class uses a {@link BrowseMenuModel} to update its state.
 * </p>
 */
public class BrowseMenu extends Menu {
    private final static ResourceBundle resources = UiUtilities.getResources();
    private final Map<WebClient, BrowserCommand> browserCommands = new HashMap<>();
    private MenuItem newServerItem;

    /**
     * Creates the browse menu.
     */
    public BrowseMenu() {
        initUI();
        setUpListeners();
    }

    private void initUI() {
        setText(resources.getString("Browser.BrowseMenu.browseServer"));
        createNewServerItem();
    }

    private void setUpListeners() {
        BrowseMenuModel.getClients().addListener((ListChangeListener<? super WebClient>) change -> {
            while (change.next()) {
                if (change.wasRemoved()) {
                    for (WebClient client: change.getRemoved()) {
                        if (browserCommands.containsKey(client)) {
                            browserCommands.get(client).close();
                            browserCommands.remove(client);
                        }
                    }
                }
            }

            getItems().clear();
            for (WebClient client: change.getList()) {
                BrowserCommand browserCommand = getBrowserCommand(client);

                MenuItem clientMenuItem = new MenuItem(client.getServerURI() + "...");
                clientMenuItem.setOnAction(e -> browserCommand.run());
                getItems().add(clientMenuItem);
            }

            if (getItems().size() > 0) {
                getItems().add(new SeparatorMenuItem());
            }

            getItems().add(newServerItem);
        });
    }

    private void createNewServerItem() {
        newServerItem = new MenuItem(resources.getString("Browser.BrowseMenu.newServer"));
        newServerItem.setOnAction(ignoredEvent -> {
            NewServerForm newServerForm = new NewServerForm();
            boolean dialogConfirmed = Dialogs.showConfirmDialog(resources.getString("Browser.BrowseMenu.enterURL"), newServerForm);

            if (dialogConfirmed) {
                String url = newServerForm.getURL();
                WebClients.createClient(url).thenAccept(client -> Platform.runLater(() -> {
                    if (client.isEmpty()) {
                        Dialogs.showErrorMessage(
                                resources.getString("Browser.BrowseMenu.webServer"),
                                MessageFormat.format(resources.getString("Browser.BrowseMenu.connectionFailed"), url)
                        );
                    } else {
                        BrowserCommand browser = getBrowserCommand(client.get());
                        browser.run();
                    }
                }));
            }
        });
        getItems().add(newServerItem);
    }

    private BrowserCommand getBrowserCommand(WebClient client) {
        if (!browserCommands.containsKey(client)) {
            browserCommands.put(client, new BrowserCommand(client));
        }

        return browserCommands.get(client);
    }
}
