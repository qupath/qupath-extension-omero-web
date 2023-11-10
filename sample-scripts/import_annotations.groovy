import qupath.ext.omero.imagesserver.*

/*
 * This script imports all annotation of an image stored on an OMERO server
 * and add them to the image in QuPath.
 *
 * An OMERO image must be currently opened in QuPath through the QuPath GUI or through the command line, for example with:
 * path/to/QuPath/bin/QuPath script --image=the_web_link_of_your_image --server "[--username,your_username,--password,your_password]" path/to/this/script/import_annotations.groovy
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

def pathObjects = omeroServer.readPathObjects()
println "New annotations: " + pathObjects

// Add annotations to the QuPath image
addObjects(pathObjects)

// Close server
omeroServer.close()