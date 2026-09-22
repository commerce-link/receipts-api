package pl.commercelink.receipts.api;

/**
 * Payment form printed on the receipt; a subset of the forms fiscal devices know
 * (Gotówka, Karta, Przelew, Mobilna, Voucher/Bon, Kredyt, Inna). Adapters map it to the provider's values.
 */
public enum PaymentForm {
    CASH,
    CARD,
    TRANSFER,
    MOBILE,
    VOUCHER,
    CREDIT,
    OTHER
}
