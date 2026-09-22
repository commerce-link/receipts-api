package pl.commercelink.receipts.api;

/**
 * The provider definitively refused the receipt (e.g. unknown point of sale, totals that do not add up);
 * nothing was created or fiscalised. Do not retry blindly: fix the cause and issue with a NEW receipt key.
 */
public class ReceiptRejectedException extends ReceiptException {

    private final String code;

    public ReceiptRejectedException(String code, String message) {
        super(message);
        this.code = code;
    }

    public ReceiptRejectedException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /** Provider error code, if the provider returns one; may be null. */
    public String code() {
        return code;
    }
}
