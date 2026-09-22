package pl.commercelink.receipts.api.testing;

import org.junit.jupiter.api.Test;
import pl.commercelink.provider.api.EventBinding.WebhookBinding;
import pl.commercelink.provider.api.WebhookContext;
import pl.commercelink.provider.api.WebhookOutcome;
import pl.commercelink.provider.api.WebhookStatusResponse;
import pl.commercelink.receipts.api.FiscalData;
import pl.commercelink.receipts.api.Receipt;
import pl.commercelink.receipts.api.ReceiptProviderDescriptor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Rules for providers that push status updates: exactly one webhook binding exists iff the provider says it
 * pushes, an authentic call yields the receipt's state under its receipt key, and an unauthenticated or
 * tampered call yields no result and a {@code REJECTED} response.
 */
public abstract class ReceiptWebhookContractTest {

    public static final String REJECTED = "REJECTED";

    protected abstract ReceiptProviderDescriptor descriptor();

    /** Store configuration the executor sees in {@link WebhookContext#providerConfig()} (holds the webhook secret). */
    protected abstract Map<String, String> providerConfig();

    /** The call the provider would send to report {@code receipt}'s current state, correctly authenticated. */
    protected abstract SignedWebhook validWebhook(Receipt receipt);

    /** {@code valid} with its authentication removed or wrong (missing signature header, wrong token). */
    protected abstract SignedWebhook unauthenticated(SignedWebhook valid);

    /** {@code valid} with its body altered but authentication kept; empty when the scheme cannot detect it. */
    protected Optional<SignedWebhook> tampered(SignedWebhook valid) {
        return Optional.empty();
    }

    /** The receipt reported in the sample webhook. */
    protected Receipt sampleReceipt() {
        return Receipt.fiscalised("tck-order-1:R1", "tck-provider-1",
                new FiscalData("TCK0000001", "000001", Instant.parse("2026-09-22T10:00:00Z")),
                "https://receipts.example/view/tck-provider-1");
    }

    @Test
    void pushesStatusUpdatesMatchesWebhookBinding() {
        // when
        boolean pushes = descriptor().create(providerConfig()).pushesStatusUpdates();
        List<WebhookBinding<?>> webhooks = webhookBindings();

        // then
        assertEquals(pushes, !webhooks.isEmpty());
        assumeTrue(pushes, "Provider does not push status updates");
        assertEquals(1, webhooks.size());
    }

    @Test
    void authenticWebhookYieldsReceiptUnderItsKey() {
        // given
        Receipt expected = sampleReceipt();

        // when
        WebhookOutcome<?> outcome = execute(validWebhook(expected));

        // then
        Receipt receipt = assertInstanceOf(Receipt.class, outcome.result());
        assertEquals(expected.receiptKey(), receipt.receiptKey());
        assertEquals(expected.state(), receipt.state());
        assertNotNull(receipt.fiscal());
    }

    @Test
    void unauthenticatedWebhookIsRejected() {
        // when
        WebhookOutcome<?> outcome = execute(unauthenticated(validWebhook(sampleReceipt())));

        // then
        assertRejected(outcome);
    }

    @Test
    void tamperedWebhookIsRejected() {
        // given
        Optional<SignedWebhook> tampered = tampered(validWebhook(sampleReceipt()));
        assumeTrue(tampered.isPresent(), "Authentication scheme does not cover the body");

        // when
        WebhookOutcome<?> outcome = execute(tampered.get());

        // then
        assertRejected(outcome);
    }

    private WebhookOutcome<?> execute(SignedWebhook webhook) {
        List<WebhookBinding<?>> webhooks = webhookBindings();
        assumeTrue(!webhooks.isEmpty(), "Provider declares no webhook binding");
        return webhooks.getFirst().executor().execute(webhook.payload(), new WebhookContext(webhook.headers(), providerConfig()));
    }

    private List<WebhookBinding<?>> webhookBindings() {
        return descriptor().bindings().stream()
                .filter(binding -> binding instanceof WebhookBinding<?>)
                .<WebhookBinding<?>>map(binding -> (WebhookBinding<?>) binding)
                .toList();
    }

    private static void assertRejected(WebhookOutcome<?> outcome) {
        assertNull(outcome.result());
        WebhookStatusResponse response = assertInstanceOf(WebhookStatusResponse.class, outcome.responseBody());
        assertEquals(REJECTED, response.status());
    }
}
