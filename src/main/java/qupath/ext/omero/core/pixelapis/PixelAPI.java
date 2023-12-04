package qupath.ext.omero.core.pixelapis;

import javafx.beans.value.ObservableBooleanValue;
import qupath.lib.images.servers.ImageServerMetadata;
import qupath.lib.images.servers.PixelType;

import java.io.IOException;

/**
 * This interface provides information (e.g. types of image supported) on a specific API to access
 * pixel values of an OMERO image. It can also be used to create a {@link PixelAPIReader}
 * corresponding to this API.
 */
public interface PixelAPI {

    /**
     * @return a human-readable name of this API
     */
    String getName();

    /**
     * @return arguments used internally by this pixel API
     */
    default String[] getArgs() {
        return new String[0];
    }

    /**
     * Change parameters of this API based on the provided arguments.
     *
     * @param args  the arguments containing parameters
     */
    default void setParametersFromArgs(String... args) {}

    /**
     * @return whether this API can be used. This property may be updated from any thread
     */
    ObservableBooleanValue isAvailable();

    /**
     * @return whether pixel values returned by this API are accurate (and not JPEG-compressed for example)
     */
    boolean canAccessRawPixels();

    /**
     * Indicates if an image with the provided parameters can be read by this API.
     * This method shouldn't need to be overridden.
     *
     * @param pixelType  the pixel type of the image
     * @param numberOfChannels  the number of channels of the image
     * @return whether the image can be read
     */
    default boolean canReadImage(PixelType pixelType, int numberOfChannels) {
        return canReadImage(pixelType) && canReadImage(numberOfChannels);
    }

    /**
     * Indicates if an image with the provided parameters can be read by this API.
     *
     * @param pixelType  the pixel type of the image
     * @return whether the image can be read
     */
    boolean canReadImage(PixelType pixelType);

    /**
     * Indicates if an image with the provided parameters can be read by this API.
     *
     * @param numberOfChannels  the number of channels of the image
     * @return whether the image can be read
     */
    boolean canReadImage(int numberOfChannels);

    /**
     * <p>
     *     Creates a {@link PixelAPIReader} corresponding to this API that will be
     *     used to read pixel values of an image.
     * </p>
     * <p>
     *     Note that you should {@link PixelAPIReader#close() close} this reader when it's
     *     no longer used.
     * </p>
     *
     * @param id  the ID of the image to open
     * @param metadata  the metadata of the image to open
     * @param allowSmoothInterpolation  whether to use smooth interpolation when resizing the image to open
     * @param nResolutions  the number of resolutions of the image to open
     * @return a new reader corresponding to this API
     * @throws IOException when the reader creation fails
     * @throws IllegalStateException when this API is not available (see {@link #isAvailable()})
     * @throws IllegalArgumentException when the provided image cannot be read by this API
     * (see {@link #canReadImage(PixelType, int)})
     */
    PixelAPIReader createReader(
            long id,
            ImageServerMetadata metadata,
            boolean allowSmoothInterpolation,
            int nResolutions
    ) throws IOException;
}
