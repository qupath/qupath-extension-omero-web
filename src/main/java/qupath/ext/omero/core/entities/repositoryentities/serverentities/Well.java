package qupath.ext.omero.core.entities.repositoryentities.serverentities;

import com.google.gson.annotations.SerializedName;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import qupath.ext.omero.core.entities.repositoryentities.RepositoryEntity;
import qupath.ext.omero.core.entities.repositoryentities.serverentities.image.Image;
import qupath.ext.omero.gui.UiUtilities;

import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Represents an OMERO well.
 * A well acquisition contains {@link Image} ids.
 */
public class Well extends ServerEntity {

    private static final ResourceBundle resources = UiUtilities.getResources();
    private static final String[] ATTRIBUTES = new String[] {
            resources.getString("Web.Entities.Well.name"),
            resources.getString("Web.Entities.Well.id"),
            resources.getString("Web.Entities.Well.owner"),
            resources.getString("Web.Entities.Well.group"),
            resources.getString("Web.Entities.Well.column"),
            resources.getString("Web.Entities.Well.row")
    };
    @SerializedName(value = "WellSamples") private List<WellSample> wellSamples;
    @SerializedName(value = "Column") private int column;
    @SerializedName(value = "Row") private int row;

    /**
     * Creates an empty well only defined by its ID.
     */
    public Well(long id) {
        this.id = id;
    }

    @Override
    public boolean hasChildren() {
        return wellSamples != null && wellSamples.stream()
                .map(WellSample::getImage)
                .flatMap(Optional::stream)
                .toList()
                .size() > 0;
    }

    @Override
    public ObservableList<? extends RepositoryEntity> getChildren() {
        return FXCollections.emptyObservableList();
    }

    @Override
    public ReadOnlyStringProperty getLabel() {
        return new SimpleStringProperty(String.format("Column: %d, Row: %d", column, row));
    }

    @Override
    public boolean isPopulatingChildren() {
        return false;
    }

    @Override
    public String getAttributeName(int informationIndex) {
        if (informationIndex < ATTRIBUTES.length) {
            return ATTRIBUTES[informationIndex];
        } else {
            return "";
        }
    }

    @Override
    public String getAttributeValue(int informationIndex) {
        return switch (informationIndex) {
            case 0 -> name == null || name.isEmpty() ? "-" : name;
            case 1 -> String.valueOf(getId());
            case 2 -> getOwner().getFullName();
            case 3 -> getGroup().getName();
            case 4 -> String.valueOf(column);
            case 5 -> String.valueOf(row);
            default -> "";
        };
    }

    @Override
    public int getNumberOfAttributes() {
        return ATTRIBUTES.length;
    }

    @Override
    public String toString() {
        return String.format("Well of ID %d", id);
    }

    /**
     * Indicates if an OMERO entity type refers to a well.
     *
     * @param type  the OMERO entity type
     * @return whether this type refers to a well
     */
    public static boolean isWell(String type) {
        return "http://www.openmicroscopy.org/Schemas/OME/2016-06#Well".equalsIgnoreCase(type) || "Well".equalsIgnoreCase(type);
    }

    /**
     * Retrieve a list of image ids belonging to this well.
     *
     * @param withPlateAcquisition  whether to include image that are part of a plate acquisition
     * @return the image ids belonging to this well
     */
    public List<Long> getImagesIds(boolean withPlateAcquisition) {
        return wellSamples == null ? List.of() : wellSamples.stream()
                .filter(wellSample -> {
                    if (withPlateAcquisition) {
                        return wellSample.plateAcquisitionExists();
                    } else {
                        return !wellSample.plateAcquisitionExists();
                    }
                })
                .map(WellSample::getImage)
                .flatMap(Optional::stream)
                .map(Image::getId)
                .toList();
    }

    private static class WellSample {

        @SerializedName(value = "Image") private Image image;
        @SerializedName(value = "PlateAcquisition") private PlateAcquisition plateAcquisition;

        public Optional<Image> getImage() {
            return Optional.ofNullable(image);
        }

        public boolean plateAcquisitionExists() {
            return plateAcquisition != null;
        }
    }
}
