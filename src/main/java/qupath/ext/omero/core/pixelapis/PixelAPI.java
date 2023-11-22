package qupath.ext.omero.core.pixelapis;

import javafx.beans.property.ReadOnlyBooleanProperty;
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
     *
     * @param isUint8  whether the image type is 8-bit unsigned integer
     * @param has3Channels  whether the image has exactly 3 channels
     * @return whether the image can be read
     */
    boolean canReadImage(boolean isUint8, boolean has3Channels);

    /**
     * Indicates if an image with the provided parameter can be read by this API.
     * This function shouldn't need to be overridden.
     *
     * @param metadata  the metadata of the image
     * @return whether the image can be read
     */
    default boolean canReadImage(ImageServerMetadata metadata) {
        return canReadImage(metadata.getPixelType().equals(PixelType.UINT8), metadata.getChannels().size() == 3);
    }

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
     * @param args  optional arguments
     * @return a new reader corresponding to this API
     * @throws IOException when the reader creation fails
     * @throws IllegalStateException when this API is not available (see {@link #isAvailable()})
     * @throws IllegalArgumentException when the provided image cannot be read by this API
     * (see {@link #canReadImage(ImageServerMetadata)} or {@link #canReadImage(boolean, boolean)})
     */
    PixelAPIReader createReader(
            long id,
            ImageServerMetadata metadata,
            boolean allowSmoothInterpolation,
            int nResolutions,
            String... args
    ) throws IOException;
}
