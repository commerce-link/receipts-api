package pl.commercelink.receipts.api.testing;

import org.junit.jupiter.api.Test;
import pl.commercelink.receipts.api.Money;
import pl.commercelink.receipts.api.PaymentForm;
import pl.commercelink.receipts.api.Receipt;
import pl.commercelink.receipts.api.ReceiptBuyer;
import pl.commercelink.receipts.api.ReceiptLine;
import pl.commercelink.receipts.api.ReceiptMedium;
import pl.commercelink.receipts.api.ReceiptOutcomeUnknownException;
import pl.commercelink.receipts.api.ReceiptPayment;
import pl.commercelink.receipts.api.ReceiptProvider;
import pl.commercelink.receipts.api.ReceiptRejectedException;
import pl.commercelink.receipts.api.ReceiptRequest;
import pl.commercelink.receipts.api.ReceiptState;
import pl.commercelink.receipts.api.ReceiptValidationException;
import pl.commercelink.receipts.api.VatRate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Rules every {@link ReceiptProvider} must satisfy: issuing is idempotent by receipt key, {@code find} is a
 * read-only probe, {@code fetch} reflects fiscalisation and failure, bad input fails before any remote call,
 * and failures surface only as the contract's exceptions. Adapters extend this class in their tests and
 * implement the hooks against a fake backend.
 */
public abstract class ReceiptProviderContractTest {

    /** Provider bound to a working fake backend. */
    protected abstract ReceiptProvider provider();

    /** A receipt key never used before in this JVM (adapters may keep receipts statically). */
    protected abstract String uniqueReceiptKey();

    /**
     * Moves a PENDING receipt forward on the fake backend, as the fiscal device would, so that the next
     * {@code fetch} sees {@code target} (FISCALISED or FAILED).
     */
    protected abstract void settle(Receipt pending, ReceiptState target);

    /** A valid request the adapter's fixture accepts. Override when the provider needs other data (e.g. buyer email). */
    protected ReceiptRequest sampleRequest(String receiptKey) {
        return ReceiptRequest.builder()
                .receiptKey(receiptKey)
                .orderId("tck-order-" + receiptKey.replace(':', '-'))
                .saleDate(LocalDateTime.of(2026, 9, 22, 12, 0))
                .line(ReceiptLine.goods("Kabel HDMI 2m", new BigDecimal("2"), Money.ofGrosze(2499), VatRate.VAT_23).sku("HDMI-2"))
                .line(ReceiptLine.shipping("Dostawa kurier", Money.ofGrosze(1599), VatRate.VAT_23))
                .payment(ReceiptPayment.of(PaymentForm.TRANSFER, Money.ofGrosze(6597)).label("Przelewy24"))
                .buyer(ReceiptBuyer.builder().email("tck@example.com").build())
                .build();
    }

    /** Provider whose backend fails with a transport error after the request was sent. */
    protected Optional<ReceiptProvider> providerWithFailingTransport() {
        return Optional.empty();
    }

    /** Provider whose backend explicitly refuses every receipt. */
    protected Optional<ReceiptProvider> providerWithRejectingBackend() {
        return Optional.empty();
    }

    /** Number of receipts created on the fake backend since the test started, if observable. */
    protected OptionalInt createCalls() {
        return OptionalInt.empty();
    }

    /** Number of remote backend interactions since the test started, if observable. */
    protected OptionalInt remoteCalls() {
        return OptionalInt.empty();
    }

    @Test
    void issueReturnsPendingOrFiscalisedCarryingTheKey() {
        // given
        String key = uniqueReceiptKey();

        // when
        Receipt receipt = provider().issue(sampleRequest(key));

        // then
        assertEquals(key, receipt.receiptKey());
        assertTrue(receipt.state() == ReceiptState.PENDING || receipt.state() == ReceiptState.FISCALISED,
                "issue must return PENDING or FISCALISED, got " + receipt.state());
        assertFalse(receipt.providerReceiptId().isBlank());
    }

    @Test
    void retryWithSameKeyDoesNotCreateSecondReceipt() {
        // given
        ReceiptProvider provider = provider();
        ReceiptRequest request = sampleRequest(uniqueReceiptKey());

        // when
        Receipt first = provider.issue(request);
        Receipt second = provider.issue(request);

        // then
        assertEquals(first.providerReceiptId(), second.providerReceiptId());
        createCalls().ifPresent(count -> assertEquals(1, count));
    }

    @Test
    void findForUnknownKeyIsEmptyAndCreatesNothing() {
        // when
        Optional<Receipt> found = provider().find(uniqueReceiptKey());

        // then
        assertTrue(found.isEmpty());
        createCalls().ifPresent(count -> assertEquals(0, count));
    }

    @Test
    void findAfterIssueReturnsTheSameReceipt() {
        // given
        ReceiptProvider provider = provider();
        String key = uniqueReceiptKey();
        Receipt issued = provider.issue(sampleRequest(key));

        // when
        Optional<Receipt> found = provider.find(key);

        // then
        assertTrue(found.isPresent());
        assertEquals(key, found.get().receiptKey());
        assertEquals(issued.providerReceiptId(), found.get().providerReceiptId());
        createCalls().ifPresent(count -> assertEquals(1, count));
    }

    @Test
    void fetchReflectsFiscalisation() {
        // given
        ReceiptProvider provider = provider();
        Receipt issued = provider.issue(sampleRequest(uniqueReceiptKey()));
        if (issued.state() == ReceiptState.PENDING) {
            settle(issued, ReceiptState.FISCALISED);
        }

        // when
        Receipt fetched = provider.fetch(issued.providerReceiptId());

        // then
        assertEquals(ReceiptState.FISCALISED, fetched.state());
        assertNotNull(fetched.fiscal());
        assertEquals(issued.receiptKey(), fetched.receiptKey());
    }

    @Test
    void fetchReflectsFailure() {
        // given
        ReceiptProvider provider = provider();
        Receipt issued = provider.issue(sampleRequest(uniqueReceiptKey()));
        assumeTrue(issued.state() == ReceiptState.PENDING, "Provider fiscalises synchronously — no later failure possible");
        settle(issued, ReceiptState.FAILED);

        // when
        Receipt fetched = provider.fetch(issued.providerReceiptId());

        // then
        assertEquals(ReceiptState.FAILED, fetched.state());
        assertNotNull(fetched.failure());
    }

    @Test
    void nullRequestThrowsBeforeAnyRemoteCall() {
        // when / then
        assertThrows(ReceiptValidationException.class, () -> provider().issue(null));
        remoteCalls().ifPresent(count -> assertEquals(0, count));
    }

    @Test
    void blankKeyFindThrowsBeforeAnyRemoteCall() {
        // when / then
        assertThrows(ReceiptValidationException.class, () -> provider().find(" "));
        remoteCalls().ifPresent(count -> assertEquals(0, count));
    }

    @Test
    void transportFailureAfterSendSurfacesAsOutcomeUnknown() {
        // given
        ReceiptProvider provider = assumePresent(providerWithFailingTransport(), "a transport failure after send");

        // when / then
        assertThrows(ReceiptOutcomeUnknownException.class, () -> provider.issue(sampleRequest(uniqueReceiptKey())));
    }

    @Test
    void explicitRejectionSurfacesAsRejected() {
        // given
        ReceiptProvider provider = assumePresent(providerWithRejectingBackend(), "an explicit rejection");

        // when / then
        assertThrows(ReceiptRejectedException.class, () -> provider.issue(sampleRequest(uniqueReceiptKey())));
    }

    @Test
    void capabilitiesAreUsable() {
        // given
        ReceiptProvider provider = provider();

        // when / then
        assertTrue(provider.supportedMedia().contains(ReceiptMedium.ELECTRONIC));
        assertTrue(provider.maxLineNameLength() > 0);
    }

    protected static ReceiptProvider assumePresent(Optional<ReceiptProvider> provider, String scenario) {
        assumeTrue(provider.isPresent(), "Adapter does not simulate " + scenario + " — hook not implemented");
        return provider.orElseThrow();
    }
}
