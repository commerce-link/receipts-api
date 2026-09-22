package pl.commercelink.receipts.api;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * What to fiscalise. Built only through {@link #builder()} so fields can be added without breaking
 * callers; {@link Builder#build()} validates, so an invalid request never reaches a provider.
 */
public final class ReceiptRequest {

    private static final int MAX_QUANTITY_SCALE = 3;

    private final String receiptKey;
    private final String orderId;
    private final LocalDateTime saleDate;
    private final ReceiptMedium medium;
    private final List<ReceiptLine> lines;
    private final List<ReceiptPayment> payments;
    private final ReceiptBuyer buyer;

    private ReceiptRequest(Builder builder) {
        this.receiptKey = builder.receiptKey;
        this.orderId = builder.orderId;
        this.saleDate = builder.saleDate;
        this.medium = builder.medium;
        this.lines = List.copyOf(builder.lines);
        this.payments = List.copyOf(builder.payments);
        this.buyer = builder.buyer != null ? builder.buyer : ReceiptBuyer.NONE;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Idempotency key of this issuing attempt. */
    public String receiptKey() {
        return receiptKey;
    }

    public String orderId() {
        return orderId;
    }

    public LocalDateTime saleDate() {
        return saleDate;
    }

    public ReceiptMedium medium() {
        return medium;
    }

    public List<ReceiptLine> lines() {
        return lines;
    }

    public List<ReceiptPayment> payments() {
        return payments;
    }

    /** Never null; {@link ReceiptBuyer#NONE} when not given. */
    public ReceiptBuyer buyer() {
        return buyer;
    }

    /** Sum of the lines' gross totals. */
    public Money totalGross() {
        Money total = Money.ZERO;
        for (ReceiptLine line : lines) {
            total = total.plus(line.totalGross());
        }
        return total;
    }

    /** Throws {@link ReceiptValidationException} on the first broken rule. */
    public void validate() {
        ReceiptKeys.requireValid(receiptKey);
        require(orderId != null && !orderId.isBlank(), "orderId is required");
        require(saleDate != null, "saleDate is required");
        require(medium != null, "medium is required");
        require(!lines.isEmpty(), "at least one line is required");
        for (int i = 0; i < lines.size(); i++) {
            validateLine(i, lines.get(i));
        }
        require(!payments.isEmpty(), "at least one payment is required");
        Money paid = Money.ZERO;
        for (int i = 0; i < payments.size(); i++) {
            ReceiptPayment payment = payments.get(i);
            require(payment.form() != null, "payment " + i + ": form is required");
            require(payment.amount() != null && payment.amount().isPositive(), "payment " + i + ": amount must be positive");
            paid = paid.plus(payment.amount());
        }
        Money total = totalGross();
        require(total.equals(paid), "lines total " + total.toBigDecimal() + " != payments total " + paid.toBigDecimal());
    }

    private static void validateLine(int index, ReceiptLine line) {
        String prefix = "line " + index + ": ";
        require(line.name() != null && !line.name().strip().isEmpty(), prefix + "name is required");
        require(line.kind() != null, prefix + "kind is required");
        require(line.vatRate() != null, prefix + "vatRate is required");
        require(line.quantity() != null && line.quantity().signum() > 0, prefix + "quantity must be positive");
        require(line.quantity().stripTrailingZeros().scale() <= MAX_QUANTITY_SCALE,
                prefix + "quantity allows at most " + MAX_QUANTITY_SCALE + " decimal places");
        require(line.unitGross() != null && !line.unitGross().isNegative(), prefix + "unitGross must not be negative");
        require(line.unitGross().times(line.quantity()).equals(line.totalGross()),
                prefix + "totalGross " + line.totalGross() + " != unitGross x quantity");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new ReceiptValidationException(message);
        }
    }

    public static final class Builder {

        private String receiptKey;
        private String orderId;
        private LocalDateTime saleDate;
        private ReceiptMedium medium = ReceiptMedium.ELECTRONIC;
        private final List<ReceiptLine> lines = new ArrayList<>();
        private final List<ReceiptPayment> payments = new ArrayList<>();
        private ReceiptBuyer buyer = ReceiptBuyer.NONE;

        private Builder() {
        }

        public Builder receiptKey(String receiptKey) {
            this.receiptKey = receiptKey;
            return this;
        }

        public Builder orderId(String orderId) {
            this.orderId = orderId;
            return this;
        }

        public Builder saleDate(LocalDateTime saleDate) {
            this.saleDate = saleDate;
            return this;
        }

        public Builder medium(ReceiptMedium medium) {
            this.medium = medium;
            return this;
        }

        public Builder line(ReceiptLine line) {
            this.lines.add(line);
            return this;
        }

        public Builder line(ReceiptLine.Builder line) {
            return line(line.build());
        }

        public Builder payment(ReceiptPayment payment) {
            this.payments.add(payment);
            return this;
        }

        public Builder payment(ReceiptPayment.Builder payment) {
            return payment(payment.build());
        }

        public Builder buyer(ReceiptBuyer buyer) {
            this.buyer = buyer;
            return this;
        }

        public ReceiptRequest build() {
            ReceiptRequest request = new ReceiptRequest(this);
            request.validate();
            return request;
        }
    }
}
