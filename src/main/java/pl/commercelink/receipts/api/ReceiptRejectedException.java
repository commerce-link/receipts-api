package pl.commercelink.receipts.api;

/**
 * The provider definitively refused the receipt, or the attempt is certainly dead (e.g. unknown point of sale,
 * totals that do not add up, the fiscal printer refused the sale): nothing was fiscalised under this key. A
 * non-fiscal document may remain at the provider. Do not retry blindly: fix the cause and issue with a NEW
 * receipt key.
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
