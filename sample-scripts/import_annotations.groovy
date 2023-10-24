import qupath.ext.omero.imagesserver.*

/*
 * This script imports all annotation of an image stored on an OMERO server.
 */

// Open server
def imageData = getCurrentImageData()
if (imageData == null) {
    println "An image needs to be opened in QuPath before running this script"
    return
}
def server = imageData.getServer()
def omeroServer = (OmeroImageServer) server

def shapes = omeroServer.readPathObjects()
println shapes

// Close server
omeroServer.close()