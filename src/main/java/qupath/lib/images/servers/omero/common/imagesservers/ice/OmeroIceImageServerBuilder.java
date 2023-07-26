/*-
 * #%L
 * This file is part of QuPath.
 * %%
 * Copyright (C) 2022 QuPath developers, The University of Edinburgh
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

package qupath.lib.images.servers.omero.common.imagesservers.ice;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.ImageServerBuilder;
import qupath.lib.images.servers.omero.common.api.requests.RequestsUtilities;


/**
 * {@link ImageServerBuilder} for use with {@link OmeroIceServer}.
 *
 * @author Pete Bankhead
 */
public class OmeroIceImageServerBuilder implements ImageServerBuilder<BufferedImage> {

	private final static Logger logger = LoggerFactory.getLogger(OmeroIceImageServerBuilder.class);

	private static Boolean gatewayAvailable = null;

	@Override
	public UriImageSupport<BufferedImage> checkImageSupport(URI uri, String... args) throws IOException {
		// TODO Better check validity of URL for OMERO server
		if (!isGatewayAvailable()) {
			logger.debug("OMERO Ice gateway is not available");
			return UriImageSupport.createInstance(this.getClass(), 0f, Collections.emptyList());
		}

		var id = RequestsUtilities.parseEntityId(uri);
		float supportLevel = 0f;
		if (id.isPresent()) {
			supportLevel = RequestsUtilities.getServerURI(uri).isEmpty() ? 0f : 5f;
		}
		var builders = new ArrayList<ServerBuilder<BufferedImage>>();
		if (supportLevel > 0f) {
			try {
				var builder = buildServer(uri, args).getBuilder();
				if (builder != null)
					builders.add(builder);
			} catch (Exception e) {
				logger.debug(e.getLocalizedMessage(), e);
			}
		}
		return UriImageSupport.createInstance(this.getClass(), supportLevel, builders);
	}


	/**
	 * Check if the OMERO Gateway is available, using reflection.
	 * @return
	 */
	private static boolean isGatewayAvailable() {
		if (gatewayAvailable == null) {
			Class<?> cls;
			try {
				cls = Class.forName("omero.gateway.Gateway");
				gatewayAvailable = cls != null;
			} catch (ClassNotFoundException e) {
				gatewayAvailable = Boolean.FALSE;
				logger.debug("OMERO Ice gateway is unavailable ('omero.gateway.Gateway' not found)");
			}
		}
		return gatewayAvailable.booleanValue();
	}


	@Override
	public ImageServer<BufferedImage> buildServer(URI uri, String... args) throws Exception {
		return new OmeroIceServer(uri, args);
	}

	@Override
	public String getName() {
		return "OMERO Ice";
	}

	@Override
	public String getDescription() {
		return "Image server using the OMERO Ice API (can access raw pixels)";
	}

	@Override
	public Class<BufferedImage> getImageType() {
		return BufferedImage.class;
	}

}