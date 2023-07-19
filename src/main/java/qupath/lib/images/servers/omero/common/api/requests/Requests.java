package qupath.lib.images.servers.omero.common.api.requests;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.io.GsonTools;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.Long.max;
import static java.time.temporal.ChronoUnit.SECONDS;

/**
 * <p>
 *     Utility class that sends web request and can convert HTTP responses
 *     to an understandable format (like JSON for example).
 * </p>
 * <p>Each request is performed asynchronously.</p>
 * <p>
 *     The requests never throw exceptions. When an error (e.g. connection failed) occurs,
 *     a message is logged and an empty Optional (or an empty list depending on the request)
 *     is returned.
 * </p>
 */
public class Requests {
    private final static Logger logger = LoggerFactory.getLogger(Requests.class);
    private final static int REQUEST_TIMEOUT = 20;
    /**
     * <p>
     *     The redirection policy is specified to allow the HTTP client to automatically
     *     follow HTTP redirections (from http:// to https:// for example).
     *     This is needed for icons requests for example.
     * </p>
     * <p>
     *     The cookie policy is specified because some APIs use a
     *     <a href="https://docs.openmicroscopy.org/omero/5.6.0/developers/json-api.html#get-csrf-token">CSRF token</a>.
     *     This token is stored in a session cookie, so we need to store this session cookie.
     * </p>
     */
    private final static HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER))
            .build();

    private Requests() {
        throw new RuntimeException("This class is not instantiable.");
    }

    /**
     * Performs a GET request to the specified URI.
     *
     * @param uri  the link of the request
     * @return the raw HTTP response with in text format, or an empty Optional if the request failed
     */
    public static CompletableFuture<Optional<String>> get(URI uri) {
        return request(getGETRequest(uri));
    }

    /**
     * Performs a GET request to the specified URI and convert the response to the provided type.
     *
     * @param uri  the link of the request
     * @param conversionClass  the class the response should be converted to
     * @return the HTTP response converted to the desired format, or an empty Optional if the request or the conversion failed
     */
    public static <T> CompletableFuture<Optional<T>> getAndConvert(URI uri, Class<T> conversionClass) {
        return getAndConvert(uri, TypeToken.get(conversionClass));
    }

    /**
     * See {@link #getAndConvert(URI, Class)}. This method is suited for generic types.
     */
    public static <T> CompletableFuture<Optional<T>> getAndConvert(URI uri, TypeToken<T> conversionClass) {
        return get(uri).thenApply(body -> {
            if (body.isPresent()) {
                try {
                    return Optional.ofNullable(GsonTools.getInstance().fromJson(body.get(), conversionClass));
                } catch (Exception e) {
                    logger.error("Cannot deserialize " + body + " got from " + uri, e);
                    return Optional.empty();
                }
            } else {
                return Optional.empty();
            }
        });
    }

    /**
     * <p>Performs a GET request to the specified URI when the response is expected to be paginated.</p>
     * <p>If there are more results than the size of each page, subsequent requests are carried to obtain all results.</p>
     *
     * @param uri  the link of the request
     * @return a list of JSON elements containing all elements, or an empty list if an error occurred
     */
    public static CompletableFuture<List<JsonElement>> getPaginated(URI uri) {
        String delimiter = uri.getQuery() == null || uri.getQuery().isEmpty() ? "?" : "&";

        return getAndConvert(uri, JsonObject.class).thenApplyAsync(response -> {
            if (response.isEmpty()) {
                return List.of();
            } else {
                try {
                    JsonObject meta = response.get().getAsJsonObject("meta");

                    List<JsonElement> elements = response.get().getAsJsonArray("data").asList();
                    elements.addAll(readFollowingPages(uri + delimiter, meta.get("limit").getAsInt(), meta.get("totalCount").getAsInt()));

                    return elements;
                } catch (Exception e) {
                    logger.error("Cannot read paginated URI " + uri, e);
                    return List.of();
                }
            }
        });
    }

    /**
     * Performs a GET request to the specified URI and convert the response to an image.
     *
     * @param uri  the link of the request
     * @return the HTTP response converted to an image, or an empty Optional if the request or the conversion failed
     */
    public static CompletableFuture<Optional<BufferedImage>> getImage(URI uri) {
        return httpClient
                .sendAsync(getGETRequest(uri), HttpResponse.BodyHandlers.ofByteArray())
                .handle((response, error) -> {
                    boolean requestFailed = hasRequestFailed(uri, response, error);
                    if (requestFailed) {
                        return Optional.empty();
                    } else {
                        try (InputStream targetStream = new ByteArrayInputStream(response.body())) {
                            return Optional.ofNullable(ImageIO.read(targetStream));
                        } catch (IOException e) {
                            logger.error("Error when reading image from " + uri, e);
                            return Optional.empty();
                        }
                    }
                });
    }

    /**
     * Performs a GET request to the specified URI and convert the response to a list of JSON elements
     * using the provided member of the response.
     *
     * @param uri  the link of the request
     * @param memberName  the member of the response that should contain the list to convert
     * @return a list of JSON elements, or an empty list if the request or the conversion failed
     */
    public static CompletableFuture<List<JsonElement>> requestAndConvertToJsonList(URI uri, String memberName) {
        return getAndConvert(uri, JsonObject.class).thenApply(response -> {
            if (response.isPresent()) {
                try {
                    return response.get().getAsJsonArray(memberName).asList();
                } catch (Exception e) {
                    logger.error("Cannot parse " + response.get() + " from " + uri, e);
                    return List.of();
                }
            } else {
                return List.of();
            }
        });
    }

    /**
     * Performs a GET request to the specified URI to determine if it is reachable.
     *
     * @param uri  the link of the request
     * @return whether the provided link is reachable
     */
    public static CompletableFuture<Boolean> isLinkReachable(URI uri) {
        return httpClient
                .sendAsync(
                        getGETRequest(uri),
                        HttpResponse.BodyHandlers.ofString()
                ).handle((response, error) -> !hasRequestFailed(response, error));
    }

    /**
     * <p>Performs a POST request to the specified URI.</p>
     * <p>The body of the request uses the application/x-www-form-urlencoded content type.</p>
     *
     * @param uri  the link of the request
     * @param body  the keys and values of the request body
     * @param referer  <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Referer">
     *                 the absolute or partial address from which a resource has been requested.</a>
     *                 It is needed for some requests
     * @param token  the <a href="https://docs.openmicroscopy.org/omero/5.6.0/developers/json-api.html#get-csrf-token">CSRF token</a>
     *               of the session
     */
    public static void post(URI uri, Map<String, String> body, String referer, String token) {
        request(getUrlEncodedPOSTRequest(
                uri,
                body,
                referer,
                token
        ));
    }

    /**
     * <p>Performs a POST request to the specified URI.</p>
     * <p>The body of the request uses the application/x-www-form-urlencoded content type.</p>
     *
     * @param uri  the link of the request
     * @param body  the keys and values of the request body, encoded to a byte array with the UTF 8 format.
     * @param referer  <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Referer">
     *                 the absolute or partial address from which a resource has been requested.</a>
     *                 It is needed for some requests
     * @param token  the <a href="https://docs.openmicroscopy.org/omero/5.6.0/developers/json-api.html#get-csrf-token">CSRF token</a>
     *               of the session
     * @return the raw HTTP response with in text format, or an empty Optional if the request failed
     */
    public static CompletableFuture<Optional<String>> post(URI uri, byte[] body, String referer, String token) {
        return request(getUrlEncodedPOSTRequest(
                uri,
                body,
                referer,
                token
        ));
    }

    /**
     * <p>Performs a POST request to the specified URI.</p>
     * <p>The body of the request uses the application/json content type.</p>
     *
     * @param uri  the link of the request
     * @param body  the keys and values of the request body with the JSON format.
     * @param referer  <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Referer">
     *                 the absolute or partial address from which a resource has been requested.</a>
     *                 It is needed for some requests
     * @param token  the <a href="https://docs.openmicroscopy.org/omero/5.6.0/developers/json-api.html#get-csrf-token">CSRF token</a>
     *               of the session
     * @return the raw HTTP response with in text format, or an empty Optional if the request failed
     */
    public static CompletableFuture<Optional<String>> post(URI uri, String body, String referer, String token) {
        return request(getJSONPOSTRequest(
                uri,
                body,
                referer,
                token
        ));
    }

    private static CompletableFuture<Optional<String>> request(HttpRequest request) {
        return httpClient
                .sendAsync(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                )
                .handle((response, error) -> {
                    boolean requestFailed = hasRequestFailed(request.uri(), response, error);
                    return requestFailed ? Optional.empty() : Optional.ofNullable(response.body());
                });
    }

    private static HttpRequest getGETRequest(URI uri) {
        return HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(Duration.of(REQUEST_TIMEOUT, SECONDS))
                .build();
    }

    private static List<JsonElement> readFollowingPages(String uri, int limit, int totalCount) {
        return IntStream.iterate(limit, i -> i + limit)
                .limit(max(0, (totalCount - limit) / limit))
                .mapToObj(offset -> RequestsUtilities.createURI(uri + "offset=" + offset).orElse(null))
                .filter(Objects::nonNull)
                .map(currentURI -> requestAndConvertToJsonList(currentURI, "data"))
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .toList();
    }

    private static boolean hasRequestFailed(HttpResponse<?> response, Throwable error) {
        return hasRequestFailed(null, response, error);
    }

    private static boolean hasRequestFailed(URI uri, HttpResponse<?> response, Throwable error) {
        if (error != null) {
            if (uri != null) {
                logger.error("Connection to " + uri + " failed", error);
            }
            return true;
        } else if (response.statusCode() != 200) {
            if (uri != null) {
                logger.error("Connection to " + uri + " failed. HTTP status code: " + response.statusCode());
            }
            return true;
        }
        return false;
    }

    private static HttpRequest getUrlEncodedPOSTRequest(URI uri, Map<String, String> body, String referer, String token) {
        return getPOSTRequest(
                uri,
                body.entrySet().stream()
                        .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                        .collect(Collectors.joining("&")),
                "application/x-www-form-urlencoded",
                referer,
                token
        );
    }

    private static HttpRequest getUrlEncodedPOSTRequest(URI uri, byte[] body, String referer, String token) {
        return getPOSTRequest(
                uri,
                body,
                referer,
                token
        );
    }

    private static HttpRequest getJSONPOSTRequest(URI uri, String body, String referer, String token) {
        return getPOSTRequest(uri, body, "application/json", referer, token);
    }

    private static HttpRequest getPOSTRequest(URI uri, String body, String contentType, String referer, String token) {
        return HttpRequest.newBuilder()
                .uri(uri)
                .headers(
                        "Content-Type", contentType,
                        "X-CSRFToken", token,
                        "Referer", referer
                )
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.of(REQUEST_TIMEOUT, SECONDS))
                .build();
    }

    private static HttpRequest getPOSTRequest(URI uri, byte[] body, String referer, String token) {
        return HttpRequest.newBuilder()
                .uri(uri)
                .headers(
                        "Content-Type", "application/x-www-form-urlencoded",
                        "X-CSRFToken", token,
                        "Referer", referer
                )
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .timeout(Duration.of(REQUEST_TIMEOUT, SECONDS))
                .build();
    }
}
