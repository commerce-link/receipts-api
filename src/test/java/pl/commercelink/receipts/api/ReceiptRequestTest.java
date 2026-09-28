package pl.commercelink.receipts.api;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void acceptsAReceiptWithoutPayments() {
        // when
        ReceiptRequest request = ReceiptRequest.builder()
                .receiptKey("k").orderId("o").saleDate(SALE)
                .line(ReceiptLine.goods("A", BigDecimal.ONE, Money.ofGrosze(100), VatRate.VAT_23))
                .build();

        // then
        assertTrue(request.payments().isEmpty());
    }

    @Test
    void rejectsNonPositivePayment() {
        // when / then
        assertThrows(ReceiptValidationException.class, () -> singleLine(
                ReceiptLine.goods("A", BigDecimal.ONE, Money.ofGrosze(0), VatRate.VAT_23), Money.ofGrosze(0)));
    }

    @Test
    void rejectsPaymentsExceedingTheLines() {
        // when / then
        ReceiptValidationException error = assertThrows(ReceiptValidationException.class, () -> valid()
                .payment(ReceiptPayment.of(PaymentForm.CASH, Money.ofGrosze(1)))
                .build());
        assertTrue(error.getMessage().contains("4014.99"));
    }

    @Test
    void acceptsAPartPayment() {
        // when
        ReceiptRequest request = singleLine(
                ReceiptLine.goods("A", BigDecimal.ONE, Money.ofGrosze(100), VatRate.VAT_23), Money.ofGrosze(40));

        // then
        assertEquals(Money.ofGrosze(40), request.payments().get(0).amount());
    }

    @Test
    void zeroValueLineIsRefused() {
        // given: "Gratis" has unitGross 0, so it is a free line hiding in an otherwise valid receipt
        ReceiptRequest.Builder builder = ReceiptRequest.builder()
                .receiptKey("k").orderId("o").saleDate(SALE)
                .line(ReceiptLine.goods("Zestaw", BigDecimal.ONE, Money.ofGrosze(100), VatRate.VAT_23))
                .line(ReceiptLine.goods("Gratis", BigDecimal.ONE, Money.ofGrosze(0), VatRate.VAT_23))
                .payment(ReceiptPayment.of(PaymentForm.CASH, Money.ofGrosze(100)));

        // when / then
        ReceiptValidationException error = assertThrows(ReceiptValidationException.class, builder::build);
        assertTrue(error.getMessage().startsWith("line 1: "), error.getMessage());
        assertTrue(error.getMessage().contains("Gratis"), error.getMessage());
    }

    @Test
    void lineRoundingToZeroIsRefused() {
        // given: 1 grosz x 0.4 rounds HALF_UP to a 0 total, so the line would fiscalise nothing for free
        ReceiptRequest.Builder builder = ReceiptRequest.builder()
                .receiptKey("k").orderId("o").saleDate(SALE)
                .line(ReceiptLine.goods("Zestaw", BigDecimal.ONE, Money.ofGrosze(100), VatRate.VAT_23))
                .line(ReceiptLine.goods("Śruba", new BigDecimal("0.4"), Money.ofGrosze(1), VatRate.VAT_23))
                .payment(ReceiptPayment.of(PaymentForm.CASH, Money.ofGrosze(100)));

        // when
        ReceiptValidationException error = assertThrows(ReceiptValidationException.class, builder::build);

        // then
        assertTrue(error.getMessage().startsWith("line 1: "), error.getMessage());
    }

    @Test
    void nullLineOrPaymentIsAValidationError() {
        // when / then
        assertThrows(ReceiptValidationException.class, () -> valid().line((ReceiptLine) null).build());
        assertThrows(ReceiptValidationException.class, () -> valid().payment((ReceiptPayment) null).build());
    }

    @Test
    void validationMessagesShowAmountsAsDecimals() {
        // given
        ReceiptRequest.Builder builder = ReceiptRequest.builder()
                .receiptKey("k").orderId("o").saleDate(SALE)
                .line(ReceiptLine.goods("X", BigDecimal.ONE, Money.ofGrosze(-5), VatRate.VAT_23))
                .payment(ReceiptPayment.of(PaymentForm.CASH, Money.ofGrosze(-5)));

        // when / then
        ReceiptValidationException error = assertThrows(ReceiptValidationException.class, builder::build);
        assertFalse(error.getMessage().contains("Money["));
        assertTrue(error.getMessage().contains("-0.05"));
    }

    private static ReceiptRequest singleLine(ReceiptLine.Builder line, Money paid) {
        return ReceiptRequest.builder()
                .receiptKey("k").orderId("o").saleDate(SALE)
                .line(line)
                .payment(ReceiptPayment.of(PaymentForm.CASH, paid))
                .build();
    }
}
