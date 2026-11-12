package com.nightwalker12.foxxe;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.regex.Pattern;

final class BreadUnits {
    private static final Pattern NUMBER = Pattern.compile("(?:[0-9]+(?:[.,][0-9]+)?|[.,][0-9]+)");
    static final BigDecimal MAX_WEIGHT = new BigDecimal("1000000");
    static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private BreadUnits() {}

    static BigDecimal parse(String input, BigDecimal max, boolean allowZero) {
        String text = input.trim();
        if (text.length() > 18 || !NUMBER.matcher(text).matches()) {
            return null;
        }
        BigDecimal value = new BigDecimal(text.replace(',', '.'));
        if (value.compareTo(max) > 0 || (!allowZero && value.signum() == 0)) {
            return null;
        }
        return value;
    }

    static BigDecimal portionCarbs(BigDecimal carbsPer100, BigDecimal grams) {
        return carbsPer100.multiply(grams).movePointLeft(2);
    }

    static BigDecimal units(BigDecimal portionCarbs, int gramsPerUnit) {
        if (gramsPerUnit < 10 || gramsPerUnit > 12) {
            throw new IllegalArgumentException("Expected 10–12 g per bread unit");
        }
        return portionCarbs.divide(BigDecimal.valueOf(gramsPerUnit), MathContext.DECIMAL128);
    }

    static String format(BigDecimal value) {
        if (value.signum() > 0 && value.compareTo(new BigDecimal("0.01")) < 0) {
            return "< 0,01";
        }
        DecimalFormat decimal = new DecimalFormat("0.##",
                DecimalFormatSymbols.getInstance(Locale.forLanguageTag("ru-RU")));
        decimal.setRoundingMode(RoundingMode.HALF_UP);
        return decimal.format(value);
    }
}
