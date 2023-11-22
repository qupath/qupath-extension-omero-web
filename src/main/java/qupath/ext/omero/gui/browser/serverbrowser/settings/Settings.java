package qupath.ext.omero.gui.browser.serverbrowser.settings;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.omero.core.WebClient;
import qupath.ext.omero.core.pixelapis.mspixelbuffer.MsPixelBufferAPI;
import qupath.ext.omero.gui.UiUtilities;
import qupath.fx.dialogs.Dialogs;

import java.io.IOException;
import java.util.ResourceBundle;

/**
 * Window allowing to change settings related to a {@link WebClient}.
 */
public class Settings extends Stage {

    private static final Logger logger = LoggerFactory.getLogger(Settings.class);
    private static final ResourceBundle resources = UiUtilities.getResources();
    private final MsPixelBufferAPI msPixelBufferAPI;
    @FXML
    private TextField msPixelBufferAPIPort;

    /**
     * Creates the settings window.
     *
     * @param ownerWindow  the stage who should own this window
     * @param client  the client whose settings should be displayed
     * @throws IOException if an error occurs while creating the window
     */
    public Settings(Stage ownerWindow, WebClient client) throws IOException {
        this.msPixelBufferAPI = client.getPixelAPI(MsPixelBufferAPI.class);

        initUI(ownerWindow);
        setUpListeners();
    }

    @FXML
    private void onOKClicked(ActionEvent ignoredEvent) {
        if (save()) {
            close();
        }
    }

    @FXML
    private void onCancelClicked(ActionEvent ignoredEvent) {
        close();
    }

    @FXML
    private void onApplyClicked(ActionEvent ignoredEvent) {
        save();
    }

    private void initUI(Stage ownerWindow) throws IOException {
        UiUtilities.loadFXML(this, Settings.class.getResource("settings.fxml"));

        msPixelBufferAPIPort.setText(String.valueOf(msPixelBufferAPI.getPort().get()));

        initOwner(ownerWindow);
        show();
    }

    private void setUpListeners() {
        showingProperty().addListener((p, o, n) -> {
            if (n) {
                msPixelBufferAPIPort.setText(String.valueOf(msPixelBufferAPI.getPort().get()));
            }
        });

        msPixelBufferAPIPort.textProperty().addListener((p, o, n) -> {
            if (!n.matches("\\d*")) {
                msPixelBufferAPIPort.setText(n.replaceAll("\\D", ""));
            }
        });

        msPixelBufferAPI.getPort().addListener((p, o, n) -> Platform.runLater(() ->
                msPixelBufferAPIPort.setText(String.valueOf(n))
        ));
    }

    private boolean save() {
        try {
            msPixelBufferAPI.setPort(Integer.parseInt(msPixelBufferAPIPort.getText()), false);

            Dialogs.showInfoNotification(
                    resources.getString("Browser.ServerBrowser.Settings.saved"),
                    resources.getString("Browser.ServerBrowser.Settings.parametersSaved")
            );

            return true;
        } catch (NumberFormatException e) {
            logger.warn("Incorrect port", e);
            return false;
        }
    }
}
