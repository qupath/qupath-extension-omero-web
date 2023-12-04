package qupath.ext.omero.core.entities.repositoryentities.serverentities;

import com.google.common.collect.Lists;
import com.google.gson.annotations.SerializedName;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import qupath.ext.omero.core.apis.ApisHandler;
import qupath.ext.omero.core.entities.repositoryentities.RepositoryEntity;
import qupath.ext.omero.core.entities.repositoryentities.serverentities.image.Image;
import qupath.ext.omero.gui.UiUtilities;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

/**
 * Represents an OMERO plate acquisition.
 * A plate acquisition contains {@link Image images}.
 */
public class PlateAcquisition extends ServerEntity {

    private static final ResourceBundle resources = UiUtilities.getResources();
    private static final String[] ATTRIBUTES = new String[] {
            resources.getString("Web.Entities.PlateAcquisition.name"),
            resources.getString("Web.Entities.PlateAcquisition.id"),
            resources.getString("Web.Entities.PlateAcquisition.owner"),
            resources.getString("Web.Entities.PlateAcquisition.group"),
            resources.getString("Web.Entities.PlateAcquisition.acquisitionTime")
    };
    private final transient ObservableList<Image> children = FXCollections.observableArrayList();
    private final transient ObservableList<Image> childrenImmutable = FXCollections.unmodifiableObservableList(children);
    private final transient StringProperty label = new SimpleStringProperty((this.name == null ? "" : this.name) + " (0)");
    private transient boolean childrenPopulated = false;
    private transient ApisHandler apisHandler;
    private transient boolean isPopulating = false;
    private transient int numberOfWells = 0;
    @SerializedName(value = "omero:wellsampleIndex") private List<Integer> wellSampleIndices;
    @SerializedName(value = "StartTime") private long startTime;

    /**
     * Creates an empty plate acquisition.
     */
    public PlateAcquisition() {
        // This constructor is declared because otherwise transient fields
        // of this class are not declared when it is created through JSON
    }

    /**
     * Creates an empty plate acquisition only defined by its ID.
     */
    public PlateAcquisition(long id) {
        this.id = id;
    }

    @Override
    public boolean hasChildren() {
        return numberOfWells > 0;
    }

    /**
     * @throws IllegalStateException when the APIs handler has not been set (see {@link #setApisHandler(ApisHandler)})
     */
    @Override
    public ObservableList<? extends RepositoryEntity> getChildren() {
        if (!childrenPopulated) {
            populateChildren();
            childrenPopulated = true;
        }
        return childrenImmutable;
    }

    @Override
    public ReadOnlyStringProperty getLabel() {
        return label;
    }

    @Override
    public boolean isPopulatingChildren() {
        return isPopulating;
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
            case 4 -> startTime == 0 ? "-" : new Date(startTime).toString();
            default -> "";
        };
    }

    @Override
    public int getNumberOfAttributes() {
        return ATTRIBUTES.length;
    }

    @Override
    public String toString() {
        return String.format("Plate acquisition %s of ID %d", name, id);
    }

    /**
     * Indicates if an OMERO entity type refers to a plate acquisition.
     *
     * @param type  the OMERO entity type
     * @return whether this type refers to a plate acquisition
     */
    public static boolean isPlateAcquisition(String type) {
        return "http://www.openmicroscopy.org/Schemas/OME/2016-06#PlateAcquisition".equalsIgnoreCase(type) || "PlateAcquisition".equalsIgnoreCase(type);
    }

    /**
     * Set the APIs handler for this plate acquisition. This is needed to populate its children.
     *
     * @param apisHandler the APIs handler of this browser
     */
    public void setApisHandler(ApisHandler apisHandler) {
        this.apisHandler = apisHandler;
    }

    /**
     * Set the number of wells of this plate acquisition.
     *
     * @param numberOfWells  the number of wells of this plate acquisition
     */
    public void setNumberOfWells(int numberOfWells) {
        this.numberOfWells = numberOfWells;
        label.set((this.name == null ? "" : this.name) + " (" + numberOfWells + ")");
    }

    private void populateChildren() {
        if (apisHandler == null) {
            throw new IllegalStateException(
                    "The APIs handler has not been set on this plate acquisition. See the setApisHandler(ApisHandler) function."
            );
        } else {
            isPopulating = true;

            int wellSampleIndex = 0;
            if (wellSampleIndices != null && wellSampleIndices.size() > 1) {
                wellSampleIndex = wellSampleIndices.get(0);
            }

            apisHandler.getWellsFromPlateAcquisition(getId(), wellSampleIndex).thenAcceptAsync(wells -> {
                List<Long> ids = wells.stream()
                        .map(well -> well.getImagesIds(true))
                        .flatMap(List::stream)
                        .toList();
                List<List<Long>> batches = Lists.partition(ids, 16);

                for (List<Long> batch: batches) {
                    children.addAll(batch.stream()
                            .map(id -> apisHandler.getImage(id))
                            .map(CompletableFuture::join)
                            .flatMap(Optional::stream)
                            .toList());
                }

                isPopulating = false;
            });
        }
    }
}
