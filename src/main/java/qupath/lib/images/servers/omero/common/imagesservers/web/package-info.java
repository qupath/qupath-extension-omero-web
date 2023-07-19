/**
 * <p>
 *     This package contains the {@link qupath.lib.images.servers.ImageServer image servers} and
 *     {@link qupath.lib.images.servers.ImageServerBuilder image builders} using the web API.
 * </p>
 * <p>
 *     This image server doesn't have dependencies but can only work with 8-bit RGB images,
 *     and the images are JPEG-compressed, so there is no accurate access of raw pixel data.
 * </p>
 */
package qupath.lib.images.servers.omero.common.imagesservers.web;