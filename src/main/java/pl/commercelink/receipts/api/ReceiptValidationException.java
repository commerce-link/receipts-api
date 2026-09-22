package pl.commercelink.receipts.api;

/** The request or key is invalid; thrown before any remote call. Retrying the same input cannot succeed. */
public class ReceiptValidationException extends ReceiptException {

    public ReceiptValidationException(String message) {
        super(message);
    }
}
