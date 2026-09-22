package pl.commercelink.receipts.api;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ReceiptProviderTest {

    private final ReceiptProvider minimal = new ReceiptProvider() {
        @Override
        public Receipt issue(ReceiptRequest request) {
            return Receipt.pending(request.receiptKey(), "p-1");
        }

        @Override
        public Optional<Receipt> find(String receiptKey) {
            return Optional.empty();
        }

        @Override
        public Receipt fetch(String providerReceiptId) {
            throw new ReceiptException("unknown " + providerReceiptId);
        }
    };

    @Test
    void capabilityDefaultsAreConservative() {
        // when / then
        assertEquals(Set.of(ReceiptMedium.ELECTRONIC), minimal.supportedMedia());
        assertEquals(40, minimal.maxLineNameLength());
        assertFalse(minimal.requiresBuyerEmail());
        assertFalse(minimal.pushesStatusUpdates());
    }
}
