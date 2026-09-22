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
 * before sending. Adapters must never let a raw exception escape.
 */
public interface ReceiptProvider {

    /**
     * Idempotent by {@code request.receiptKey()}: calling again with the same key never creates a second
     * receipt and returns the existing one. Returns {@link ReceiptState#PENDING} or {@link ReceiptState#FISCALISED}.
     */
    Receipt issue(ReceiptRequest request);

    /**
     * Read-only probe after {@link ReceiptOutcomeUnknownException}: returns the receipt created under this key,
     * if any. MUST never create or fiscalise anything. Validates the key with {@link ReceiptKeys#requireValid}.
     */
    Optional<Receipt> find(String receiptKey);

    /** Current state of a known receipt: polling of PENDING receipts and of a missing documentUrl. */
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
