package pl.commercelink.receipts.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VatRateTest {

    @Test
    void fromMultiplierMapsStandardRates() {
        // when / then
        assertEquals(VatRate.VAT_23, VatRate.fromMultiplier(1.23));
        assertEquals(VatRate.VAT_8, VatRate.fromMultiplier(1.08));
        assertEquals(VatRate.VAT_5, VatRate.fromMultiplier(1.05));
        assertEquals(VatRate.VAT_0, VatRate.fromMultiplier(1.0));
    }

    @Test
    void fromMultiplierToleratesFloatingPointNoise() {
        // when / then
        assertEquals(VatRate.VAT_23, VatRate.fromMultiplier(1.2300000000000002));
    }

    @Test
    void fromMultiplierRejectsUnknownRate() {
        // when / then
        assertThrows(IllegalArgumentException.class, () -> VatRate.fromMultiplier(1.07));
    }
}
