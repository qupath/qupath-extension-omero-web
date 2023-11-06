package qupath.ext.omero.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Optional;

public class TestWebUtilities {

    @Test
    void Check_URI_Creation() {
        String validURL = "https://omero-czi-cpw.mvm.ed.ac.uk/iviewer/?images=12546&dataset=1157";
        URI expectedURI = URI.create(validURL);

        URI uri = WebUtilities.createURI(validURL).orElse(null);

        Assertions.assertEquals(expectedURI, uri);
    }

    @Test
    void Check_Malformed_URI_Creation() {
        String malformedURL = "://";

        Optional<URI> uri = WebUtilities.createURI(malformedURL);

        Assertions.assertTrue(uri.isEmpty());
    }

    @Test
    void Check_OMERO_ID_On_Project() {
        long expectedID = 201;
        URI uri = URI.create(String.format("https://omero-czi-cpw.mvm.ed.ac.uk/webclient/?show=project-%d", expectedID));

        int id = WebUtilities.parseEntityId(uri).orElse(-1);

        Assertions.assertEquals(expectedID, id);
    }

    @Test
    void Check_OMERO_ID_On_Dataset() {
        long expectedID = 1157;
        URI uri = URI.create(String.format("https://omero-czi-cpw.mvm.ed.ac.uk/webclient/?show=dataset-%d", expectedID));

        int id = WebUtilities.parseEntityId(uri).orElse(-1);

        Assertions.assertEquals(expectedID, id);
    }

    @Test
    void Check_OMERO_ID_On_Webclient_Image() {
        long expectedID = 12546;
        URI uri = URI.create(String.format("https://omero-czi-cpw.mvm.ed.ac.uk/webclient/?show=image-%d", expectedID));

        int id = WebUtilities.parseEntityId(uri).orElse(-1);

        Assertions.assertEquals(expectedID, id);
    }

    @Test
    void Check_OMERO_ID_On_Webclient_Image_Alternate() {
        long expectedID = 12546;
        URI uri = URI.create(String.format("https://omero-czi-cpw.mvm.ed.ac.uk/webclient/img_detail/%d/?dataset=1157", expectedID));

        int id = WebUtilities.parseEntityId(uri).orElse(-1);

        Assertions.assertEquals(expectedID, id);
    }

    @Test
    void Check_OMERO_ID_On_WebGateway_Image() {
        long expectedID = 12546;
        URI uri = URI.create(String.format("https://omero-czi-cpw.mvm.ed.ac.uk/webgateway/img_detail/%d/?dataset=1157", expectedID));

        int id = WebUtilities.parseEntityId(uri).orElse(-1);

        Assertions.assertEquals(expectedID, id);
    }

    @Test
    void Check_OMERO_ID_On_IViewer_Image() {
        long expectedID = 12546;
        URI uri = URI.create(String.format("https://omero-czi-cpw.mvm.ed.ac.uk/iviewer/?images=%d&dataset=1157", expectedID));

        int id = WebUtilities.parseEntityId(uri).orElse(-1);

        Assertions.assertEquals(expectedID, id);
    }

    @Test
    void Check_Host_Part_Of_URI() {
        URI expectedServerURI = URI.create("https://omero-czi-cpw.mvm.ed.ac.uk");
        URI uri = URI.create(expectedServerURI + "/iviewer/?images=12546&dataset=1157");

        URI serverURI = WebUtilities.getServerURI(uri).orElse(null);

        Assertions.assertEquals(expectedServerURI, serverURI);
    }
}
