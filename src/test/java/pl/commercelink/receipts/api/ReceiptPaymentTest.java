package pl.commercelink.receipts.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ReceiptPaymentTest {

    @Test
    void carriesFormAmountAndOptionalLabel() {
        // when
        ReceiptPayment labelled = ReceiptPayment.of(PaymentForm.TRANSFER, Money.ofGrosze(401499)).label("Przelewy24").build();
        ReceiptPayment plain = ReceiptPayment.of(PaymentForm.CASH, Money.ofGrosze(100)).build();

        // then
        assertEquals(PaymentForm.TRANSFER, labelled.form());
        assertEquals(Money.ofGrosze(401499), labelled.amount());
        assertEquals("Przelewy24", labelled.label());
        assertNull(plain.label());
    }
}
