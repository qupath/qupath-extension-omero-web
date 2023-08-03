package qupath.lib.images.servers.omero.common.omeroentities.repositoryentities.serverentities.image;

import com.google.gson.annotations.SerializedName;
import javafx.collections.ObservableList;
import qupath.lib.images.servers.omero.common.gui.UiUtilities;
import qupath.lib.images.servers.omero.common.imagesservers.OmeroImageServer;
import qupath.lib.images.servers.omero.common.omeroentities.repositoryentities.RepositoryEntity;
import qupath.lib.images.servers.omero.common.omeroentities.repositoryentities.serverentities.ServerEntity;

import java.util.Date;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * <p>
 *     Provides some information on an OMERO image.
 *     An image is a child of a {@link qupath.lib.images.servers.omero.common.omeroentities.repositoryentities.serverentities.Dataset Dataset}
 *     or an {@link qupath.lib.images.servers.omero.common.omeroentities.repositoryentities.OrphanedFolder OrphanedFolder}.
 * </p>
 * <p>
 *     This class uses the {@link PixelInfo} class to get information about pixels.
 * </p>
 */
public class Image extends ServerEntity {
    private static final ResourceBundle resources = UiUtilities.getResources();
    private static final String[] ATTRIBUTES = new String[] {
            resources.getString("Common.OmeroEntities.Image.name"),
            resources.getString("Common.OmeroEntities.Image.id"),
            resources.getString("Common.OmeroEntities.Image.owner"),
            resources.getString("Common.OmeroEntities.Image.group"),
            resources.getString("Common.OmeroEntities.Image.acquisitionDate"),
            resources.getString("Common.OmeroEntities.Image.imageWidth"),
            resources.getString("Common.OmeroEntities.Image.imageHeight"),
            resources.getString("Common.OmeroEntities.Image.nbChannels"),
            resources.getString("Common.OmeroEntities.Image.nbZSlices"),
            resources.getString("Common.OmeroEntities.Image.nbTimePoints"),
            resources.getString("Common.OmeroEntities.Image.pixelSizeX"),
            resources.getString("Common.OmeroEntities.Image.pixelSizeY"),
            resources.getString("Common.OmeroEntities.Image.pixelSizeZ"),
            resources.getString("Common.OmeroEntities.Image.pixelType")
    };
    @SerializedName(value = "AcquisitionDate") private long acquisitionDate;
    @SerializedName(value = "Pixels") private PixelInfo pixels;

    @Override
    public ObservableList<RepositoryEntity> getChildren() {
        return childrenImmutable;
    }

    @Override
    public int getNumberOfChildren() {
        return 0;
    }

    @Override
    public String getType() {
        return "image";
    }

    @Override
    public String getAttributeInformation(int informationIndex) {
        if (informationIndex < ATTRIBUTES.length) {
            return ATTRIBUTES[informationIndex];
        } else {
            return "";
        }
    }

    @Override
    public String getValueInformation(int informationIndex) {
        return switch (informationIndex) {
            case 0 -> name == null || name.isEmpty() ? "-" : name;
            case 1 -> String.valueOf(getId());
            case 2 -> getOwner().getName();
            case 3 -> getGroup().getName();
            case 4 -> acquisitionDate == 0 ? "-" : new Date(acquisitionDate * 1000).toString();
            case 5 -> getImageDimensions().map(d -> d[0] + " px").orElse("-");
            case 6 -> getImageDimensions().map(d -> d[1] + " px").orElse("-");
            case 7 -> getImageDimensions().map(d -> String.valueOf(d[2])).orElse("-");
            case 8 -> getImageDimensions().map(d -> String.valueOf(d[3])).orElse("-");
            case 9 -> getImageDimensions().map(d -> String.valueOf(d[4])).orElse("-");
            case 10 -> getPhysicalSizeX().map(x -> x.getValue() + " " + x.getSymbol().orElse("")).orElse("-");
            case 11 -> getPhysicalSizeY().map(x -> x.getValue() + " " + x.getSymbol().orElse("")).orElse("-");
            case 12 -> getPhysicalSizeZ().map(x -> x.getValue() + " " + x.getSymbol().orElse("")).orElse("-");
            case 13 -> getPixelType().orElse("-");
            default -> "";
        };
    }

    @Override
    public int getNumberOfAttributes() {
        return ATTRIBUTES.length;
    }

    /**
     * Indicates if an OMERO entity type refers to an image
     *
     * @param type  the OMERO entity type
     * @return whether this type refers to an image
     */
    public static boolean isOfType(String type) {
        return "http://www.openmicroscopy.org/Schemas/OME/2016-06#Image".equalsIgnoreCase(type) || "Image".equalsIgnoreCase(type);
    }

    /**
     * @return whether this image can be opened within QuPath
     */
    public boolean isSupported() {
        return OmeroImageServer.canReadAllImages() || (isUint8() && has3Channels());
    }

    /**
     * @return whether this is an 8-bit image
     */
    public boolean isUint8() {
        return getPixelType().map(s -> s.equals("uint8")).orElse(false);
    }

    /**
     * @return whether this image has 3 channels (RGB)
     */
    public boolean has3Channels() {
        return getImageDimensions().filter(ints -> ints[2] == 3).isPresent();
    }

    private Optional<int[]> getImageDimensions() {
        if (pixels == null) {
            return Optional.empty();
        } else {
            return Optional.of(pixels.getImageDimensions());
        }
    }

    private Optional<PhysicalSize> getPhysicalSizeX() {
        return pixels == null ? Optional.empty() : pixels.getPhysicalSizeX();
    }

    private Optional<PhysicalSize> getPhysicalSizeY() {
        return pixels == null ? Optional.empty() : pixels.getPhysicalSizeY();
    }

    private Optional<PhysicalSize> getPhysicalSizeZ() {
        return pixels == null ? Optional.empty() : pixels.getPhysicalSizeZ();
    }

    private Optional<String> getPixelType() {
        return pixels == null ? Optional.empty() : pixels.getPixelType();
    }
}
