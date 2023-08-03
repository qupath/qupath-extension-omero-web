package qupath.lib.images.servers.omero.common.api.requests.apis.utilities;

import java.io.CharArrayWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;

/**
 * Utility class for HTML form encoding.
 * This is basically a modification of the {@link java.net.URLEncoder URLEncoder}
 * class with support for char arrays.
 */
class CharArrayURLEncoder {
    private static final BitSet dontNeedEncoding = new BitSet(256);

    private CharArrayURLEncoder() {
        throw new RuntimeException("This class is not instantiable.");
    }

    static {
        int i;
        for(i = 97; i <= 122; ++i) {
            dontNeedEncoding.set(i);
        }

        for(i = 65; i <= 90; ++i) {
            dontNeedEncoding.set(i);
        }

        for(i = 48; i <= 57; ++i) {
            dontNeedEncoding.set(i);
        }

        dontNeedEncoding.set(32);
        dontNeedEncoding.set(45);
        dontNeedEncoding.set(95);
        dontNeedEncoding.set(46);
        dontNeedEncoding.set(42);
    }

    /**
     * Translates a char array into application/x-www-form-urlencoded format using a specific encoding scheme.
     * This method uses the supplied encoding scheme to obtain the bytes for unsafe characters.
     *
     * @param s  the char array to be translated
     * @param charset  the charset to use for the conversion
     * @return a translated char array
     */
    public static char[] encode(char[] s, Charset charset) {
        Objects.requireNonNull(charset, "charset");
        List<Character> out = new ArrayList<>();
        CharArrayWriter charArrayWriter = new CharArrayWriter();

        int i = 0;
        while(i < s.length) {
            char c = s[i];
            if (dontNeedEncoding.get(c)) {
                if (c == ' ') {
                    c = '+';
                }

                out.add(c);
                ++i;
            } else {
                do {
                    charArrayWriter.write(c);
                    if (c >= '\ud800' && c <= '\udbff' && i + 1 < s.length) {
                        char d = s[i + 1];
                        if (d >= '\udc00' && d <= '\udfff') {
                            charArrayWriter.write(d);
                            ++i;
                        }
                    }

                    ++i;
                } while(i < s.length && !dontNeedEncoding.get(c = s[i]));

                charArrayWriter.flush();
                String str = charArrayWriter.toString();
                byte[] ba = str.getBytes(charset);

                for (byte b : ba) {
                    out.add('%');
                    char ch = Character.forDigit(b >> 4 & 15, 16);
                    if (Character.isLetter(ch)) {
                        ch = (char) (ch - 32);
                    }

                    out.add(ch);
                    ch = Character.forDigit(b & 15, 16);
                    if (Character.isLetter(ch)) {
                        ch = (char) (ch - 32);
                    }

                    out.add(ch);
                }

                charArrayWriter.reset();
            }
        }

        char[] res = new char[out.size()];
        for (int j=0; j<out.size(); ++j) {
            res[j] = out.get(j);
        }
        return res;
    }
}
