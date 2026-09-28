package pl.commercelink.receipts.api;

/**
 * The request may have reached the provider (timeout, 5xx, connection dropped after sending), so the receipt
 * MAY exist and fiscalisation MAY have been ordered. Retry {@link ReceiptProvider#issue} with the SAME receipt
 * key until it returns or throws something other than this exception — whether or not
 * {@link ReceiptProvider#find} sees the receipt: a receipt that exists but was never sent for fiscalisation is
 * completed only by that retry, and a PENDING result from {@code find} does not tell the two apart. Use
 * {@code find} to diagnose, and bound the retries with an operator alert. Never retry with a new key — that
 * risks registering the sale twice.
 */
public class ReceiptOutcomeUnknownException extends ReceiptException {

    public ReceiptOutcomeUnknownException(String message) {
        super(message);
    }

    public ReceiptOutcomeUnknownException(String message, Throwable cause) {
        super(message, cause);
    }
}
