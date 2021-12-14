/*-
 * #%L
 * This file is part of QuPath.
 * %%
 * Copyright (C) 2018 - 2021 QuPath developers, The University of Edinburgh
 * %%
 * QuPath is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * QuPath is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with QuPath.  If not, see <https://www.gnu.org/licenses/>.
 * #L%
 */

package qupath.lib.images.servers.omero;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonSyntaxException;

/**
 * Class to keep track of active OMERO clients.
 * 
 * @author Melvin Gelbard
 */
public class OmeroWebClients {
	
	/**
	 * Suppress default constructor for non-instantiability
	 */
	private OmeroWebClients() {
		throw new AssertionError();
	}

	final private static Logger logger = LoggerFactory.getLogger(OmeroWebClients.class);
	
	/**
	 * An observable list of active/non-active clients. The user may not necessarily be logged in.
	 */
	final private static List<OmeroWebClient> clients = new ArrayList<>();
	
	/**
	 * A set of potential hosts that don't correspond to valid OMERO web servers.
	 * This is used to avoid trying again.
	 */
	final private static Set<URI> failedUris = new HashSet<>();
	
	/**
	 * Authenticator used for getting credentials. Can either be a custom JavaFX one or a simple Java one.
	 */
	private static Authenticator authenticator = new QuPathAuthenticator();
	
	/**
	 * Return the client associated with the specified server URI. 
	 * If no client is found, {@code null} is returned.
	 * @param serverURI
	 * @return client
	 */
	static OmeroWebClient getClientFromServerURI(URI serverURI) {
		return clients.parallelStream().filter(e -> e.getServerURI().equals(serverURI)).findFirst().orElse(null);
	}
	
	/**
	 * Return the client associated with the specified image URI. 
	 * If no client is found, {@code null} is returned.
	 * @param imageURI
	 * @return client
	 */
	static OmeroWebClient getClientFromImageURI(URI imageURI) {
		return clients.parallelStream().filter(e -> e.getURIs().contains(imageURI)).findFirst().orElse(null);
	}
	
	/**
	 * Add a server (with an empty list of clients) to the client list. 
	 * Nothing happens if the client is already present in the list.
	 * @param client
	 */
	static void addClient(OmeroWebClient client) {
		client.checkIfLoggedIn();
		
		if (!clients.contains(client))
			clients.add(client);
		else
			logger.debug("Client already exists in the list. Ignoring operation.");
	}
	
	/**
	 * Remove the client from the clients list (losing all info about its URIs).
	 * @param client
	 */
	static void removeClient(OmeroWebClient client) {
		clients.remove(client);
	}

	/**
	 * Return whether the specified server URI was processed before 
	 * and had failed (to avoid unnecessary processing).
	 * @param serverURI
	 * @return hasFailed
	 */
	static boolean hasFailed(URI serverURI) {
		return failedUris.contains(serverURI);
	}
	
	/**
	 * Add the specified host to the list of failed hosts.
	 * @param serverURI
	 */
	static void addFailedHost(URI serverURI) {
		if (!failedUris.contains(serverURI))
			failedUris.add(serverURI);
		else
			logger.debug("URI already exists in the list. Ignoring operation.");
	}
	
	/**
	 * Return an unmodifiable list of all clients.
	 * @return client list
	 */
	static List<OmeroWebClient> getAllClients() {
		return Collections.unmodifiableList(clients);
	}
	
	/**
	 * Create client from the server URI provided.
	 * If login is required, it will prompt a dialog automatically.
	 * This method will also add it to the list of clients.
	 * 
	 * @param serverURI
	 * @return client
	 * @throws IOException 
	 * @throws URISyntaxException 
	 * @throws JsonSyntaxException 
	 */
	public static OmeroWebClient createClientAndLogin(URI serverURI) throws IOException, URISyntaxException {
		OmeroWebClient client = OmeroWebClient.create(serverURI, false);
		boolean loggedIn = true;
		if (!client.checkIfLoggedIn())
			loggedIn = client.logIn();
		
		if (!loggedIn)
			return null;
		
		client.startTimer();
		addClient(client);
		return client;
	}
	
	/**
	 * Set the authenticator to use for client login.
	 * @param authenticator
	 */
	static void setAuthenticator(Authenticator authenticator) {
		OmeroWebClients.authenticator = authenticator;
	}
	
	/**
	 * Get the authenticator to use for client login.
	 * @return authenticator
	 */
	static Authenticator getAuthenticator() {
		return authenticator;
	}
	
	private static class QuPathAuthenticator extends Authenticator {

		@Override
		protected PasswordAuthentication getPasswordAuthentication() {
			System.out.println(getRequestingPrompt() + ": " + getRequestingHost());
			System.out.print("Username: ");
			String username = System.console().readLine();
			System.out.print("Password: ");
			char[] pass = System.console().readPassword();
			return new PasswordAuthentication(username, pass);
		}
	}
}
