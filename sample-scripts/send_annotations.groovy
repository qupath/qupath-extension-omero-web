import qupath.ext.omero.imagesserver.*

/*
 * This script send all annotations of the current image to the OMERO server.
 *
 * An OMERO image must be currently opened in QuPath through the QuPath GUI or
 * through the command line (see the open_image_from_command_line.groovy script).
 */

// Open server
def imageData = getCurrentImageData()
if (imageData == null) {
    println "An image needs to be opened in QuPath before running this script"
    return
}
def server = imageData.getServer()
def omeroServer = (OmeroImageServer) server

// Get all annotations of the current image
def annotations = getAnnotationObjects()

// Send annotation to OMERO
def removeExistingAnnotations = true
def status = omeroServer.sendPathObjects(annotations, removeExistingAnnotations)
if (status) {
    println "Annotations sent"
} else {
    println "Annotations not sent. Check the logs"
}

// Close server
omeroServer.close()