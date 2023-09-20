package qupath.lib.images.servers.omero.web.entities.repositoryentities.serverentities.image;

import com.google.gson.annotations.SerializedName;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import qupath.lib.images.servers.omero.gui.UiUtilities;
import qupath.lib.images.servers.omero.web.WebClient;
import qupath.lib.images.servers.omero.web.entities.repositoryentities.OrphanedFolder;
import qupath.lib.images.servers.omero.web.entities.repositoryentities.RepositoryEntity;
import qupath.lib.images.servers.omero.web.entities.repositoryentities.serverentities.ServerEntity;
import qupath.lib.images.servers.omero.web.entities.repositoryentities.serverentities.Dataset;

import java.util.Date;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * <p>
 *     Provides some information on an OMERO image.
 *     An image is a child of a {@link Dataset Dataset}
 *     or of an {@link OrphanedFolder OrphanedFolder}.
 * </p>
 * <p>
 *     This class uses the {@link PixelInfo} class to get information about pixels.
 * </p>
 */
public class Image extends ServerEntity {

    private static final ResourceBundle resources = UiUtilities.getResources();
    private static final String[] ATTRIBUTES = new String[] {
            resources.getString("Web.Entities.Image.name"),
            resources.getString("Web.Entities.Image.id"),
            resources.getString("Web.Entities.Image.owner"),
            resources.getString("Web.Entities.Image.group"),
            resources.getString("Web.Entities.Image.acquisitionDate"),
            resources.getString("Web.Entities.Image.imageWidth"),
            resources.getString("Web.Entities.Image.imageHeight"),
            resources.getString("Web.Entities.Image.nbChannels"),
            resources.getString("Web.Entities.Image.nbZSlices"),
            resources.getString("Web.Entities.Image.nbTimePoints"),
            resources.getString("Web.Entities.Image.pixelSizeX"),
            resources.getString("Web.Entities.Image.pixelSizeY"),
            resources.getString("Web.Entities.Image.pixelSizeZ"),
            resources.getString("Web.Entities.Image.pixelType")
    };
    private transient BooleanProperty isSupported;
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

    @Override
    public String toString() {
        return String.format("Image %s of ID %d: %s", name, id, pixels);
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
     * Set the web client of this image. This is needed to determine if this image
     * can be opened.
     *
     * @param client the web client of this image
     */
    public void setWebClient(WebClient client) {
        isSupported = new SimpleBooleanProperty();
        isSupported.bind(Bindings.createBooleanBinding(
                () -> client.getSelectedPixelAPI().get().canReadImage(isUint8(), has3Channels()),
                client.getSelectedPixelAPI()
        ));
    }

    /**
     * @return whether this image can be opened within QuPath
     * @throws IllegalStateException when the APIs handler has not been set (see {@link #setWebClient(WebClient)})
     */
    public ReadOnlyBooleanProperty isSupported() {
        if (isSupported == null) {
            throw new IllegalStateException(
                    "The web client has not been set on this dataset. See the setWebClient(WebClient) function."
            );
        }
        return isSupported;
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
