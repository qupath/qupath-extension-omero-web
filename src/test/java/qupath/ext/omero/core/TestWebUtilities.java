package qupath.ext.omero.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Optional;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class TestWebUtilities {

    @Test
    void Check_URI_Creation() {
        Optional<URI> optionalURI = WebUtilities.createURI("https://omero-czi-cpw.mvm.ed.ac.uk/iviewer/?images=12546&dataset=1157");

        URI uri = optionalURI.orElse(null);

        Assertions.assertEquals(URI.create("https://omero-czi-cpw.mvm.ed.ac.uk/iviewer/?images=12546&dataset=1157"), uri);
    }

    @Test
    void Check_Malformed_URI_Creation() {
        Optional<URI> optionalURI = WebUtilities.createURI("://");

        boolean urlCreated = optionalURI.isPresent();

        Assertions.assertFalse(urlCreated);
    }

    @Test
    void Check_OMERO_ID_On_Project() {
        URI uri = URI.create("https://omero-czi-cpw.mvm.ed.ac.uk/webclient/?show=project-201");

        int id = WebUtilities.parseEntityId(uri).orElse(-1);

        Assertions.assertEquals(201, id);
    }

    @Test
    void Check_OMERO_ID_On_Dataset() {
        URI uri = URI.create("https://omero-czi-cpw.mvm.ed.ac.uk/webclient/?show=dataset-1157");

        int id = WebUtilities.parseEntityId(uri).orElse(-1);

        Assertions.assertEquals(1157, id);
    }

    @Test
    void Check_OMERO_ID_On_Webclient_Image() {
        URI uri = URI.create("https://omero-czi-cpw.mvm.ed.ac.uk/webclient/?show=image-12546");

        int id = WebUtilities.parseEntityId(uri).orElse(-1);

        Assertions.assertEquals(12546, id);
    }

    @Test
    void Check_OMERO_ID_On_Webclient_Image_Alternate() {
        URI uri = URI.create("https://omero-czi-cpw.mvm.ed.ac.uk/webclient/img_detail/12546/?dataset=1157");

        int id = WebUtilities.parseEntityId(uri).orElse(-1);

        Assertions.assertEquals(12546, id);
    }

    @Test
    void Check_OMERO_ID_On_WebGateway_Image() {
        URI uri = URI.create("https://omero-czi-cpw.mvm.ed.ac.uk/webgateway/img_detail/12546/?dataset=1157");

        int id = WebUtilities.parseEntityId(uri).orElse(-1);

        Assertions.assertEquals(12546, id);
    }

    @Test
    void Check_OMERO_ID_On_IViewer_Image() {
        URI uri = URI.create("https://omero-czi-cpw.mvm.ed.ac.uk/iviewer/?images=12546&dataset=1157");

        int id = WebUtilities.parseEntityId(uri).orElse(-1);

        Assertions.assertEquals(12546, id);
    }

    @Test
    void Check_Host_Part_Of_URI() {
        URI uri = URI.create("https://omero-czi-cpw.mvm.ed.ac.uk/iviewer/?images=12546&dataset=1157");

        URI serverURI = WebUtilities.getServerURI(uri).orElse(null);

        Assertions.assertEquals(URI.create("https://omero-czi-cpw.mvm.ed.ac.uk"), serverURI);
    }
}
