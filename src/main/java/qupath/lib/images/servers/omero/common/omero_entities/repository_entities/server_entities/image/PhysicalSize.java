package qupath.lib.images.servers.omero.common.omero_entities.repository_entities.server_entities.image;

import com.google.gson.annotations.SerializedName;

import java.util.Optional;

/**
 * This class holds information about a physical size (value and unit).
 */
class PhysicalSize {
    @SerializedName(value = "Symbol") private String symbol;
    @SerializedName(value = "Value") private double value;

    /**
     * @return the unit of the size, or an empty Optional if not found
     */
    public Optional<String> getSymbol() {
        return Optional.ofNullable(symbol);
    }

    /**
     * @return the value of the size, or 0 if not found
     */
    public double getValue() {
        return value;
    }
}
