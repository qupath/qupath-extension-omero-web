package qupath.lib.images.servers.omero.common.api.requests.apis;

import qupath.lib.images.servers.omero.common.api.requests.Requests;
import qupath.lib.images.servers.omero.common.api.requests.RequestsUtilities;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Utility methods used by the different APIs.
 */
class ApiUtilities {
    private ApiUtilities() {
        throw new RuntimeException("This class is not instantiable.");
    }

    /**
     * <p>Attempt to retrieve an image from a URL link.</p>
     * <p>This function is asynchronous.</p>
     *
     * @param url  the URL of the request
     * @return a CompletableFuture containing the resulting image, or an empty optional if it couldn't be retrieved
     */
    public static CompletableFuture<Optional<BufferedImage>> getImage(String url) {
        return RequestsUtilities.createURI(url)
                .map(Requests::getImage)
                .orElse(CompletableFuture.completedFuture(Optional.empty()));
    }

    /**
     * <p>Concatenate and convert two char arrays to a byte array using the UTF 8 encoding.</p>
     * <p>The input parameters are cleared (filled with zeros) once processed.</p>
     */
    public static byte[] concatAndConvertToBytes(char[] arr1, char[] arr2) {
        return toBytes(concatChars(arr1, arr2));
    }

    private static byte[] toBytes(char[] chars) {
        CharBuffer charBuffer = CharBuffer.wrap(chars);
        ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(charBuffer);

        byte[] bytes = Arrays.copyOfRange(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit());

        // Clear sensitive data
        Arrays.fill(byteBuffer.array(), (byte) 0);

        return bytes;
    }

    private static char[] concatChars(char[] arr1, char[] arr2) {
        char[] result = new char[arr1.length + arr2.length];

        System.arraycopy(arr1, 0, result, 0, arr1.length);
        System.arraycopy(arr2, 0, result, arr1.length, arr2.length);

        // Clear sensitive data
        Arrays.fill(arr1, (char) 0);
        Arrays.fill(arr2, (char) 0);

        return result;
    }
}
