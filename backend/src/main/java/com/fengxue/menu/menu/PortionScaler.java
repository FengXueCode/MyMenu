package com.fengxue.menu.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 按用餐人数缩放配料用量：
 * - g/ml 类以 5 为步长取整；
 * - 个/只/根/瓣/片等按整数向上取整；
 * - “适量”“少许”保持原样。
 */
public final class PortionScaler {

    private static final Pattern NUMERIC = Pattern.compile("^(\\d+(?:\\.\\d+)?)\\s*(.*)$");
    private static final List<String> DISCRETE_UNITS = List.of(
            "个", "只", "根", "瓣", "片", "块", "颗", "枚", "条", "张", "杯", "碗", "把", "勺", "朵", "节");
    private static final List<String> MASS_UNITS = List.of("g", "ml", "克", "毫升");

    private PortionScaler() {
    }

    public static List<MenuPlan.IngredientLine> scale(List<MenuPlan.IngredientLine> base,
                                                      int baseServings, int targetServings) {
        List<MenuPlan.IngredientLine> result = new ArrayList<>();
        if (base == null) {
            return result;
        }
        double factor = baseServings <= 0 ? 1.0 : (double) targetServings / baseServings;
        for (MenuPlan.IngredientLine line : base) {
            result.add(scaleOne(line, factor));
        }
        return result;
    }

    private static MenuPlan.IngredientLine scaleOne(MenuPlan.IngredientLine line, double factor) {
        String amount = line.getAmount() == null ? "" : line.getAmount().trim();
        String unit = line.getUnit() == null ? "" : line.getUnit().trim();
        Matcher matcher = NUMERIC.matcher(amount);
        if (!matcher.matches()) {
            return new MenuPlan.IngredientLine(line.getName(), amount, unit);
        }
        double value = Double.parseDouble(matcher.group(1));
        String embeddedUnit = matcher.group(2).trim();
        if (unit.isEmpty()) {
            unit = embeddedUnit;
        }
        double scaled = value * factor;
        String formatted;
        if (unit.isEmpty() || unit.equals("适量") || unit.equals("少许")) {
            formatted = format(scaled);
            return new MenuPlan.IngredientLine(line.getName(), formatted, unit);
        }
        if (DISCRETE_UNITS.contains(unit)) {
            formatted = String.valueOf(Math.max(1, (int) Math.ceil(scaled)));
        } else if (MASS_UNITS.contains(unit)) {
            long rounded = Math.max(5, Math.round(scaled / 5.0) * 5);
            formatted = String.valueOf(rounded);
        } else {
            formatted = format(scaled);
        }
        return new MenuPlan.IngredientLine(line.getName(), formatted, unit);
    }

    private static String format(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.05) {
            return String.valueOf((long) Math.rint(value));
        }
        return String.format("%.1f", value);
    }
}
