package qupath.ext.omero.gui.browser;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.omero.gui.browser.newserver.NewServerForm;
import qupath.ext.omero.gui.browser.serverbrowser.BrowserCommand;
import qupath.fx.dialogs.Dialogs;
import qupath.ext.omero.core.WebClient;
import qupath.ext.omero.core.WebClients;
import qupath.ext.omero.gui.UiUtilities;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * <p>
 *     Menu allowing to create a connection with a new server
 *     (see the {@link qupath.ext.omero.gui.browser.newserver new server} package), or to browse
 *     an already connected server (see the {@link qupath.ext.omero.gui.browser.serverbrowser browse server} package).
 * </p>
 * <p>
 *     This class uses a {@link BrowseMenuModel} to update its state.
 * </p>
 */
public class BrowseMenu extends Menu {

    private static final Logger logger = LoggerFactory.getLogger(BrowseMenu.class);
    private static final ResourceBundle resources = UiUtilities.getResources();
    private final Map<WebClient, BrowserCommand> browserCommands = new HashMap<>();
    private MenuItem newServerItem;

    /**
     * Creates the browse menu.
     */
    public BrowseMenu() {
        initUI();
        setUpListeners();
    }

    public void openBrowserOfClient(WebClient client) {
        if (browserCommands.containsKey(client)) {
            browserCommands.get(client).run();
        }
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

                MenuItem clientMenuItem = new MenuItem(client.getApisHandler().getWebServerURI() + "...");
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
            try {
                NewServerForm newServerForm = new NewServerForm();

                boolean dialogConfirmed = Dialogs.showConfirmDialog(resources.getString("Browser.BrowseMenu.enterURL"), newServerForm);

                if (dialogConfirmed) {
                    String url = newServerForm.getURL();
                    WebClients.createClient(url).thenAccept(client -> Platform.runLater(() -> {
                        if (client.getStatus().equals(WebClient.Status.SUCCESS)) {
                            BrowserCommand browser = getBrowserCommand(client);
                            browser.run();
                        } else if (client.getStatus().equals(WebClient.Status.FAILED)) {
                            Dialogs.showErrorMessage(
                                    resources.getString("Browser.BrowseMenu.webServer"),
                                    MessageFormat.format(resources.getString("Browser.BrowseMenu.connectionFailed"), url)
                            );
                        }
                    }));
                }
            } catch (IOException e) {
                logger.error("Error while creating the new server form", e);
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
