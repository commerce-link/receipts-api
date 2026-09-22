package pl.commercelink.receipts.api;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class ReceiptExceptionsTest {

    @Test
    void rejectedCarriesProviderCode() {
        // when
        ReceiptRejectedException rejected = new ReceiptRejectedException("43", "Invalid posId");

        // then
        assertEquals("43", rejected.code());
        assertEquals("Invalid posId", rejected.getMessage());
        assertInstanceOf(ReceiptException.class, rejected);
    }

    @Test
    void outcomeUnknownKeepsCause() {
        // given
        IOException timeout = new IOException("read timed out");

        // when
        ReceiptOutcomeUnknownException unknown = new ReceiptOutcomeUnknownException("timeout after send", timeout);

        // then
        assertSame(timeout, unknown.getCause());
        assertInstanceOf(ReceiptException.class, unknown);
    }

    @Test
    void outcomeUnknownWithoutCauseCarriesMessage() {
        // when
        ReceiptOutcomeUnknownException unknown = new ReceiptOutcomeUnknownException("response lost");

        // then
        assertEquals("response lost", unknown.getMessage());
        assertInstanceOf(ReceiptException.class, unknown);
    }

    @Test
    void validationIsAReceiptException() {
        // when / then
        assertInstanceOf(ReceiptException.class, new ReceiptValidationException("bad"));
    }
}
