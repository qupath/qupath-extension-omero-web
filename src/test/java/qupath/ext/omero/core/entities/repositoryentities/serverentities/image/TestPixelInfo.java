package qupath.ext.omero.core.entities.repositoryentities.serverentities.image;

import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

public class TestPixelInfo {

    @Test
    void Check_Empty() {
        PixelInfo pixelInfo = new Gson().fromJson("{}", PixelInfo.class);

        Optional<String> symbol = pixelInfo.getPixelType();

        Assertions.assertTrue(symbol.isEmpty());
    }

    @Test
    void Check_Dimensions() {
        PixelInfo pixelInfo = createPixelInfo();
        int[] expectedDimensions = new int[] {1234, 789, 12, 3, 2};

        int[] dimensions = pixelInfo.getImageDimensions();

        Assertions.assertArrayEquals(expectedDimensions, dimensions);
    }

    @Test
    void Check_Physical_Size_X() {
        PixelInfo pixelInfo = createPixelInfo();
        PhysicalSize expectedPhysicalSizeX = new PhysicalSize("μm", 45.63);

        PhysicalSize physicalSizeX = pixelInfo.getPhysicalSizeX().orElse(null);

        Assertions.assertEquals(expectedPhysicalSizeX, physicalSizeX);
    }

    @Test
    void Check_Physical_Size_Y() {
        PixelInfo pixelInfo = createPixelInfo();
        PhysicalSize expectedPhysicalSizeY = new PhysicalSize("μm", 87.2);

        PhysicalSize physicalSizeY = pixelInfo.getPhysicalSizeY().orElse(null);

        Assertions.assertEquals(expectedPhysicalSizeY, physicalSizeY);
    }

    @Test
    void Check_Physical_Size_Z() {
        PixelInfo pixelInfo = createPixelInfo();
        PhysicalSize expectedPhysicalSizeZ = new PhysicalSize("mm", 1.2);

        PhysicalSize physicalSizeZ = pixelInfo.getPhysicalSizeZ().orElse(null);

        Assertions.assertEquals(expectedPhysicalSizeZ, physicalSizeZ);
    }

    @Test
    void Check_Pixel_Type() {
        PixelInfo pixelInfo = createPixelInfo();
        String expectedPixelType = "float";

        String pixelType = pixelInfo.getPixelType().orElse("");

        Assertions.assertEquals(expectedPixelType, pixelType);
    }

    private PixelInfo createPixelInfo() {
        String json = """
                {
                    "SizeX": 1234,
                    "SizeY": 789,
                    "SizeZ": 12,
                    "SizeC": 3,
                    "SizeT": 2,
                    "PhysicalSizeX": {
                        "Symbol": "μm",
                        "Value": 45.63
                    },
                    "PhysicalSizeY": {
                        "Symbol": "μm",
                        "Value": 87.2
                    },
                    "PhysicalSizeZ": {
                        "Symbol": "mm",
                        "Value": 1.2
                    },
                    "Type": {
                        "value": "float"
                    }
                }
                """;
        return new Gson().fromJson(json, PixelInfo.class);
    }
}
