/**
 * This package contains classes to communicate with the OMERO web API.
 * <ul>
 *     <li>
 *         The {@link qupath.lib.images.servers.omero.web.RequestSender RequestSender} class
 *         provides generic methods to send HTTP requests to a server.
 *     </li>
 *     <li>
 *         The {@link qupath.lib.images.servers.omero.web.ClientsPreferencesManager ClientsPreferencesManager}
 *         stores server information in a permanent way. This is useful for the user to not have to write again server URIs
 *         after each application restart.
 *     </li>
 *     <li>
 *         The {@link qupath.lib.images.servers.omero.web.WebClients WebClients} class handle all active connections.
 *         Connections should be created and removed using this class.
 *     </li>
 *     <li>
 *         The {@link qupath.lib.images.servers.omero.web.WebClient WebClient} class represents an active connection.
 *         It is used to perform any operation related to the related server.
 *     </li>
 *     <li>
 *         The {@link qupath.lib.images.servers.omero.web.apis apis} package contains requests that can made to an active
 *         connection.
 *     </li>
 *     <li>
 *         The {@link qupath.lib.images.servers.omero.web.entities entities} package contains classes representing objects
 *         required or returned by some requests.
 *     </li>
 *     <li>
 *         The {@link qupath.lib.images.servers.omero.web.pixelapis pixelapis} package contains different methods to read
 *         the pixel values of an image.
 *     </li>
 * </ul>
 */
package qupath.lib.images.servers.omero.web;