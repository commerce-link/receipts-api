package pl.commercelink.receipts.api;

import java.util.Optional;
import java.util.Set;

/**
 * Issues fiscal e-receipts through one provider, bound to one store's configuration. Implementations
 * must be thread-safe.
 *
 * <p>Failure contract of {@link #issue}: {@link ReceiptValidationException} before any remote call;
 * {@link ReceiptRejectedException} when the provider definitively refused; {@link ReceiptOutcomeUnknownException}
 * for every other failure after the request may have been sent; plain {@link ReceiptException} for failures
 * before sending. Adapters must never let a raw exception escape: a failure while constructing the result
 * from a provider response (e.g. a response that claims success but lacks fiscal data) is also converted to
 * a {@link ReceiptException}, never left to escape as-is.
 *
 * <p><b>Evolving enums.</b> {@link LineKind}, {@link PaymentForm}, {@link ReceiptMedium} and {@link VatRate}
 * may gain constants in later versions. An adapter that does not map a constant it receives must refuse the
 * request with {@link ReceiptValidationException} before any remote call, rather than sending a guess or
 * silently dropping data. Future discount lines, for instance, will be introduced behind a capability so
 * adapters that do not support them keep refusing safely instead of mis-reporting a total.
 */
public interface ReceiptProvider {

    /**
     * Idempotent by {@code request.receiptKey()}: calling again with the same key never creates a second
     * receipt and returns the existing one. Returns {@link ReceiptState#PENDING} or {@link ReceiptState#FISCALISED},
     * both carrying {@link Receipt#receiptKey()}.
     */
    Receipt issue(ReceiptRequest request);

    /**
     * Read-only probe: returns the receipt created under this key, if any, carrying {@link Receipt#receiptKey()}.
     * MUST never create or fiscalise anything. Validates the key with {@link ReceiptKeys#requireValid}. Never
     * returns empty because of a transport error — that is thrown as {@link ReceiptException} instead, since an
     * empty result here means "no receipt under this key". It does not replace retrying {@link #issue} after
     * {@link ReceiptOutcomeUnknownException}: a PENDING receipt may not have been sent for fiscalisation yet.
     */
    Optional<Receipt> find(String receiptKey);

    /**
     * Current state of a known receipt: polling of PENDING receipts and of a missing documentUrl. Throws
     * {@link ReceiptException} when {@code providerReceiptId} is unknown to the provider. {@link Receipt#receiptKey()}
     * may be null in the result when the provider does not echo the key back on this call; callers then
     * correlate by {@code providerReceiptId}.
     */
    Receipt fetch(String providerReceiptId);

    /** Media this provider can issue. */
    default Set<ReceiptMedium> supportedMedia() {
        return Set.of(ReceiptMedium.ELECTRONIC);
    }

    /** Longest line name the configured device prints; callers pass it to {@link ReceiptLineNames#normalize}. */
    default int maxLineNameLength() {
        return 40;
    }

    /** Whether {@link ReceiptBuyer#email()} is mandatory for an e-receipt. */
    default boolean requiresBuyerEmail() {
        return false;
    }

    /**
     * Whether the descriptor declares a status webhook binding. Consumers still poll PENDING receipts:
     * some providers do not retry failed webhooks.
     */
    default boolean pushesStatusUpdates() {
        return false;
    }
}
