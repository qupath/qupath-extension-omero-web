package qupath.ext.omero.core.apis.utilities;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;

public class TestCharArrayURLEncoder {

    @Test
    void Check_Encode_Works_On_Complex_Text() {
        char[] text = "!#$%&'()+,/0123456789".toCharArray();
        char[] expectedEncodedText = "%21%23%24%25%26%27%28%29%2B%2C%2F0123456789".toCharArray();

        char[] encodedText = CharArrayURLEncoder.encode(text, StandardCharsets.UTF_8);

        Assertions.assertArrayEquals(expectedEncodedText, encodedText);
    }
}
