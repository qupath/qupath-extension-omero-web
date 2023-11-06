package qupath.ext.omero.core.entities.annotations;

import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestRatingAnnotation {

    @Test
    void Check_Value() {
        RatingAnnotation ratingAnnotation = createRatingAnnotation((short) 3);

        short rating = ratingAnnotation.getValue();

        Assertions.assertEquals(3, rating);
    }

    @Test
    void Check_Max_Value() {
        RatingAnnotation ratingAnnotation = createRatingAnnotation((short) (RatingAnnotation.getMaxValue() + 1));

        short rating = ratingAnnotation.getValue();

        Assertions.assertEquals(RatingAnnotation.getMaxValue(), rating);
    }

    @Test
    void Check_Missing_Value() {
        RatingAnnotation ratingAnnotation = new Gson().fromJson("{}", RatingAnnotation.class);

        short rating = ratingAnnotation.getValue();

        Assertions.assertEquals(0, rating);
    }

    private RatingAnnotation createRatingAnnotation(short value) {
        String json = String.format("""
                {
                    "longValue": %d
                }
                """, value);
        return new Gson().fromJson(json, RatingAnnotation.class);
    }
}
