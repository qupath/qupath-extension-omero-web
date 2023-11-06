package qupath.ext.omero.core.entities.repositoryentities.serverentities.image;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;
import java.util.Optional;

/**
 * This class holds information about a physical size (value and unit).
 */
class PhysicalSize {

    @SerializedName(value = "Symbol") private String symbol;
    @SerializedName(value = "Value") private double value;

    public PhysicalSize(String symbol, double value) {
        this.symbol = symbol;
        this.value = value;
    }

    @Override
    public String toString() {
        return String.format("Size: %f %s", value, symbol);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof PhysicalSize physicalSize))
            return false;
        return physicalSize.symbol.equals(symbol) && physicalSize.value == value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, value);
    }

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
