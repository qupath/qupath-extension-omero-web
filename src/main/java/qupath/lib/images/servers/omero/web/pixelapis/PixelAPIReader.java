package qupath.lib.images.servers.omero.web.pixelapis;

import qupath.lib.images.servers.TileRequest;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * <p>
 *     This interface allows to read pixel values from a tile request.
 *     It should only be created by a {@link PixelAPI}.
 * </p>
 * <p>
 *     Once no longer used, any instance of this interface must be {@link #close() closed}.
 * </p>
 */
public interface PixelAPIReader extends AutoCloseable {
    /**
     * Read a tile of an image.
     *
     * @param tileRequest  the tile parameters
     * @return the resulting image
     * @throws IOException when a reading error occurs
     */
    BufferedImage readTile(TileRequest tileRequest) throws IOException;

    /**
     * @return a human-readable name of this API
     */
    String getName();
}
