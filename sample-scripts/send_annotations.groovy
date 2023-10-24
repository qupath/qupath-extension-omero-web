import qupath.ext.omero.imagesserver.*

/*
 * This script creates an annotation on the current image and send it to the OMERO server.
 */

// Open server
def imageData = getCurrentImageData()
if (imageData == null) {
    println "An image needs to be opened in QuPath before running this script"
    return
}
def server = imageData.getServer()
def omeroServer = (OmeroImageServer) server

// Create annotation
int z = 0
int t = 0
def plane = ImagePlane.getPlane(z, t)
def roi = ROIs.createEllipseROI(0, 0, 100, 100, plane)
def annotation = PathObjects.createAnnotationObject(roi)
addObject(annotation)
def annotations = [annotation]    // other annotations could be added to this list

// Send annotation to OMERO
def status = omeroServer.getClient().getApisHandler().writeROIs(omeroServer.getId(), annotations, true).get()
if (status) {
    println "Annotation sent"
} else {
    println "Annotation not sent. Check the logs"
}

// Close server
omeroServer.close()