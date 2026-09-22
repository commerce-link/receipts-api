package pl.commercelink.receipts.api;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * An amount in PLN grosze. Fiscal receipts are always issued in PLN, so there is no currency.
 * Rounding to grosze happens once, when a {@link BigDecimal} enters the contract, always HALF_UP.
 */
public record Money(long grosze) implements Comparable<Money> {

    public static final Money ZERO = new Money(0);

    public static Money ofGrosze(long grosze) {
        return new Money(grosze);
    }

    /** Converts a PLN amount, rounding HALF_UP to whole grosze. */
    public static Money of(BigDecimal amount) {
        Objects.requireNonNull(amount, "amount");
        return new Money(amount.setScale(2, RoundingMode.HALF_UP).unscaledValue().longValueExact());
    }

    public Money plus(Money other) {
        return new Money(Math.addExact(grosze, other.grosze));
    }

    /** Multiplies by a quantity and rounds HALF_UP to whole grosze. */
    public Money times(BigDecimal quantity) {
        Objects.requireNonNull(quantity, "quantity");
        return of(toBigDecimal().multiply(quantity));
    }

    public BigDecimal toBigDecimal() {
        return BigDecimal.valueOf(grosze, 2);
    }

    public boolean isPositive() {
        return grosze > 0;
    }

    public boolean isNegative() {
        return grosze < 0;
    }

    @Override
    public int compareTo(Money other) {
        return Long.compare(grosze, other.grosze);
    }
}
