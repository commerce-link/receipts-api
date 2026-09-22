package pl.commercelink.receipts.api;

import java.util.Objects;

/**
 * Optional buyer data. {@code email} is passed because some providers require it for an e-receipt;
 * delivering the receipt to the buyer is the consumer's job. {@code taxId} is the NIP printed at the
 * buyer's request.
 */
public final class ReceiptBuyer {

    public static final ReceiptBuyer NONE = builder().build();

    private final String email;
    private final String taxId;

    private ReceiptBuyer(Builder builder) {
        this.email = builder.email;
        this.taxId = builder.taxId;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** May be null. */
    public String email() {
        return email;
    }

    /** May be null. */
    public String taxId() {
        return taxId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReceiptBuyer other)) {
            return false;
        }
        return Objects.equals(email, other.email) && Objects.equals(taxId, other.taxId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, taxId);
    }

    @Override
    public String toString() {
        return "ReceiptBuyer[email=" + email + ", taxId=" + taxId + "]";
    }

    public static final class Builder {

        private String email;
        private String taxId;

        private Builder() {
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder taxId(String taxId) {
            this.taxId = taxId;
            return this;
        }

        public ReceiptBuyer build() {
            return new ReceiptBuyer(this);
        }
    }
}
