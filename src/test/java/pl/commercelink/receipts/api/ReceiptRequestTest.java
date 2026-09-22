package pl.commercelink.receipts.api;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReceiptRequestTest {

    private static final LocalDateTime SALE = LocalDateTime.of(2026, 9, 22, 12, 0);

    private static ReceiptRequest.Builder valid() {
        return ReceiptRequest.builder()
                .receiptKey("order-1:R1")
                .orderId("order-1")
                .saleDate(SALE)
                .line(ReceiptLine.goods("Laptop X15", BigDecimal.ONE, Money.ofGrosze(399900), VatRate.VAT_23))
                .line(ReceiptLine.shipping("Dostawa kurier", Money.ofGrosze(1599), VatRate.VAT_23))
                .payment(ReceiptPayment.of(PaymentForm.TRANSFER, Money.ofGrosze(401499)).label("Przelewy24"));
    }

    @Test
    void buildsValidRequestWithDefaults() {
        // when
        ReceiptRequest request = valid().build();

        // then
        assertEquals("order-1:R1", request.receiptKey());
        assertEquals(ReceiptMedium.ELECTRONIC, request.medium());
        assertEquals(ReceiptBuyer.NONE, request.buyer());
        assertEquals(2, request.lines().size());
        assertEquals(Money.ofGrosze(401499), request.totalGross());
    }

    @Test
    void linesAndPaymentsAreUnmodifiable() {
        // given
        ReceiptRequest request = valid().build();

        // when / then
        assertThrows(UnsupportedOperationException.class, () -> request.lines().clear());
        assertThrows(UnsupportedOperationException.class, () -> request.payments().clear());
    }

    @Test
    void rejectsInvalidKey() {
        // when / then
        assertThrows(ReceiptValidationException.class, () -> valid().receiptKey("order/1").build());
    }

    @Test
    void rejectsMissingOrderIdOrSaleDateOrMedium() {
        // when / then
        assertThrows(ReceiptValidationException.class, () -> valid().orderId(" ").build());
        assertThrows(ReceiptValidationException.class, () -> valid().saleDate(null).build());
        assertThrows(ReceiptValidationException.class, () -> valid().medium(null).build());
    }

    @Test
    void rejectsRequestWithoutLines() {
        // when / then
        assertThrows(ReceiptValidationException.class, () -> ReceiptRequest.builder()
                .receiptKey("k").orderId("o").saleDate(SALE)
                .payment(ReceiptPayment.of(PaymentForm.CASH, Money.ofGrosze(1)))
                .build());
    }

    @Test
    void rejectsBlankLineName() {
        // when / then
        ReceiptValidationException error = assertThrows(ReceiptValidationException.class, () -> ReceiptRequest.builder()
                .receiptKey("k").orderId("o").saleDate(SALE)
                .line(ReceiptLine.goods("  ", BigDecimal.ONE, Money.ofGrosze(100), VatRate.VAT_23))
                .payment(ReceiptPayment.of(PaymentForm.CASH, Money.ofGrosze(100)))
                .build());
        assertTrue(error.getMessage().contains("line 0"));
    }

    @Test
    void rejectsNonPositiveQuantityAndTooFineQuantity() {
        // when / then
        assertThrows(ReceiptValidationException.class, () -> singleLine(
                ReceiptLine.goods("A", BigDecimal.ZERO, Money.ofGrosze(100), VatRate.VAT_23), Money.ofGrosze(0)));
        assertThrows(ReceiptValidationException.class, () -> singleLine(
                ReceiptLine.goods("A", new BigDecimal("0.0001"), Money.ofGrosze(100), VatRate.VAT_23), Money.ofGrosze(0)));
    }

    @Test
    void rejectsNegativeUnitPriceAndMissingVatRate() {
        // when / then
        assertThrows(ReceiptValidationException.class, () -> singleLine(
                ReceiptLine.goods("A", BigDecimal.ONE, Money.ofGrosze(-1), VatRate.VAT_23), Money.ofGrosze(1)));
        assertThrows(ReceiptValidationException.class, () -> singleLine(
                ReceiptLine.goods("A", BigDecimal.ONE, Money.ofGrosze(100), null), Money.ofGrosze(100)));
    }

    @Test
    void rejectsTotalThatDoesNotMatchUnitTimesQuantity() {
        // when / then
        assertThrows(ReceiptValidationException.class, () -> singleLine(
                ReceiptLine.goods("A", BigDecimal.ONE, Money.ofGrosze(100), VatRate.VAT_23).totalGross(Money.ofGrosze(99)),
                Money.ofGrosze(99)));
    }

    @Test
    void rejectsMissingOrNonPositivePayment() {
        // when / then
        assertThrows(ReceiptValidationException.class, () -> ReceiptRequest.builder()
                .receiptKey("k").orderId("o").saleDate(SALE)
                .line(ReceiptLine.goods("A", BigDecimal.ONE, Money.ofGrosze(100), VatRate.VAT_23))
                .build());
        assertThrows(ReceiptValidationException.class, () -> singleLine(
                ReceiptLine.goods("A", BigDecimal.ONE, Money.ofGrosze(0), VatRate.VAT_23), Money.ofGrosze(0)));
    }

    @Test
    void rejectsPaymentsThatDoNotCoverLines() {
        // when / then
        ReceiptValidationException error = assertThrows(ReceiptValidationException.class, () -> valid()
                .payment(ReceiptPayment.of(PaymentForm.CASH, Money.ofGrosze(1)))
                .build());
        assertTrue(error.getMessage().contains("4014.99"));
    }

    private static ReceiptRequest singleLine(ReceiptLine.Builder line, Money paid) {
        return ReceiptRequest.builder()
                .receiptKey("k").orderId("o").saleDate(SALE)
                .line(line)
                .payment(ReceiptPayment.of(PaymentForm.CASH, paid))
                .build();
    }
}
