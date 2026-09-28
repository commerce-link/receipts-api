package pl.commercelink.receipts.api;

/** Why a receipt was not fiscalised; {@code code} is the provider's or device's code and may be null. */
public record ReceiptFailure(String code, String message) {

    public ReceiptFailure {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message is required");
        }
    }
}
