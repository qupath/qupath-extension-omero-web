package qupath.lib.images.servers.omero.common.gui;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.commands.ProjectCommands;
import qupath.lib.gui.dialogs.Dialogs;
import qupath.lib.gui.tools.GuiTools;
import qupath.lib.gui.tools.IconFactory;
import qupath.lib.images.servers.ImageServerProvider;
import qupath.lib.images.servers.omero.common.images_servers.OmeroImageServerBuilder;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Utility methods related to the user interface.
 */
public class UiUtilities {
    final private static Logger logger = LoggerFactory.getLogger(UiUtilities.class);
    final private static ResourceBundle resources = ResourceBundle.getBundle("qupath.lib.images.servers.omero.strings");

    private UiUtilities() {
        throw new RuntimeException("This class is not instantiable.");
    }

    /**
     * @return whether the graphical user interface is used
     */
    public static boolean usingGUI() {
        return QuPathGUI.getInstance() != null;
    }

    /**
     * Creates a Label whose text is selectable with the cursor.
     *
     * @param text  the text to display in the label
     * @return a Label that can be selected
     */
    public static Label createSelectableLabel(String text) {
        Label label = new Label(text);

        StackPane textStack = new StackPane();
        TextField textField = new TextField(text);
        textField.setEditable(false);
        textField.setStyle(
                "-fx-background-color: transparent; -fx-background-insets: 0; -fx-background-radius: 0; -fx-padding: 0;"
        );

        // the invisible label is a hack to get the textField to size like a label.
        Label invisibleLabel = new Label();
        invisibleLabel.textProperty().bind(label.textProperty());
        invisibleLabel.setVisible(false);
        textStack.getChildren().addAll(invisibleLabel, textField);
        label.textProperty().bindBidirectional(textField.textProperty());
        label.setGraphic(textStack);
        label.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        return label;
    }

    /**
     * Loads the FXML file located at the URL, set its controller and returns the
     * resources containing the localized strings.
     *
     * @param controller  the controller of the FXML file to load
     * @param url  the path of the FXML file to load
     * @return the resources containing the localized strings
     */
    public static ResourceBundle loadFXMLAndGetResources(Object controller, URL url) {
        FXMLLoader loader = new FXMLLoader(url, resources);
        loader.setRoot(controller);
        loader.setController(controller);

        try {
            loader.load();
        } catch (IOException e) {
            logger.error("Could not open " + url, e);
        }
        return resources;
    }

    /**
     * @return the resources containing the localized strings
     */
    public static ResourceBundle getResources() {
        return resources;
    }

    /**
     * @return a node with a dot, either filled with green if {@code active} or red otherwise
     */
    public static Node createStateNode(boolean active) {
        return IconFactory.createNode(
                QuPathGUI.TOOLBAR_ICON_SIZE,
                QuPathGUI.TOOLBAR_ICON_SIZE,
                active ? IconFactory.PathIcons.ACTIVE_SERVER : IconFactory.PathIcons.INACTIVE_SERVER
        );
    }

    /**
     * Paint the specified image onto the specified canvas.
     * Additionally, it returns the {@code WritableImage} for further use.
     *
     * @return the writable image, or an empty Optional if the provided image is null
     */
    public static Optional<WritableImage> paintBufferedImageOnCanvas(BufferedImage image, Canvas canvas) {
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Color the canvas in black, in case no new image can be painted
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        if (image == null) {
            return Optional.empty();
        } else {
            WritableImage wi = SwingFXUtils.toFXImage(image, null);

            GuiTools.paintImage(canvas, wi);
            return Optional.of(wi);
        }
    }

    /**
     * <p>Attempt to open images in the QuPath viewer from the provided URIs.</p>
     * <p>
     *     If impossible (no URI provided or attempt to open multiple images
     *     without using a project), an error message will appear.
     * </p>
     *
     * @param uris  the URIs of the images to open
     */
    public static void openImages(String... uris) {
        if (uris.length == 0) {
            Dialogs.showErrorMessage(
                    resources.getString("Common.gui.Utilities.noImages"),
                    resources.getString("Common.gui.Utilities.noValidImagesInSelected")
            );
        } else {
            if (QuPathGUI.getInstance().getProject() == null) {
                if (uris.length == 1) {
                    QuPathGUI.getInstance().openImage(uris[0], true, true);
                } else {
                    Dialogs.showErrorMessage(
                            resources.getString("Common.gui.Utilities.openImages"),
                            resources.getString("Common.gui.Utilities.createProjectFirst")
                    );
                }
            } else {
                promptToImportOmeroImages(uris);
            }
        }
    }

    private static void promptToImportOmeroImages(String... validUris) {
        ProjectCommands.promptToImportImages(
                QuPathGUI.getInstance(),
                ImageServerProvider.getInstalledImageServerBuilders(BufferedImage.class).stream().filter(b -> b instanceof OmeroImageServerBuilder).findFirst().orElse(null),
                validUris
        );
    }
}
