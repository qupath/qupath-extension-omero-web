package qupath.ext.omero.gui;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
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
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.fx.dialogs.Dialogs;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.commands.ProjectCommands;
import qupath.lib.gui.tools.GuiTools;
import qupath.lib.images.servers.ImageServerProvider;
import qupath.ext.omero.imagesserver.OmeroImageServerBuilder;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Utility methods related to the user interface.
 */
public class UiUtilities {

    private static final Logger logger = LoggerFactory.getLogger(UiUtilities.class);
    private static final ResourceBundle resources = ResourceBundle.getBundle("qupath.ext.omero.strings");

    private UiUtilities() {
        throw new AssertionError("This class is not instantiable.");
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
     * Loads the FXML file located at the URL and set its controller.
     *
     * @param controller  the controller of the FXML file to load
     * @param url  the path of the FXML file to load
     * @throws IOException if an error occurs while loading the FXML file
     */
    public static void loadFXML(Object controller, URL url) throws IOException {
        FXMLLoader loader = new FXMLLoader(url, resources);
        loader.setRoot(controller);
        loader.setController(controller);
        loader.load();
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
        Circle circle = new Circle(5);
        circle.getStyleClass().add(active ? "connected" : "disconnected");
        return circle;
    }

    /**
     * Paint the specified image onto the specified canvas.
     * Additionally, it returns the {@code WritableImage} for further use.
     *
     * @param image  the image to paint on the canvas
     * @param canvas  the canvas to paint
     * @return a copy of the input image
     */
    public static WritableImage paintBufferedImageOnCanvas(BufferedImage image, Canvas canvas) {
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Color the canvas in black, in case no new image can be painted
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        WritableImage wi = SwingFXUtils.toFXImage(image, null);

        GuiTools.paintImage(canvas, wi);
        return wi;
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
                    resources.getString("Utilities.noImages"),
                    resources.getString("Utilities.noValidImagesInSelected")
            );
        } else {
            if (QuPathGUI.getInstance().getProject() == null) {
                if (uris.length == 1) {
                    try {
                        QuPathGUI.getInstance().openImage(QuPathGUI.getInstance().getViewer(), uris[0], true, true);
                    } catch (IOException e) {
                        logger.error("Could not open image", e);
                    }
                } else {
                    Dialogs.showErrorMessage(
                            resources.getString("Utilities.openImages"),
                            resources.getString("Utilities.createProjectFirst")
                    );
                }
            } else {
                promptToImportOmeroImages(uris);
            }
        }
    }

    /**
     * <p>Propagates changes made to a property to another property.</p>
     * <p>The listening property is updated in the UI thread.</p>
     *
     * @param propertyToUpdate  the property to update
     * @param propertyToListen  the property to listen
     * @param <T>  the type of the property
     */
    public static <T> void bindPropertyInUIThread(WritableValue<T> propertyToUpdate, ObservableValue<T> propertyToListen) {
        propertyToUpdate.setValue(propertyToListen.getValue());
        propertyToListen.addListener((p, o, n) -> {
            if (Platform.isFxApplicationThread()) {
                propertyToUpdate.setValue(n);
            } else {
                Platform.runLater(() -> propertyToUpdate.setValue(n));
            }
        });
    }

    /**
     * <p>Propagates changes made to an observable set to another observable set.</p>
     * <p>The listening set is updated in the UI thread.</p>
     *
     * @param setToUpdate  the set to update
     * @param setToListen  the set to listen
     * @param <T>  the type of the elements of the sets
     */
    public static <T> void bindSetInUIThread(ObservableSet<T> setToUpdate, ObservableSet<T> setToListen) {
        setToUpdate.addAll(setToListen);

        setToListen.addListener((SetChangeListener<? super T>) change -> {
            if (Platform.isFxApplicationThread()) {
                if (change.wasAdded()) {
                    setToUpdate.add(change.getElementAdded());
                }
                if (change.wasRemoved()) {
                    setToUpdate.remove(change.getElementRemoved());
                }
            } else {
                Platform.runLater(() -> {
                    if (change.wasAdded()) {
                        setToUpdate.add(change.getElementAdded());
                    }
                    if (change.wasRemoved()) {
                        setToUpdate.remove(change.getElementRemoved());
                    }
                });
            }
        });
    }

    /**
     * <p>Propagates changes made to an observable list to another observable list.</p>
     * <p>The listening list is updated in the UI thread.</p>
     *
     * @param listToUpdate  the list to update
     * @param listToListen  the list to listen
     * @param <T>  the type of the elements of the lists
     */
    public static <T> void bindListInUIThread(ObservableList<T> listToUpdate, ObservableList<T> listToListen) {
        listToUpdate.addAll(listToListen);

        listToListen.addListener((ListChangeListener<? super T>) change -> Platform.runLater(() -> {
            if (Platform.isFxApplicationThread()) {
                listToUpdate.setAll(change.getList());
            } else {
                Platform.runLater(() -> listToUpdate.setAll(change.getList()));
            }
        }));
    }

    /**
     * Show a window that is hidden. the focus is also set to it.
     *
     * @param window  the window to show
     */
    public static void showHiddenWindow(Stage window) {
        window.show();
        window.requestFocus();

        // This is necessary to avoid a bug on Linux
        // that reset the window size
        window.setWidth(window.getWidth() + 1);
        window.setHeight(window.getHeight() + 1);
    }

    private static void promptToImportOmeroImages(String... validUris) {
        ProjectCommands.promptToImportImages(
                QuPathGUI.getInstance(),
                ImageServerProvider.getInstalledImageServerBuilders(BufferedImage.class).stream().filter(b -> b instanceof OmeroImageServerBuilder).findFirst().orElse(null),
                validUris
        );
    }
}
