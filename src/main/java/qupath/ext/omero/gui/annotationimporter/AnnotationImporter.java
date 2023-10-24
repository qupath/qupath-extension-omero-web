package qupath.ext.omero.gui.annotationimporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.omero.gui.UiUtilities;
import qupath.fx.dialogs.Dialogs;
import qupath.lib.gui.QuPathGUI;
import qupath.ext.omero.imagesserver.OmeroImageServer;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.ResourceBundle;

/**
 * <p>
 *     Non instantiable class that import QuPath annotations from an OMERO server
 *     to the currently opened image.
 * </p>
 * <p>
 *     Here, an annotation refers to a QuPath annotation (a path object)
 *     and <b>not</b> an OMERO annotation (some metadata attached to images for example).
 * </p>
 * <p>
 *     This class uses a {@link AnnotationForm}
 *     to prompt the user for parameters.
 * </p>
 */
public class AnnotationImporter {

    private static final Logger logger = LoggerFactory.getLogger(AnnotationImporter.class);
    private static final ResourceBundle resources = UiUtilities.getResources();

    private AnnotationImporter() {
        throw new AssertionError("This class is not instantiable.");
    }

    /**
     * @return the localized name of this command
     */
    public static String getMenuTitle() {
        return resources.getString("AnnotationsImporter.importAnnotations");
    }

    /**
     * Attempt to import annotations to the currently opened image from the corresponding OMERO server.
     * This method doesn't return anything but will show a dialog indicating the success of the operation.
     */
    public static void importAnnotations() {
        var viewer = QuPathGUI.getInstance().getViewer();

        if (viewer.getServer() instanceof OmeroImageServer omeroImageServer) {
            try {
                AnnotationForm annotationForm = new AnnotationForm();
                boolean requestCanceled = !Dialogs.showConfirmDialog(
                        resources.getString("AnnotationsImporter.dataToSend"),
                        annotationForm
                );

                if (!requestCanceled) {
                    Collection<PathObject> pathObjects = omeroImageServer.readPathObjects();
                    if (pathObjects.isEmpty()) {
                        Dialogs.showErrorMessage(
                                resources.getString("AnnotationsImporter.noAnnotations"),
                                resources.getString("AnnotationsImporter.noAnnotationsFound")
                        );
                    } else {
                        PathObjectHierarchy hierarchy = viewer.getImageData().getHierarchy();
                        String title;
                        String message = "";

                        if (annotationForm.deleteCurrentAnnotations()) {
                            hierarchy.removeObjects(hierarchy.getAnnotationObjects(),true);
                            message += resources.getString("AnnotationsImporter.currentAnnotationsDeleted") + "\n";
                        }

                        if (annotationForm.deleteCurrentDetections()) {
                            hierarchy.removeObjects(hierarchy.getDetectionObjects(), false);
                            message += resources.getString("AnnotationsImporter.currentDetectionsDeleted") + "\n";
                        }

                        hierarchy.addObjects(pathObjects);
                        hierarchy.resolveHierarchy();

                        if (pathObjects.size() == 1) {
                            title = resources.getString("AnnotationsImporter.1WrittenSuccessfully");
                            message += resources.getString("AnnotationsImporter.1AnnotationImported");
                        } else {
                            title = resources.getString("AnnotationsImporter.1WrittenSuccessfully");
                            message += MessageFormat.format(resources.getString("AnnotationsImporter.XAnnotationImported"), pathObjects.size());
                        }

                        Dialogs.showInfoNotification(
                                title,
                                message
                        );
                    }
                }
            } catch (IOException e) {
                logger.error("Error when creating the annotation form", e);
                Dialogs.showErrorMessage(
                        resources.getString("AnnotationsImporter.importAnnotations"),
                        e.getLocalizedMessage()
                );
            }
        } else {
            Dialogs.showErrorMessage(
                    resources.getString("AnnotationsImporter.importAnnotations"),
                    resources.getString("AnnotationsImporter.notFromOMERO")
            );
        }
    }
}
