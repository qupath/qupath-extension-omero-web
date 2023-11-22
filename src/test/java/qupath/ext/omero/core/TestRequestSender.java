package qupath.ext.omero.core;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import qupath.ext.omero.OmeroServer;
import qupath.ext.omero.TestUtilities;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class TestRequestSender extends OmeroServer {

    @Test
    void Check_Link_Reachable() throws ExecutionException, InterruptedException {
        URI reachableLink = URI.create(OmeroServer.getServerURL());

        boolean linkReachable = RequestSender.isLinkReachableWithGet(reachableLink).get();

        Assertions.assertTrue(linkReachable);
    }

    @Test
    void Check_Link_Unreachable() throws ExecutionException, InterruptedException {
        URI unreachableLink = URI.create("http://invalid.invalid");

        boolean linkReachable = RequestSender.isLinkReachableWithGet(unreachableLink).get();

        Assertions.assertFalse(linkReachable);
    }

    @Test
    void Check_Get_Request() throws ExecutionException, InterruptedException {
        URI reachableLink = URI.create(OmeroServer.getServerURL());

        Optional<String> response = RequestSender.get(reachableLink).get();

        Assertions.assertTrue(response.isPresent());
    }

    @Test
    void Check_Get_Request_On_Invalid_Link() throws ExecutionException, InterruptedException {
        URI unreachableLink = URI.create("http://invalid.invalid");

        Optional<String> response = RequestSender.get(unreachableLink).get();

        Assertions.assertTrue(response.isEmpty());
    }

    @Test
    void Check_Get_Request_And_Convert() throws ExecutionException, InterruptedException {
        URI apiLink = URI.create(OmeroServer.getServerURL() + "/api/");
        ApiResponse expectedResponse = new ApiResponse(OmeroServer.getServerURL());

        ApiResponse response = RequestSender.getAndConvert(apiLink, ApiResponse.class).get().orElse(null);

        Assertions.assertEquals(expectedResponse, response);
    }

    @Test
    void Check_Get_Request_And_Convert_On_Invalid_Link() throws ExecutionException, InterruptedException {
        URI invalidApiLink = URI.create(OmeroServer.getServerURL());

        ApiResponse response = RequestSender.getAndConvert(invalidApiLink, ApiResponse.class).get().orElse(null);

        Assertions.assertNull(response);
    }

    @Test
    void Check_Get_Image() throws ExecutionException, InterruptedException {
        URI imageLink = URI.create(OmeroServer.getServerURL() + "/static/webgateway/img/folder16.png");

        BufferedImage image = RequestSender.getImage(imageLink).get().orElse(null);

        Assertions.assertNotNull(image);
    }

    @Test
    void Check_Get_Image_On_Invalid_Link() throws ExecutionException, InterruptedException {
        URI invalidImageLink = URI.create(OmeroServer.getServerURL());

        BufferedImage image = RequestSender.getImage(invalidImageLink).get().orElse(null);

        Assertions.assertNull(image);
    }

    @Test
    void Check_Get_Request_And_Convert_To_JSON_List() throws ExecutionException, InterruptedException {
        URI jsonListLink = URI.create(OmeroServer.getServerURL() + "/api/");
        String memberName = "data";
        List<JsonElement> expectedResponse = List.of(new Gson().toJsonTree(new ApiResponseVersion(OmeroServer.getServerURL())));

        List<JsonElement> response = RequestSender.getAndConvertToJsonList(jsonListLink, memberName).get();

        TestUtilities.assertListEqualsWithoutOrder(expectedResponse, response);
    }

    @Test
    void Check_Get_Request_And_Convert_To_JSON_List_On_Invalid_Request() throws ExecutionException, InterruptedException {
        URI invalidJsonListLink = URI.create(OmeroServer.getServerURL());
        String memberName = "data";
        List<JsonElement> expectedResponse = List.of();

        List<JsonElement> response = RequestSender.getAndConvertToJsonList(invalidJsonListLink, memberName).get();

        TestUtilities.assertListEqualsWithoutOrder(expectedResponse, response);
    }

    @Test
    void Check_Get_Request_And_Convert_To_JSON_List_With_Invalid_Member() throws ExecutionException, InterruptedException {
        URI jsonListLink = URI.create(OmeroServer.getServerURL() + "/api/");
        String memberName = "invalid";
        List<JsonElement> expectedResponse = List.of();

        List<JsonElement> response = RequestSender.getAndConvertToJsonList(jsonListLink, memberName).get();

        TestUtilities.assertListEqualsWithoutOrder(expectedResponse, response);
    }

    private static class ApiResponse {
        @SerializedName("data") private List<ApiResponseVersion> versions;

        public ApiResponse(String baseAddress) {
            versions = List.of(new ApiResponseVersion(baseAddress));
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof ApiResponse other))
                return false;
            return other.versions.equals(versions);
        }

        @Override
        public String toString() {
            return versions.toString();
        }
    }

    private static class ApiResponseVersion {
        @SerializedName("version") private String version;
        @SerializedName("url:base") private String url;

        public ApiResponseVersion(String baseAddress) {
            version = "0";
            url = baseAddress + "/api/v0/";
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof ApiResponseVersion other))
                return false;
            return other.version.equals(version) && other.url.equals(url);
        }

        @Override
        public String toString() {
            return "version: " + version + "; url: " + url;
        }
    }
}




