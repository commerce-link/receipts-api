package pl.commercelink.receipts.api;

/**
 * The request may have reached the provider (timeout, 5xx, connection dropped after sending), so the
 * receipt MAY exist. Probe with {@link ReceiptProvider#find(String)}; if nothing is found, retry with the
 * SAME receipt key. Never retry with a new key — that risks registering the sale twice.
 */
public class ReceiptOutcomeUnknownException extends ReceiptException {

    public ReceiptOutcomeUnknownException(String message) {
        super(message);
    }

    public ReceiptOutcomeUnknownException(String message, Throwable cause) {
        super(message, cause);
    }
}
