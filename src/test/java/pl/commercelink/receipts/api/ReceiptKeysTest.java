package pl.commercelink.receipts.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReceiptKeysTest {

    @Test
    void acceptsOrderScopedKey() {
        // when / then
        assertEquals("0f3c-9a_1:R1", ReceiptKeys.requireValid("0f3c-9a_1:R1"));
    }

    @Test
    void rejectsNullBlankTooLongAndForeignCharacters() {
        // when / then
        assertThrows(ReceiptValidationException.class, () -> ReceiptKeys.requireValid(null));
        assertThrows(ReceiptValidationException.class, () -> ReceiptKeys.requireValid(" "));
        assertThrows(ReceiptValidationException.class, () -> ReceiptKeys.requireValid("a".repeat(65)));
        assertThrows(ReceiptValidationException.class, () -> ReceiptKeys.requireValid("order/1"));
    }
}
