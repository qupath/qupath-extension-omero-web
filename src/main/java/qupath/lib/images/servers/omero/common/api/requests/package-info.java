/**
 * <p>This package contains classes that perform requests to the web API.</p>
 * <p>
 *     The {@link qupath.lib.images.servers.omero.common.api.requests.Requests Requests} class
 *     provides generic methods to send HTTP requests to a server.
 * </p>
 * <p>
 *     It is used by the {@link qupath.lib.images.servers.omero.common.api.requests.RequestsHandler RequestsHandler} class
 *     that provides functions to perform operations with an OMERO server.
 * </p>
 * <p>
 *     The RequestsHandler uses different APIs depending on the operation. These APIs are all in the
 *     {@link qupath.lib.images.servers.omero.common.api.requests.apis apis} package.
 * </p>
 * <p>
 *     Some operations of the RequestsHandler require or return complex data. Classes describing this data
 *     are contained in the {@link qupath.lib.images.servers.omero.common.api.requests.entities entities} package.
 * </p>
 * <p>
 *     The {@link qupath.lib.images.servers.omero.common.api.requests.RequestsUtilities RequestsUtilities} class
 *     contains useful utility functions to help using the web API.
 * </p>
 */
package qupath.lib.images.servers.omero.common.api.requests;