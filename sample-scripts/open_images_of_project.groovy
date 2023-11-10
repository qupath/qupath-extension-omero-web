/*
 * This script iterates over the images of the currently opened project.
 * Each image is opened, their metadata is printed, and a part of the images is read.
 * This script it not specific to OMERO but is a good way to see if all images of a project
 * are supported.
 */
def project = getProject()
if (project == null) {
    println "A project needs to be opened in QuPath before running this script"
    return
}

for (entry in project.getImageList()) {
    println "Opening " + entry.getImageName()
    
    // Accessing image metadata
    def server = entry.getServerBuilder().build()
    println "Metadata: " + server.getMetadata()

    // Accessing pixels
    double downsample = 4.0
    int x = 100
    int y = 200
    int width = 100
    int height = 200
    def request = RegionRequest.createInstance(server.getPath(), downsample, x, y, width, height)
    def img = server.readRegion(request)
    println "Image: " + img
    
    // Closing server. This is needed to free resources on the OMERO server
    server.close()
    println "Closing " + entry.getImageName()
    println ""
}
