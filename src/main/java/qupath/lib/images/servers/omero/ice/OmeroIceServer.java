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

package qupath.lib.images.servers.omero.ice;

import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import Glacier2.CannotCreateSessionException;
import Glacier2.PermissionDeniedException;
import ome.model.units.BigResult;
import omero.ServerError;
import omero.gateway.Gateway;
import omero.gateway.JoinSessionCredentials;
import omero.gateway.SecurityContext;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.exception.DataSourceException;
import omero.gateway.facility.BrowseFacility;
import omero.gateway.facility.MetadataFacility;
import omero.gateway.facility.RawDataFacility;
import omero.gateway.model.ChannelData;
import omero.gateway.model.ExperimenterData;
import omero.gateway.model.ImageAcquisitionData;
import omero.gateway.model.ImageData;
import omero.gateway.model.PixelsData;
import omero.gateway.rnd.Plane2D;
import omero.log.SimpleLogger;
import omero.model.enums.UnitsLength;
import qupath.lib.color.ColorModelFactory;
import qupath.lib.common.ColorTools;
import qupath.lib.images.servers.AbstractTileableImageServer;
import qupath.lib.images.servers.ImageChannel;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.ImageServerBuilder;
import qupath.lib.images.servers.ImageServerBuilder.ServerBuilder;
import qupath.lib.images.servers.ImageServerMetadata;
import qupath.lib.images.servers.ImageServerMetadata.ImageResolutionLevel;
import qupath.lib.images.servers.PixelType;
import qupath.lib.images.servers.TileRequest;
import qupath.lib.images.servers.omero.OmeroTools;
import qupath.lib.images.servers.omero.OmeroWebClients;
import qupath.lib.images.servers.omero.OmeroWebImageServer;

/**
 * {@link ImageServer} implementation using the OMERO Java Gateway.
 * <p>
 * This requires the OMERO Gateway dependencies to be available on the classpath.
 * Its advantage over {@link OmeroWebImageServer} is that it enables raw pixel access.
 * 
 * @author Pete Bankhead
 */
public class OmeroIceServer extends AbstractTileableImageServer {
	
	private final static Logger logger = LoggerFactory.getLogger(OmeroIceServer.class);
	
	private URI uri;
	private String[] args;
	private ImageServerMetadata originalMetatada;
	
	private GatewayWrapper gateway;
	
	OmeroIceServer(URI uri, String...args) throws IOException {
		super();
		this.uri = uri;
		this.args = args;
		try {
			this.gateway = new GatewayWrapper(uri);
			this.originalMetatada = gateway.buildMetadata();
		} catch (Exception e) {
			throw new IOException("Unable to create OMERO Gateway", e);
		}
	}

	@Override
	public Collection<URI> getURIs() {
		return Collections.singletonList(uri);
	}

	@Override
	public String getServerType() {
		return "Omero Server (Ice)";
	}

	@Override
	public ImageServerMetadata getOriginalMetadata() {
		return originalMetatada;
	}

	@Override
	protected BufferedImage readTile(TileRequest tileRequest) throws IOException {
		try {
			return gateway.getPixels(tileRequest);
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	@Override
	protected ServerBuilder<BufferedImage> createServerBuilder() {
		return ImageServerBuilder.DefaultImageServerBuilder.createInstance(
				OmeroIceImageServerBuilder.class,
				getMetadata(),
				uri,
				args);
	}

	@Override
	protected String createID() {
		// TODO: Consider incorporating args (also for web)
		return getClass().getName() + ": " + uri.toString();
	}
	
	
	private final static Map<String, PixelType> pixelTypes = Map.of(
			PixelsData.UINT8_TYPE, PixelType.UINT8,
			PixelsData.INT8_TYPE, PixelType.INT8,
			PixelsData.UINT16_TYPE, PixelType.UINT16,
			PixelsData.INT16_TYPE, PixelType.INT16,
			PixelsData.INT32_TYPE, PixelType.INT32,
			PixelsData.UINT32_TYPE, PixelType.UINT32,
			PixelsData.FLOAT_TYPE, PixelType.FLOAT32,
			PixelsData.DOUBLE_TYPE, PixelType.FLOAT64
			);

	
	private static Map<String, String> uuidMap = new HashMap<>();

	private class GatewayWrapper {
		
		private Gateway gateway;
		private long imageID;
		private SecurityContext ctx;
		private ImageData image;
		private PixelsData pixels;
		private ImageAcquisitionData acquisition;
		private List<ChannelData> channelData;
				
		GatewayWrapper(URI uri) throws DSOutOfServiceException, DSAccessException, ExecutionException, IOException, URISyntaxException, CannotCreateSessionException, PermissionDeniedException, ServerError {
			gateway = new Gateway(new SimpleLogger());
			
			// Try to use existing web client login
			// If we can get the session UUID, then we don't need a username & password
			var client = OmeroWebClients.createClientAndLogin(uri);
			String userName = client.getSessionUUID();
			if (userName != null)
				uuidMap.put(uri.getHost(), userName);
			else
				userName = uuidMap.getOrDefault(uri.getHost(), null);
			
			var credentials = new JoinSessionCredentials(userName, uri.getHost());

			logger.debug("Attempting to connect to {}", uri.getHost());
			ExperimenterData user = null;
			try {
				user = gateway.connect(credentials);
			} catch (Exception e) {
				logger.error("Unable to connect to OMERO server: " + e.getLocalizedMessage());
				throw new IOException(e);
			}
			logger.debug("Connected to {}", uri.getHost());
			
	        ctx = new SecurityContext(user.getGroupId());
	        
	        var browse = gateway.getFacility(BrowseFacility.class);
	        
	        imageID = OmeroTools.parseImageId(uri);
	        image = browse.getImage(ctx, imageID);
	        pixels = image.getDefaultPixels();
	        
	        var mf = gateway.getFacility(MetadataFacility.class);
	        acquisition = mf.getImageAcquisitionData(ctx, imageID);
	        
	        channelData = mf.getChannelData(ctx, imageID);
		}
				
		private ImageServerMetadata buildMetadata() throws ExecutionException, DataSourceException {
			var channels = new ArrayList<ImageChannel>(
					ImageChannel.getDefaultChannelList(channelData.size())
					);
			
			int count = 0;
			for (var cd : channelData) {
				String name = cd.getName();
				var c = cd.asChannel();
				Integer color = ColorTools.packRGB(
						c.getRed().getValue(),
						c.getGreen().getValue(),
						c.getBlue().getValue()
						);
				if (name != null && color != null) {
					var channel = ImageChannel.getInstance(name,  color);
					channels.set(count, channel);					
				}
				count++;
			}
			
			PixelType pixelType = pixelTypes.getOrDefault(pixels.getPixelType(), null);
			if (pixelType == null) {
				throw new UnsupportedOperationException("Unsupported pixel type: " + pixels.getPixelType());
			}
			
			var builder = new ImageServerMetadata.Builder()
					.name("Anything")
					.width(pixels.getSizeX())
					.height(pixels.getSizeY())
					.sizeZ(pixels.getSizeZ())
					.sizeT(pixels.getSizeT())
					.pixelType(pixelType)
					.channels(channels);
			
			if (channels.size() == 3 && 
					ImageChannel.getDefaultRGBChannels().equals(channels))
				builder.rgb(true);

			// Handle pyramid levels
			try (var rdf = gateway.getFacility(RawDataFacility.class)) {
				var resDescriptions = rdf.getResolutionDescriptions(ctx, pixels);
				if (resDescriptions.size() > 1) {
					var resolutionsBuilder = new ImageResolutionLevel.Builder(pixels.getSizeX(), pixels.getSizeY());
					for (var res : resDescriptions) {
						resolutionsBuilder.addLevel(res.sizeX, res.sizeY);
					}
					builder.levels(resolutionsBuilder.build());
				}
			}
			
			var objective = acquisition.getObjective();
			double mag = Double.NaN;
			if (objective != null) {
				mag = objective.getCalibratedMagnification();
				if (Double.isNaN(mag)) {
					mag = objective.getNominalMagnification();
					if (!Double.isNaN(mag))
						logger.info("Using nominal magnification: {}", mag);
				} else {
					logger.info("Using calibrated magnification: {}", mag);
				}
				if (Double.isFinite(mag))
					builder = builder.magnification(mag);
			}
			
			try {
				var pixelWidth = pixels.getPixelSizeX(UnitsLength.MICROMETER);
				var pixelHeight = pixels.getPixelSizeY(UnitsLength.MICROMETER);
				if (pixelWidth != null && pixelHeight != null)
					builder.pixelSizeMicrons(pixelWidth.getValue(), pixelHeight.getValue());
				var pixelDepth = pixels.getPixelSizeZ(UnitsLength.MICROMETER);
				if (pixelDepth != null)
					builder.zSpacingMicrons(pixelDepth.getValue());				
			} catch (BigResult e) {
				logger.warn("Unable to parse pixel sizes: " + e.getLocalizedMessage(), e);
			}
			
			return builder.build();
		}
		
		private BufferedImage getPixels(TileRequest request) throws ServerError, DSOutOfServiceException, DataSourceException, ExecutionException {
			
			var channels = getMetadata().getChannels();
			var colorModel = ColorModelFactory.createColorModel(getPixelType(), channels);
			int nChannels = channels.size();
			
			int w = request.getTileWidth();
			int h = request.getTileHeight();
			
			if (isRGB() && nChannels == 3) {
				var img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
				int[] rgb = new int[w*h];
				try (var rdf = gateway.getFacility(RawDataFacility.class)) {
					
					var planeRed = rdf.getTile(ctx, pixels,
							request.getZ(),
							request.getT(),
							0, // Channel
							request.getTileX(),
							request.getTileY(),
							request.getTileWidth(),
							request.getTileHeight(),
							request.getLevel()
							);
					
					var planeGreen = rdf.getTile(ctx, pixels,
							request.getZ(),
							request.getT(),
							1, // Channel
							request.getTileX(),
							request.getTileY(),
							request.getTileWidth(),
							request.getTileHeight(),
							request.getLevel()
							);
					
					var planeBlue = rdf.getTile(ctx, pixels,
							request.getZ(),
							request.getT(),
							2, // Channel
							request.getTileX(),
							request.getTileY(),
							request.getTileWidth(),
							request.getTileHeight(),
							request.getLevel()
							);
					
					int ind = 0;
					for (int y = 0; y < h; y++) {
						for (int x = 0; x < w; x++) {
							rgb[ind] = ColorTools.packRGB(
									planeRed.getRawValue(ind),
									planeGreen.getRawValue(ind),
									planeBlue.getRawValue(ind));
							ind++;
						}
					}
				}
				img.setRGB(0, 0, w, h, rgb, 0, w);
				return img;
			}

			
			
			DataBuffer buffer;
			switch (getPixelType()) {
			case FLOAT32:
				buffer = new DataBufferFloat(w*h, nChannels);
				break;
			case FLOAT64:
				buffer = new DataBufferDouble(w*h, nChannels);
				break;
			case INT16:
				buffer = new DataBufferShort(w*h, nChannels);
				break;
			case INT32:
				buffer = new DataBufferInt(w*h, nChannels);
				break;
			case INT8:
				// Short should work
				buffer = new DataBufferShort(w*h, nChannels);
				break;
			case UINT16:
				buffer = new DataBufferUShort(w*h, nChannels);
				break;
			case UINT32:
				// Float likely to work
				buffer = new DataBufferFloat(w*h, nChannels);
				break;
			case UINT8:
				buffer = new DataBufferByte(w*h, nChannels);
				break;
			default:
				throw new UnsupportedOperationException("Unsupported pixel type " + getPixelType());
			}
			
			var sampleModel = new BandedSampleModel(buffer.getDataType(),
					w, h, nChannels);
			
			try (var rdf = gateway.getFacility(RawDataFacility.class)) {
					for (int b = 0; b < nChannels; b++) {
						Plane2D plane;
//						synchronized (rdf) {
							plane = rdf.getTile(ctx, pixels,
									request.getZ(),
									request.getT(),
									b, // Channel
									request.getTileX(),
									request.getTileY(),
									request.getTileWidth(),
									request.getTileHeight(),
									nResolutions() - request.getLevel() - 1
									);
							for (int i = 0; i < w*h; i++)
								buffer.setElemDouble(b, i, plane.getPixelValue(i%w, i/w));
//						}
				}
			}
			
			var raster = WritableRaster.createWritableRaster(sampleModel, buffer, null);
			return new BufferedImage(colorModel, raster, false, null);
		}
		
	}
	

}
