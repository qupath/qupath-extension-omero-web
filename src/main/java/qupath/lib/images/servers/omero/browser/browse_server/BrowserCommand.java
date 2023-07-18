package qupath.lib.images.servers.omero.browser.browse_server;

import qupath.lib.images.servers.omero.common.api.clients.WebClient;
import qupath.lib.images.servers.omero.browser.browse_server.browser.Browser;

/**
 * Command that starts a {@link qupath.lib.images.servers.omero.browser.browse_server.browser.Browser browser}.
 */
public class BrowserCommand implements Runnable {
    private final WebClient client;
    private Browser browser;

    /**
     * Creates a browser command.
     *
     * @param client  the web client which will be used by the browser to retrieve data from the corresponding OMERO server
     */
    public BrowserCommand(WebClient client) {
        this.client = client;
    }
    @Override
    public void run() {
        if (browser == null) {
            browser = new Browser(client);
        } else {
            browser.show();
            browser.requestFocus();
        }
    }

    /**
     * Close the corresponding browser.
     */
    public void remove() {
        if (browser != null) {
            browser.close();
        }
    }
}
