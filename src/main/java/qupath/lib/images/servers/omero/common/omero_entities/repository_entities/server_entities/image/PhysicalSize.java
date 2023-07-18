package qupath.lib.images.servers.omero.common.omero_entities.repository_entities.server_entities.image;

import com.google.gson.annotations.SerializedName;

import java.util.Optional;

class PhysicalSize {
    @SerializedName(value = "Symbol") private String symbol;
    @SerializedName(value = "Value") private double value;

    public Optional<String> getSymbol() {
        return Optional.ofNullable(symbol);
    }

    public double getValue() {
        return value;
    }
}
