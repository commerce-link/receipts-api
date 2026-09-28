package pl.commercelink.receipts.api;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.text.Normalizer;
import java.util.Map;
import java.util.Objects;

/**
 * Makes a product name printable on a fiscal device: Windows-1250 only, no control characters,
 * single spaces, at most {@code maxLength} characters. Polish letters are kept.
 */
public final class ReceiptLineNames {

    private static final Charset WINDOWS_1250 = Charset.forName("windows-1250");

    private static final Map<Character, String> TYPOGRAPHY = Map.ofEntries(
            Map.entry('„', "\""), Map.entry('”', "\""), Map.entry('“', "\""), Map.entry('«', "\""), Map.entry('»', "\""),
            Map.entry('‘', "'"), Map.entry('’', "'"), Map.entry('‚', "'"),
            Map.entry('–', "-"), Map.entry('—', "-"),
            Map.entry('…', "..."),
            Map.entry(' ', " "));

    private ReceiptLineNames() {
    }

    public static String normalize(String name, int maxLength) {
        Objects.requireNonNull(name, "name");
        if (maxLength < 1) {
            throw new IllegalArgumentException("maxLength must be positive: " + maxLength);
        }
        CharsetEncoder encoder = WINDOWS_1250.newEncoder();
        StringBuilder out = new StringBuilder(name.length());
        name.codePoints().forEach(codePoint -> out.append(printable(codePoint, encoder)));
        String collapsed = out.toString().replaceAll("\\s+", " ").strip();
        // Every kept character is in Windows-1250, hence in the BMP: substring cannot split a surrogate pair.
        return collapsed.length() <= maxLength ? collapsed : collapsed.substring(0, maxLength).strip();
    }

    private static String printable(int codePoint, CharsetEncoder encoder) {
        if (Character.isISOControl(codePoint) || Character.isWhitespace(codePoint)) {
            return " ";
        }
        if (Character.isBmpCodePoint(codePoint) && TYPOGRAPHY.containsKey((char) codePoint)) {
            return TYPOGRAPHY.get((char) codePoint);
        }
        String character = Character.toString(codePoint);
        if (encoder.canEncode(character)) {
            return character;
        }
        String stripped = Normalizer.normalize(character, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return !stripped.isEmpty() && encoder.canEncode(stripped) ? stripped : "";
    }
}
