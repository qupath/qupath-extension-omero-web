package qupath.ext.omero.gui.browser.serverbrowser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.omero.core.WebClient;

import java.io.IOException;

/**
 * Command that starts a {@link Browser browser}.
 */
public class BrowserCommand implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(BrowserCommand.class);
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
            try {
                browser = new Browser(client);
            } catch (IOException e) {
                logger.error("Error while creating the browser", e);
            }
        } else {
            browser.show();
            browser.requestFocus();
        }
    }

    /**
     * Close the corresponding browser.
     */
    public void close() {
        if (browser != null) {
            browser.close();
        }
    }
}
