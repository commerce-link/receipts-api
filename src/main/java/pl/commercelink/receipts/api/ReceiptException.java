package pl.commercelink.receipts.api;

/**
 * Base failure of a receipt operation. Thrown as such it means the call did nothing that a retry could
 * duplicate (configuration, authentication, a network error before sending, a failed read), so retrying with
 * the SAME receipt key is safe. The receipt may already exist at the provider from an earlier call.
 * Subclasses narrow the meaning — catch them first.
 */
public class ReceiptException extends RuntimeException {

    public ReceiptException(String message) {
        super(message);
    }

    public ReceiptException(String message, Throwable cause) {
        super(message, cause);
    }
}
