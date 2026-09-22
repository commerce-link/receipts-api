package pl.commercelink.receipts.api;

import pl.commercelink.provider.api.ProviderDescriptor;

/**
 * Service-loaded entry point of a receipts provider (register in
 * {@code META-INF/services/pl.commercelink.receipts.api.ReceiptProviderDescriptor}). Providers that push
 * status updates return a {@code WebhookBinding} whose executor yields a {@link Receipt} and answers an
 * unauthenticated call with {@code WebhookStatusResponse("REJECTED")}.
 */
public interface ReceiptProviderDescriptor extends ProviderDescriptor<ReceiptProvider> {
}
