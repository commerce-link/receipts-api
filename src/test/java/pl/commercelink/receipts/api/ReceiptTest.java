package pl.commercelink.receipts.api;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReceiptTest {

    private static final FiscalData FISCAL =
            new FiscalData("ABC1234567", "000123", Instant.parse("2026-09-22T10:15:30Z"));

    @Test
    void pendingHasNoFiscalDataNoFailure() {
        // when
        Receipt receipt = Receipt.pending("order-1:R1", "doc-1");

        // then
        assertEquals(ReceiptState.PENDING, receipt.state());
        assertNull(receipt.fiscal());
        assertNull(receipt.failure());
        assertNull(receipt.documentUrl());
        assertFalse(receipt.state().isTerminal());
    }

    @Test
    void fiscalisedCarriesFiscalDataAndOptionalLink() {
        // when
        Receipt withLink = Receipt.fiscalised("order-1:R1", "doc-1", FISCAL, "https://hub.example/view/1");
        Receipt withoutLink = Receipt.fiscalised("order-1:R1", "doc-1", FISCAL, null);

        // then
        assertEquals(ReceiptState.FISCALISED, withLink.state());
        assertEquals(FISCAL, withLink.fiscal());
        assertEquals("https://hub.example/view/1", withLink.documentUrl());
        assertNull(withoutLink.documentUrl());
        assertTrue(withLink.state().isTerminal());
    }

    @Test
    void fiscalisedWithoutFiscalDataIsRejected() {
        // when / then
        assertThrows(IllegalArgumentException.class, () -> Receipt.fiscalised("k", "doc-1", null, null));
    }

    @Test
    void failedCarriesFailure() {
        // when
        Receipt receipt = Receipt.failed("k", "doc-1", new ReceiptFailure("16", "bad name"));

        // then
        assertEquals(ReceiptState.FAILED, receipt.state());
        assertEquals("16", receipt.failure().code());
        assertNull(receipt.fiscal());
        assertTrue(receipt.state().isTerminal());
    }

    @Test
    void failedWithoutFailureIsRejected() {
        // when / then
        assertThrows(IllegalArgumentException.class, () -> Receipt.failed("k", "doc-1", null));
    }

    @Test
    void blankKeyOrProviderIdIsRejected() {
        // when / then
        assertThrows(IllegalArgumentException.class, () -> Receipt.pending(" ", "doc-1"));
        assertThrows(IllegalArgumentException.class, () -> Receipt.pending("k", null));
    }

    @Test
    void fiscalDataRequiresTimestampButNotNumbers() {
        // when / then
        assertThrows(IllegalArgumentException.class, () -> new FiscalData("A", "1", null));
        assertNull(new FiscalData(null, null, Instant.EPOCH).receiptNumber());
    }

    @Test
    void failureRequiresMessage() {
        // when / then
        assertThrows(IllegalArgumentException.class, () -> new ReceiptFailure("1", " "));
        assertNull(new ReceiptFailure(null, "printer offline").code());
    }
}
