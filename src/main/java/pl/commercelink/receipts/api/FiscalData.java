package pl.commercelink.receipts.api;

import java.time.Instant;

/**
 * Identifiers of a fiscalised receipt. The unique cash register number together with the receipt number
 * identify the receipt (e.g. for an invoice issued for it in KSeF). Providers that do not expose them
 * leave them null; {@code fiscalisedAt} is always known.
 */
public record FiscalData(String cashRegisterUniqueNumber, String receiptNumber, Instant fiscalisedAt) {

    public FiscalData {
        if (fiscalisedAt == null) {
            throw new IllegalArgumentException("fiscalisedAt is required");
        }
    }
}
