package pl.commercelink.receipts.api;

import java.util.regex.Pattern;

/**
 * Receipt keys identify one issuing attempt and are the idempotency key sent to the provider.
 * The contract treats them as opaque; the consumer decides the format (e.g. {@code orderId:R1}).
 */
public final class ReceiptKeys {

    private static final Pattern FORMAT = Pattern.compile("[A-Za-z0-9:_-]{1,64}");

    private ReceiptKeys() {
    }

    public static String requireValid(String key) {
        if (key == null || !FORMAT.matcher(key).matches()) {
            throw new ReceiptValidationException("Receipt key must match " + FORMAT.pattern() + ": " + key);
        }
        return key;
    }
}
