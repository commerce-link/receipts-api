package pl.commercelink.receipts.api;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ReceiptLineTest {

    @Test
    void goodsDefaultsTotalToUnitTimesQuantity() {
        // when
        ReceiptLine line = ReceiptLine.goods("Kawa 250 g", new BigDecimal("3"), Money.ofGrosze(1999), VatRate.VAT_23)
                .sku("KAWA-250").ean("5901234123457").build();

        // then
        assertEquals(LineKind.GOODS, line.kind());
        assertEquals(Money.ofGrosze(5997), line.totalGross());
        assertEquals("KAWA-250", line.sku());
        assertEquals("5901234123457", line.ean());
    }

    @Test
    void shippingIsOneServiceUnit() {
        // when
        ReceiptLine line = ReceiptLine.shipping("Dostawa kurier", Money.ofGrosze(1599), VatRate.VAT_23).build();

        // then
        assertEquals(LineKind.SHIPPING, line.kind());
        assertEquals(BigDecimal.ONE, line.quantity());
        assertEquals(Money.ofGrosze(1599), line.totalGross());
        assertNull(line.sku());
    }

    @Test
    void explicitTotalOverridesDefault() {
        // when
        ReceiptLine line = ReceiptLine.service("Montaż", BigDecimal.ONE, Money.ofGrosze(5000), VatRate.VAT_8)
                .totalGross(Money.ofGrosze(4999)).build();

        // then
        assertEquals(LineKind.SERVICE, line.kind());
        assertEquals(Money.ofGrosze(4999), line.totalGross());
    }

    @Test
    void weightQuantityRoundsTotalHalfUp() {
        // when
        ReceiptLine line = ReceiptLine.goods("Orzechy", new BigDecimal("0.333"), Money.ofGrosze(5000), VatRate.VAT_5).build();

        // then
        assertEquals(Money.ofGrosze(1665), line.totalGross());
    }

    @Test
    void equalLinesAreEqual() {
        // when / then
        assertEquals(ReceiptLine.shipping("D", Money.ofGrosze(1), VatRate.VAT_23).build(),
                ReceiptLine.shipping("D", Money.ofGrosze(1), VatRate.VAT_23).build());
    }
}
