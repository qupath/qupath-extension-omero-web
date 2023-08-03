package qupath.lib.images.servers.omero.common.imagesservers.pixelsapis;

import qupath.lib.images.servers.TileRequest;

import java.awt.image.BufferedImage;

public interface PixelAPI {
    BufferedImage readTile(TileRequest tileRequest);
}
