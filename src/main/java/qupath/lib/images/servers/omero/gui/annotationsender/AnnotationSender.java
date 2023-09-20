package qupath.lib.images.servers.omero.gui.annotationsender;

import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.viewer.QuPathViewer;
import qupath.lib.images.servers.omero.gui.UiUtilities;
import qupath.lib.images.servers.omero.imagesserver.OmeroImageServer;
import qupath.lib.objects.PathObject;
import qupath.fx.dialogs.Dialogs;

import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;

/**
 * <p>
 *     Non instantiable class that sends QuPath annotations from the currently opened
 *     image to an OMERO server.
 * </p>
 * <p>
 *     Here, an annotation refers to a QuPath annotation (a path object)
 *     and <b>not</b> an OMERO annotation (some metadata attached to images for example).
 * </p>
 * <p>This class uses a {@link ConfirmationForm} to prompt the user for confirmation.</p>
 */
public class AnnotationSender {

    private static final Logger logger = LoggerFactory.getLogger(AnnotationSender.class);
    private static final ResourceBundle resources = UiUtilities.getResources();

    private AnnotationSender() {
        throw new AssertionError("This class is not instantiable.");
    }

    /**
     * @return the localized name of this command
     */
    public static String getMenuTitle() {
        return resources.getString("AnnotationsSender.sendAnnotations");
    }

    /**
     * <p>
     *     Attempt to send selected annotations of the currently opened image to the corresponding OMERO server.
     *     This method doesn't return anything but will show a dialog indicating the success of the operation.
     * </p>
     * <p>If no annotation is selected, all annotations will be sent to the server.</p>
     */
    public static void sendAnnotations() {
        var viewer = QuPathGUI.getInstance().getViewer();

        if (viewer.getServer() instanceof OmeroImageServer omeroImageServer) {
            var annotations = getAnnotations(viewer);

            if (annotations.isEmpty()) {
                Dialogs.showErrorMessage(
                        resources.getString("AnnotationsSender.sendAnnotations"),
                        resources.getString("AnnotationsSender.noAnnotations")
                );
            } else {
                URI uri = viewer.getServer().getURIs().iterator().next();

                boolean dialogConfirmed = true;
                try {
                    dialogConfirmed = Dialogs.showConfirmDialog(
                            annotations.size() == 1 ?
                                    resources.getString("AnnotationsSender.send1Annotation") :
                                    MessageFormat.format(resources.getString("AnnotationsSender.sendXAnnotations"), annotations.size()),
                            new ConfirmationForm(annotations.size(), uri.toString())
                    );
                } catch (IOException e) {
                    logger.error("Error while creating the confirmation form", e);
                }
                if (dialogConfirmed) {
                    omeroImageServer.sendAnnotations(annotations).thenAccept(success -> Platform.runLater(() -> {
                        if (success) {
                            if (annotations.size() == 1) {
                                Dialogs.showInfoNotification(
                                        resources.getString("AnnotationsSender.1WrittenSuccessfully"),
                                        resources.getString("AnnotationsSender.1AnnotationWrittenSuccessfully")
                                );
                            } else {
                                Dialogs.showInfoNotification(
                                        resources.getString("AnnotationsSender.XWrittenSuccessfully"),
                                        MessageFormat.format(resources.getString("AnnotationsSender.XAnnotationsWrittenSuccessfully"), annotations.size())
                                );
                            }
                        } else {
                            Dialogs.showErrorNotification(
                                    annotations.size() == 1 ?
                                            resources.getString("AnnotationsSender.1AnnotationFailed") :
                                            MessageFormat.format(resources.getString("AnnotationsSender.XAnnotationFailed"), annotations.size()),
                                    resources.getString("AnnotationsSender.seeLogs")
                            );
                        }
                    }));
                }
            }
        } else {
            Dialogs.showErrorMessage(
                    resources.getString("AnnotationsSender.sendAnnotations"),
                    resources.getString("AnnotationsSender.notFromOMERO")
            );
        }
    }

    private static Collection<PathObject> getAnnotations(QuPathViewer viewer) {
        var selectedObjects = viewer.getAllSelectedObjects();

        if (selectedObjects.isEmpty()) {
            boolean confirm = Dialogs.showConfirmDialog(
                    resources.getString("AnnotationsSender.sendAnnotations"),
                    resources.getString("AnnotationsSender.nothingSelected")
            );
            if (confirm && viewer.getHierarchy() != null) {
                return viewer.getHierarchy().getAnnotationObjects();
            } else {
                return List.of();
            }
        } else {
            return selectedObjects;
        }
    }
}
