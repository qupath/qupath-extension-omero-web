package qupath.ext.omero.core.apis;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class TestApiUtilities {

    @Test
    void Check_Char_Concat_And_Converted_To_Bytes() {
        char[] firstPart = "Lorem ipsum dolor sit amet, consectetur adipiscing elit,".toCharArray();
        char[] secondPart = " sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.".toCharArray();
        String expected = String.valueOf(firstPart) + String.valueOf(secondPart);

        byte[] concatenation = ApiUtilities.concatAndConvertToBytes(firstPart, secondPart);

        Assertions.assertEquals(expected, new String(concatenation, StandardCharsets.UTF_8));
    }

    @Test
    void Check_Char_Concat_And_Converted_To_Bytes_Is_Cleared() {
        char[] firstPart = "Lorem ipsum dolor sit amet, consectetur adipiscing elit,".toCharArray();
        char[] secondPart = " sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.".toCharArray();
        char[] expectedFirstPart = new char[firstPart.length];
        char[] expectedSecondPart = new char[secondPart.length];

        ApiUtilities.concatAndConvertToBytes(firstPart, secondPart);

        Assertions.assertEquals(expectedFirstPart.length, firstPart.length);
        for (int i = 0; i < expectedFirstPart.length; i++) {
            Assertions.assertEquals(expectedFirstPart[i], firstPart[i]);
        }
        Assertions.assertEquals(expectedSecondPart.length, secondPart.length);
        for (int i = 0; i < expectedSecondPart.length; i++) {
            Assertions.assertEquals(expectedSecondPart[i], secondPart[i]);
        }
    }

    @Test
    void Check_Text_URL_Encoded() {
        char[] text = "!#$%&'()+,/0123456789".toCharArray();
        char[] expectedEncodedText = "%21%23%24%25%26%27%28%29%2B%2C%2F0123456789".toCharArray();

        char[] encodedText = ApiUtilities.urlEncode(text);

        Assertions.assertArrayEquals(expectedEncodedText, encodedText);
    }

    @Test
    void Check_Text_URL_Encoded_Is_Cleared() {
        char[] text = "!#$%&'()+,/0123456789".toCharArray();
        char[] expectedText = new char[text.length];

        ApiUtilities.urlEncode(text);

        Assertions.assertEquals(expectedText.length, text.length);
        for (int i = 0; i < expectedText.length; i++) {
            Assertions.assertEquals(expectedText[i], text[i]);
        }
    }
}
