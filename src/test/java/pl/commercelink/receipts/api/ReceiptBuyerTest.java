package pl.commercelink.receipts.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ReceiptBuyerTest {

    @Test
    void noneHasNoContactAndNoTaxId() {
        // when / then
        assertNull(ReceiptBuyer.NONE.email());
        assertNull(ReceiptBuyer.NONE.taxId());
    }

    @Test
    void builderSetsEmailAndTaxId() {
        // when
        ReceiptBuyer buyer = ReceiptBuyer.builder().email("jan@example.com").taxId("5260250274").build();

        // then
        assertEquals("jan@example.com", buyer.email());
        assertEquals("5260250274", buyer.taxId());
    }
}
