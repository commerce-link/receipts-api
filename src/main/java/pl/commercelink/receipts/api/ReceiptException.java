package pl.commercelink.receipts.api;

/**
 * Base failure of a receipt operation. Thrown as such it means nothing was created at the provider
 * (configuration, authentication, a network error before the request was sent): retrying with the
 * SAME receipt key is safe. Subclasses narrow the meaning — catch them first.
 */
public class ReceiptException extends RuntimeException {

    public ReceiptException(String message) {
        super(message);
    }

    public ReceiptException(String message, Throwable cause) {
        super(message, cause);
    }
}
