package lk.jiat.butterqueen.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/** The catalog price is per kilogram for cakes and per item for other treats. */
public final class UnitPricing {
    public static final String KG = "kg";
    public static final String PCS = "pcs";

    private UnitPricing() {}

    public static String normalize(String unitType, String categoryName) {
        if (KG.equalsIgnoreCase(unitType)) return KG;
        if (PCS.equalsIgnoreCase(unitType) || "piece".equalsIgnoreCase(unitType)
                || "qty".equalsIgnoreCase(unitType)) return PCS;
        return "cakes".equalsIgnoreCase(categoryName) ? KG : PCS;
    }

    public static boolean isSupported(String unitType) {
        return unitType == null || unitType.trim().isEmpty()
                || KG.equalsIgnoreCase(unitType) || PCS.equalsIgnoreCase(unitType)
                || "piece".equalsIgnoreCase(unitType) || "qty".equalsIgnoreCase(unitType);
    }

    public static boolean isKg(String unitType) {
        return KG.equalsIgnoreCase(unitType);
    }

    public static double step(String unitType) {
        return isKg(unitType) ? 0.5 : 1.0;
    }

    public static double normalizeQuantity(double quantity, String unitType) {
        if (!Double.isFinite(quantity) || quantity < step(unitType)) return step(unitType);
        return Math.round(quantity / step(unitType)) * step(unitType);
    }

    public static String quantityLabel(double quantity, String unitType) {
        if (isKg(unitType)) {
            return String.format(Locale.getDefault(), "%.1f kg", quantity);
        }
        return String.format(Locale.getDefault(), "%d pcs", Math.round(quantity));
    }

    public static String priceLabel(double price, String unitType) {
        return String.format(Locale.getDefault(), "Rs. %.2f / %s",
                price, isKg(unitType) ? "kg" : "pc");
    }

    public static double roundMoney(double amount) {
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public static double itemTotal(double unitPrice, double quantity) {
        return BigDecimal.valueOf(unitPrice)
                .multiply(BigDecimal.valueOf(quantity))
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
