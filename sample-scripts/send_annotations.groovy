import qupath.ext.omero.imagesserver.*

/*
 * This script send all annotations of the current image to the OMERO server.
 *
 * An OMERO image must be currently opened in QuPath through the QuPath GUI or through the command line, for example with:
 * path/to/QuPath/bin/QuPath script --image=the_web_link_of_your_image --server "[--username,your_username,--password,your_password]" path/to/this/script/send_annotations.groovy
 * --server "[--username,your_username,--password,your_password]" can be omitted and will be prompted if necessary.
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