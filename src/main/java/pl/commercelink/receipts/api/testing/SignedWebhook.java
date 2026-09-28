package pl.commercelink.receipts.api.testing;

import java.util.Map;

/** A webhook call as the provider would send it: raw body plus HTTP headers. */
public record SignedWebhook(String payload, Map<String, String> headers) {

    public SignedWebhook {
        headers = Map.copyOf(headers);
    }
}
