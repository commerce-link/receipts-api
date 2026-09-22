package pl.commercelink.receipts.api;

/**
 * VAT rate of a receipt line. Adapters map it to the PTU letter (A–G) configured on the fiscal device;
 * the statutory default is A=23, B=8, C=5, D=0, E=exempt.
 */
public enum VatRate {
    VAT_23(1.23),
    VAT_8(1.08),
    VAT_5(1.05),
    VAT_0(1.0),
    /** "zw" — never inferred from a multiplier, callers must choose it explicitly. */
    EXEMPT(Double.NaN);

    private static final double TOLERANCE = 1e-9;

    private final double multiplier;

    VatRate(double multiplier) {
        this.multiplier = multiplier;
    }

    /** Maps a gross/net multiplier such as {@code 1.23} to a rate. Never returns {@link #EXEMPT}. */
    public static VatRate fromMultiplier(double multiplier) {
        for (VatRate rate : values()) {
            if (Math.abs(rate.multiplier - multiplier) < TOLERANCE) {
                return rate;
            }
        }
        throw new IllegalArgumentException("Unsupported VAT multiplier: " + multiplier);
    }
}
