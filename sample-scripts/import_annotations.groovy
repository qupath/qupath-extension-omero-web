import qupath.ext.omero.imagesserver.*

/*
 * This script imports all annotation of an image stored on an OMERO server
 * and add them to the image in QuPath.
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

def pathObjects = omeroServer.readPathObjects()
println "New annotations: " + pathObjects

// Add annotations to the QuPath image
addObjects(pathObjects)

// Close server
omeroServer.close()