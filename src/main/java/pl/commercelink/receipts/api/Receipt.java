package pl.commercelink.receipts.api;

import java.util.Objects;

/**
 * State of one issuing attempt as known by the provider. Returned by issue/find/fetch and by status
 * webhooks. {@code fiscal} is present iff FISCALISED, {@code failure} iff FAILED; {@code documentUrl}
 * (the e-receipt link) may be missing even when FISCALISED — fetch again later.
 */
public final class Receipt {

    private final String receiptKey;
    private final String providerReceiptId;
    private final ReceiptState state;
    private final FiscalData fiscal;
    private final String documentUrl;
    private final ReceiptFailure failure;

    private Receipt(String receiptKey, String providerReceiptId, ReceiptState state,
                    FiscalData fiscal, String documentUrl, ReceiptFailure failure) {
        if (receiptKey != null && receiptKey.isBlank()) {
            throw new IllegalArgumentException("receiptKey must not be blank");
        }
        if (providerReceiptId == null || providerReceiptId.isBlank()) {
            throw new IllegalArgumentException("providerReceiptId is required");
        }
        this.receiptKey = receiptKey;
        this.providerReceiptId = providerReceiptId;
        this.state = state;
        this.fiscal = fiscal;
        this.documentUrl = documentUrl;
        this.failure = failure;
    }

    public static Receipt pending(String receiptKey, String providerReceiptId) {
        return new Receipt(receiptKey, providerReceiptId, ReceiptState.PENDING, null, null, null);
    }

    public static Receipt fiscalised(String receiptKey, String providerReceiptId, FiscalData fiscal, String documentUrl) {
        if (fiscal == null) {
            throw new IllegalArgumentException("fiscal data is required for a fiscalised receipt");
        }
        return new Receipt(receiptKey, providerReceiptId, ReceiptState.FISCALISED, fiscal, documentUrl, null);
    }

    public static Receipt failed(String receiptKey, String providerReceiptId, ReceiptFailure failure) {
        if (failure == null) {
            throw new IllegalArgumentException("failure is required for a failed receipt");
        }
        return new Receipt(receiptKey, providerReceiptId, ReceiptState.FAILED, null, null, failure);
    }

    /**
     * The idempotency key this receipt was issued under. Always non-null for {@link ReceiptProvider#issue}
     * and {@link ReceiptProvider#find} results. May be null for {@link ReceiptProvider#fetch} and for a
     * webhook result when the provider does not echo it back; consumers then correlate by
     * {@link #providerReceiptId()} instead.
     */
    public String receiptKey() {
        return receiptKey;
    }

    /** The provider's own id (invoice id, document token, device request id). */
    public String providerReceiptId() {
        return providerReceiptId;
    }

    public ReceiptState state() {
        return state;
    }

    /** Non-null iff {@link ReceiptState#FISCALISED}. */
    public FiscalData fiscal() {
        return fiscal;
    }

    /** E-receipt link; may be null even when fiscalised. */
    public String documentUrl() {
        return documentUrl;
    }

    /** Non-null iff {@link ReceiptState#FAILED}. */
    public ReceiptFailure failure() {
        return failure;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Receipt other)) {
            return false;
        }
        return Objects.equals(receiptKey, other.receiptKey) && providerReceiptId.equals(other.providerReceiptId)
                && state == other.state && Objects.equals(fiscal, other.fiscal)
                && Objects.equals(documentUrl, other.documentUrl) && Objects.equals(failure, other.failure);
    }

    @Override
    public int hashCode() {
        return Objects.hash(receiptKey, providerReceiptId, state, fiscal, documentUrl, failure);
    }

    @Override
    public String toString() {
        return "Receipt[receiptKey=" + receiptKey + ", providerReceiptId=" + providerReceiptId + ", state=" + state
                + ", fiscal=" + fiscal + ", documentUrl=" + documentUrl + ", failure=" + failure + "]";
    }
}
