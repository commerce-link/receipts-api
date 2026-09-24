package pl.commercelink.receipts.api;

import java.math.BigDecimal;
import java.util.Objects;

/** One receipt position with gross amounts. Built through {@link #goods}, {@link #shipping} or {@link #service}. */
public final class ReceiptLine {

    private final String name;
    private final BigDecimal quantity;
    private final Money unitGross;
    private final Money totalGross;
    private final VatRate vatRate;
    private final LineKind kind;
    private final String sku;
    private final String ean;

    private ReceiptLine(Builder builder) {
        this.name = builder.name;
        this.quantity = builder.quantity;
        this.unitGross = builder.unitGross;
        this.totalGross = builder.unitGross != null && builder.quantity != null
                ? builder.unitGross.times(builder.quantity) : null;
        this.vatRate = builder.vatRate;
        this.kind = builder.kind;
        this.sku = builder.sku;
        this.ean = builder.ean;
    }

    public static Builder goods(String name, BigDecimal quantity, Money unitGross, VatRate vatRate) {
        return new Builder(LineKind.GOODS, name, quantity, unitGross, vatRate);
    }

    public static Builder shipping(String name, Money gross, VatRate vatRate) {
        return new Builder(LineKind.SHIPPING, name, BigDecimal.ONE, gross, vatRate);
    }

    public static Builder service(String name, BigDecimal quantity, Money unitGross, VatRate vatRate) {
        return new Builder(LineKind.SERVICE, name, quantity, unitGross, vatRate);
    }

    public String name() {
        return name;
    }

    public BigDecimal quantity() {
        return quantity;
    }

    public Money unitGross() {
        return unitGross;
    }

    /** Always {@code unitGross × quantity} rounded HALF_UP. */
    public Money totalGross() {
        return totalGross;
    }

    public VatRate vatRate() {
        return vatRate;
    }

    public LineKind kind() {
        return kind;
    }

    /** Seller's product code; may be null. */
    public String sku() {
        return sku;
    }

    /** May be null. */
    public String ean() {
        return ean;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReceiptLine other)) {
            return false;
        }
        return Objects.equals(name, other.name) && Objects.equals(quantity, other.quantity)
                && Objects.equals(unitGross, other.unitGross) && Objects.equals(totalGross, other.totalGross)
                && vatRate == other.vatRate && kind == other.kind
                && Objects.equals(sku, other.sku) && Objects.equals(ean, other.ean);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, quantity, unitGross, totalGross, vatRate, kind, sku, ean);
    }

    @Override
    public String toString() {
        return "ReceiptLine[name=" + name + ", quantity=" + quantity + ", unitGross=" + unitGross
                + ", totalGross=" + totalGross + ", vatRate=" + vatRate + ", kind=" + kind
                + ", sku=" + sku + ", ean=" + ean + "]";
    }

    public static final class Builder {

        private final LineKind kind;
        private final String name;
        private final BigDecimal quantity;
        private final Money unitGross;
        private final VatRate vatRate;
        private String sku;
        private String ean;

        private Builder(LineKind kind, String name, BigDecimal quantity, Money unitGross, VatRate vatRate) {
            this.kind = kind;
            this.name = name;
            this.quantity = quantity;
            this.unitGross = unitGross;
            this.vatRate = vatRate;
        }

        public Builder sku(String sku) {
            this.sku = sku;
            return this;
        }

        public Builder ean(String ean) {
            this.ean = ean;
            return this;
        }

        public ReceiptLine build() {
            return new ReceiptLine(this);
        }
    }
}
