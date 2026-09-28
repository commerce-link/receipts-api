package pl.commercelink.receipts.api;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoneyTest {

    @Test
    void ofBigDecimalRoundsHalfUpToGrosze() {
        // when / then
        assertEquals(1001, Money.of(new BigDecimal("10.005")).grosze());
        assertEquals(1000, Money.of(new BigDecimal("10.004")).grosze());
    }

    @Test
    void timesMultipliesAndRoundsHalfUp() {
        // given
        Money price = Money.ofGrosze(333);

        // when
        Money half = price.times(new BigDecimal("0.5"));

        // then
        assertEquals(167, half.grosze());
    }

    @Test
    void plusAddsGrosze() {
        // when / then
        assertEquals(Money.ofGrosze(401499), Money.ofGrosze(399900).plus(Money.ofGrosze(1599)));
    }

    @Test
    void plusOverflowThrows() {
        // when / then
        assertThrows(ArithmeticException.class, () -> Money.ofGrosze(Long.MAX_VALUE).plus(Money.ofGrosze(1)));
    }

    @Test
    void toBigDecimalHasScaleTwo() {
        // when / then
        assertEquals(new BigDecimal("15.99"), Money.ofGrosze(1599).toBigDecimal());
    }

    @Test
    void signChecks() {
        // when / then
        assertTrue(Money.ofGrosze(1).isPositive());
        assertFalse(Money.ZERO.isPositive());
        assertTrue(Money.ofGrosze(-1).isNegative());
        assertFalse(Money.ZERO.isNegative());
    }

    @Test
    void comparesByGrosze() {
        // when / then
        assertTrue(Money.ofGrosze(100).compareTo(Money.ofGrosze(200)) < 0);
    }
}
