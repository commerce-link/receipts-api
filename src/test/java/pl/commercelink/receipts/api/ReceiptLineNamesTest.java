package pl.commercelink.receipts.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReceiptLineNamesTest {

    @Test
    void keepsPolishCharacters() {
        // when / then
        assertEquals("Zażółć gęślą jaźń", ReceiptLineNames.normalize("Zażółć gęślą jaźń", 80));
    }

    @Test
    void replacesTypographicPunctuationWithAscii() {
        // when / then
        assertEquals("Monitor \"Pro\" - 27'' ...", ReceiptLineNames.normalize("Monitor „Pro” – 27'' …", 80));
    }

    @Test
    void transliteratesAccentsOutsideWindows1250() {
        // when / then
        assertEquals("Pinata", ReceiptLineNames.normalize("Piñata", 80));
    }

    @Test
    void dropsCharactersThatCannotBePrinted() {
        // when / then
        assertEquals("Kabel HDMI 2m", ReceiptLineNames.normalize("Kabel HDMI 😀 2m", 80));
    }

    @Test
    void collapsesWhitespaceAndControlCharacters() {
        // when / then
        assertEquals("Mysz bezprzewodowa", ReceiptLineNames.normalize("  Mysz\t\n bezprzewodowa  ", 80));
    }

    @Test
    void truncatesToMaxLengthAndStripsTrailingSpace() {
        // when / then
        assertEquals("abcd", ReceiptLineNames.normalize("abcdef", 4));
        assertEquals("ab", ReceiptLineNames.normalize("ab cdef", 3));
    }

    @Test
    void rejectsNonPositiveMaxLength() {
        // when / then
        assertThrows(IllegalArgumentException.class, () -> ReceiptLineNames.normalize("x", 0));
    }
}
