package qupath.ext.omero.core.entities.imagemetadata;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import qupath.lib.common.ColorTools;
import qupath.lib.images.servers.ImageChannel;
import qupath.lib.images.servers.ImageServerMetadata;
import qupath.lib.images.servers.PixelType;

import java.util.List;
import java.util.Optional;

public class TestImageMetadataResponse {

    @Test
    void Check_Response_When_Empty() {
        JsonObject jsonObject = new JsonObject();

        Optional<ImageMetadataResponse> imageMetadataResponse = ImageMetadataResponse.createFromJson(jsonObject);

        Assertions.assertTrue(imageMetadataResponse.isEmpty());
    }

    @Test
    void Check_Image_Name() {
        ImageMetadataResponse imageMetadataResponse = createImageMetadataResponse();

        String imageName = imageMetadataResponse.getImageName();

        Assertions.assertEquals("LuCa-7color_[17572,60173]_3x3component_data.tif [resolution #1]", imageName);
    }

    @Test
    void Check_Size_X() {
        ImageMetadataResponse imageMetadataResponse = createImageMetadataResponse();

        int sizeX = imageMetadataResponse.getSizeX();

        Assertions.assertEquals(5604, sizeX);
    }

    @Test
    void Check_Size_Z() {
        ImageMetadataResponse imageMetadataResponse = createImageMetadataResponse();

        int sizeZ = imageMetadataResponse.getSizeZ();

        Assertions.assertEquals(1, sizeZ);
    }

    @Test
    void Check_Size_T() {
        ImageMetadataResponse imageMetadataResponse = createImageMetadataResponse();

        int sizeT = imageMetadataResponse.getSizeT();

        Assertions.assertEquals(1, sizeT);
    }

    @Test
    void Check_Tile_Size_X() {
        ImageMetadataResponse imageMetadataResponse = createImageMetadataResponse();

        int tileSizeX = imageMetadataResponse.getTileSizeX();

        Assertions.assertEquals(512, tileSizeX);
    }

    @Test
    void Check_Levels() {
        ImageMetadataResponse imageMetadataResponse = createImageMetadataResponse();

        List<ImageServerMetadata.ImageResolutionLevel> levels = imageMetadataResponse.getLevels();

        Assertions.assertArrayEquals(
                new ImageServerMetadata.ImageResolutionLevel.Builder(5604, 4200)
                        .addLevelByDownsample(1)
                        .addLevelByDownsample(1 / 0.5)
                        .addLevelByDownsample(1 / 0.25)
                        .build()
                        .toArray(),
                levels.toArray()
        );
    }

    @Test
    void Check_PixelType() {
        ImageMetadataResponse imageMetadataResponse = createImageMetadataResponse();

        PixelType pixelType = imageMetadataResponse.getPixelType();

        Assertions.assertEquals(PixelType.FLOAT32, pixelType);
    }

    @Test
    void Check_Channels() {
        ImageMetadataResponse imageMetadataResponse = createImageMetadataResponse();

        List<ImageChannel> channels = imageMetadataResponse.getChannels();

        Assertions.assertArrayEquals(
                new ImageChannel[]{
                        ImageChannel.getInstance(
                                "PDL1 (Opal 520)",
                                ColorTools.packRGB(
                                        Integer.valueOf("FF", 16),
                                        Integer.valueOf("00", 16),
                                        Integer.valueOf("00", 16)
                                )
                        ),
                        ImageChannel.getInstance(
                                "CD8 (Opal 540)",
                                ColorTools.packRGB(
                                        Integer.valueOf("FF", 16),
                                        Integer.valueOf("FF", 16),
                                        Integer.valueOf("00", 16)
                                )
                        ), ImageChannel.getInstance(
                        "FoxP3 (Opal 570)",
                        ColorTools.packRGB(
                                Integer.valueOf("FF", 16),
                                Integer.valueOf("80", 16),
                                Integer.valueOf("00", 16)
                        )
                ),
                        ImageChannel.getInstance(
                                "CD68 (Opal 620)",
                                ColorTools.packRGB(
                                        Integer.valueOf("FF", 16),
                                        Integer.valueOf("00", 16),
                                        Integer.valueOf("FF", 16)
                                )
                        ),
                        ImageChannel.getInstance(
                                "PD1 (Opal 650)",
                                ColorTools.packRGB(
                                        Integer.valueOf("00", 16),
                                        Integer.valueOf("FF", 16),
                                        Integer.valueOf("00", 16)
                                )
                        ),
                        ImageChannel.getInstance(
                                "CK (Opal 690)",
                                ColorTools.packRGB(
                                        Integer.valueOf("00", 16),
                                        Integer.valueOf("FF", 16),
                                        Integer.valueOf("FF", 16)
                                )
                        ),
                        ImageChannel.getInstance(
                                "DAPI",
                                ColorTools.packRGB(
                                        Integer.valueOf("00", 16),
                                        Integer.valueOf("00", 16),
                                        Integer.valueOf("FF", 16)
                                )
                        ),
                        ImageChannel.getInstance(
                                "Autofluorescence",
                                ColorTools.packRGB(
                                        Integer.valueOf("00", 16),
                                        Integer.valueOf("00", 16),
                                        Integer.valueOf("00", 16)
                                )
                        )
                },
                channels.toArray()
        );
    }

    @Test
    void Check_Is_RGB() {
        ImageMetadataResponse imageMetadataResponse = createImageMetadataResponse();

        boolean isRGB = imageMetadataResponse.isRGB();

        Assertions.assertFalse(isRGB);
    }

    @Test
    void Check_Magnification() {
        ImageMetadataResponse imageMetadataResponse = createImageMetadataResponse();

        double magnification = imageMetadataResponse.getMagnification().orElse(-1.);

        Assertions.assertEquals(10, magnification);
    }

    @Test
    void Check_Pixel_Width() {
        ImageMetadataResponse imageMetadataResponse = createImageMetadataResponse();

        double pixelWidth = imageMetadataResponse.getPixelWidthMicrons().orElse(-1.);

        Assertions.assertEquals(0.49799447890790055, pixelWidth);
    }

    @Test
    void Check_Z_Spacing() {
        ImageMetadataResponse imageMetadataResponse = createImageMetadataResponse();

        Optional<Double> zSpacing = imageMetadataResponse.getZSpacingMicrons();

        Assertions.assertTrue(zSpacing.isEmpty());
    }

    private ImageMetadataResponse createImageMetadataResponse() {
        return ImageMetadataResponse.createFromJson(JsonParser.parseString("""
                {
                  "id": 12554,
                  "meta": {
                    "imageName": "LuCa-7color_[17572,60173]_3x3component_data.tif [resolution #1]",
                    "imageDescription": "",
                    "imageAuthor": "Leo Leplat",
                    "projectName": "Multiple",
                    "projectId": null,
                    "projectDescription": "",
                    "datasetName": "Tutorial",
                    "datasetId": 1157,
                    "datasetDescription": "",
                    "wellSampleId": "",
                    "wellId": "",
                    "imageTimestamp": 1507197219,
                    "imageId": 12554,
                    "pixelsType": "float"
                  },
                  "perms": {
                    "canAnnotate": true,
                    "canEdit": true,
                    "canDelete": true,
                    "canLink": true
                  },
                  "tiles": true,
                  "tile_size": {
                    "width": 512,
                    "height": 512
                  },
                  "levels": 3,
                  "zoomLevelScaling": {
                    "0": 1,
                    "1": 0.5,
                    "2": 0.25
                  },
                  "interpolate": true,
                  "size": {
                    "width": 5604,
                    "height": 4200,
                    "z": 1,
                    "t": 1,
                    "c": 8
                  },
                  "pixel_size": {
                    "x": 0.49799447890790055,
                    "y": 0.49799447890790055,
                    "z": null
                  },
                  "init_zoom": 0,
                  "nominalMagnification": 10,
                  "pixel_range": [
                    -2147483648,
                    2147483647
                  ],
                  "channels": [
                    {
                      "emissionWave": null,
                      "label": "PDL1 (Opal 520)",
                      "color": "FF0000",
                      "inverted": false,
                      "reverseIntensity": false,
                      "family": "linear",
                      "coefficient": 1,
                      "window": {
                        "min": -2147483648,
                        "max": 2147483647,
                        "start": 0,
                        "end": 66.76290893554688
                      },
                      "active": true
                    },
                    {
                      "emissionWave": null,
                      "label": "CD8 (Opal 540)",
                      "color": "FFFF00",
                      "inverted": false,
                      "reverseIntensity": false,
                      "family": "linear",
                      "coefficient": 1,
                      "window": {
                        "min": -2147483648,
                        "max": 2147483647,
                        "start": 0,
                        "end": 25.086063385009766
                      },
                      "active": true
                    },
                    {
                      "emissionWave": null,
                      "label": "FoxP3 (Opal 570)",
                      "color": "FF8000",
                      "inverted": false,
                      "reverseIntensity": false,
                      "family": "linear",
                      "coefficient": 1,
                      "window": {
                        "min": -2147483648,
                        "max": 2147483647,
                        "start": 0,
                        "end": 15.784920692443848
                      },
                      "active": true
                    },
                    {
                      "emissionWave": null,
                      "label": "CD68 (Opal 620)",
                      "color": "FF00FF",
                      "inverted": false,
                      "reverseIntensity": false,
                      "family": "linear",
                      "coefficient": 1,
                      "window": {
                        "min": -2147483648,
                        "max": 2147483647,
                        "start": 0,
                        "end": 9.020310401916504
                      },
                      "active": false
                    },
                    {
                      "emissionWave": null,
                      "label": "PD1 (Opal 650)",
                      "color": "00FF00",
                      "inverted": false,
                      "reverseIntensity": false,
                      "family": "linear",
                      "coefficient": 1,
                      "window": {
                        "min": -2147483648,
                        "max": 2147483647,
                        "start": 0,
                        "end": 77.82771301269531
                      },
                      "active": false
                    },
                    {
                      "emissionWave": null,
                      "label": "CK (Opal 690)",
                      "color": "00FFFF",
                      "inverted": false,
                      "reverseIntensity": false,
                      "family": "linear",
                      "coefficient": 1,
                      "window": {
                        "min": -2147483648,
                        "max": 2147483647,
                        "start": 0,
                        "end": 46.21053695678711
                      },
                      "active": false
                    },
                    {
                      "emissionWave": null,
                      "label": "DAPI",
                      "color": "0000FF",
                      "inverted": false,
                      "reverseIntensity": false,
                      "family": "linear",
                      "coefficient": 1,
                      "window": {
                        "min": -2147483648,
                        "max": 2147483647,
                        "start": 0,
                        "end": 39.83445358276367
                      },
                      "active": false
                    },
                    {
                      "emissionWave": null,
                      "label": "Autofluorescence",
                      "color": "000000",
                      "inverted": false,
                      "reverseIntensity": false,
                      "family": "linear",
                      "coefficient": 1,
                      "window": {
                        "min": -2147483648,
                        "max": 2147483647,
                        "start": 0,
                        "end": 23.98431968688965
                      },
                      "active": false
                    }
                  ],
                  "split_channel": {
                    "g": {
                      "width": 16820,
                      "height": 12608,
                      "border": 2,
                      "gridx": 3,
                      "gridy": 3
                    },
                    "c": {
                      "width": 16820,
                      "height": 12608,
                      "border": 2,
                      "gridx": 3,
                      "gridy": 3
                    }
                  },
                  "rdefs": {
                    "model": "greyscale",
                    "projection": "normal",
                    "defaultZ": 0,
                    "defaultT": 0,
                    "invertAxis": false
                  }
                }
                """).getAsJsonObject()).orElse(null);
    }
}
