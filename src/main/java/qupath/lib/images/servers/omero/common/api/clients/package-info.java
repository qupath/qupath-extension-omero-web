/**
 * <p>This package contains classes that represent connections with a server.</p>
 * <p>
 *     The {@link qupath.lib.images.servers.omero.common.api.clients.ClientsPreferencesManager ClientsPreferencesManager}
 *     stores server information in a permanent way. This is useful for the user to not have to write again server URIs
 *     after each application restart.
 * </p>
 * <p>
 *     The {@link qupath.lib.images.servers.omero.common.api.clients.WebClients WebClients} class handle all active connections.
 *     Connections should be created and removed using this class.
 * </p>
 * <p>
 *     The {@link qupath.lib.images.servers.omero.common.api.clients.WebClient WebClient} class represents an active connection.
 *     It is used to perform any operation related to the related server.
 * </p>
 */
package qupath.lib.images.servers.omero.common.api.clients;