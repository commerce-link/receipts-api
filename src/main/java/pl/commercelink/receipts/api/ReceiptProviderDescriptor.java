package pl.commercelink.receipts.api;

import pl.commercelink.provider.api.ProviderDescriptor;

/**
 * Service-loaded entry point of a receipts provider (register in
 * {@code META-INF/services/pl.commercelink.receipts.api.ReceiptProviderDescriptor}). Providers that push
 * status updates return a {@code WebhookBinding} whose executor yields a {@link Receipt} and answers an
 * unauthenticated call with {@code WebhookStatusResponse("REJECTED")}.
 *
 * <p>An authentic call that has nothing to report — e.g. an event about a document that is not one of this
 * store's receipts — yields {@code WebhookOutcome.empty()}: no result and no {@code REJECTED} response, and
 * never an exception. {@code REJECTED} is reserved for calls that fail authentication or integrity.
 */
public interface ReceiptProviderDescriptor extends ProviderDescriptor<ReceiptProvider> {
}
