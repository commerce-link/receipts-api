package pl.commercelink.receipts.api;

/**
 * Lifecycle of a receipt: {@code PENDING → FISCALISED | FAILED}. Both non-pending states are terminal;
 * consumers ignore updates that would move a receipt backwards.
 *
 * <p>Constant NAMES are persisted by consumers — renaming one is a breaking change. Adding a constant
 * requires every consumer to handle it.
 */
public enum ReceiptState {
    /** Accepted by the provider; fiscalisation not confirmed yet (e.g. the device is offline). */
    PENDING,
    /** Registered in the fiscal memory. The e-receipt link may still be missing. */
    FISCALISED,
    /** Definitively not fiscalised. */
    FAILED
}
