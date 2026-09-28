package pl.commercelink.receipts.api;

/** Shipping is always its own line with its own VAT rate. */
public enum LineKind {
    GOODS,
    SHIPPING,
    SERVICE
}
