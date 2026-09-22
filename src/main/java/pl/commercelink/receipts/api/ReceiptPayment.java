package pl.commercelink.receipts.api;

import java.util.Objects;

/** One payment covering (part of) the receipt total. */
public final class ReceiptPayment {

    private final PaymentForm form;
    private final Money amount;
    private final String label;

    private ReceiptPayment(Builder builder) {
        this.form = builder.form;
        this.amount = builder.amount;
        this.label = builder.label;
    }

    public static Builder of(PaymentForm form, Money amount) {
        return new Builder(form, amount);
    }

    public PaymentForm form() {
        return form;
    }

    public Money amount() {
        return amount;
    }

    /** The payment's own name, e.g. "Przelewy24"; may be null. */
    public String label() {
        return label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReceiptPayment other)) {
            return false;
        }
        return form == other.form && Objects.equals(amount, other.amount) && Objects.equals(label, other.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(form, amount, label);
    }

    @Override
    public String toString() {
        return "ReceiptPayment[form=" + form + ", amount=" + amount + ", label=" + label + "]";
    }

    public static final class Builder {

        private final PaymentForm form;
        private final Money amount;
        private String label;

        private Builder(PaymentForm form, Money amount) {
            this.form = form;
            this.amount = amount;
        }

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public ReceiptPayment build() {
            return new ReceiptPayment(this);
        }
    }
}
