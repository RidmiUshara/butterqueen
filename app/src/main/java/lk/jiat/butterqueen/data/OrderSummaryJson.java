package lk.jiat.butterqueen.data;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lk.jiat.butterqueen.model.OrderItem;

/** User-approved machine-readable summary. Never exports name, address, phone or cake notes. */
public final class OrderSummaryJson {
    private static final Gson GSON = new Gson();

    private OrderSummaryJson() { }

    public static String encode(OrderReceipt receipt) {
        if (receipt == null) throw new IllegalArgumentException("Missing order");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("schemaVersion", 1);
        result.put("orderId", receipt.id);
        result.put("createdAtMillis", receipt.createdAtMillis);
        result.put("orderStatus", receipt.orderStatus);
        result.put("paymentMethod", receipt.paymentMethod);
        result.put("paymentStatus", receipt.paymentStatus);
        result.put("subtotal", receipt.subtotal);
        result.put("deliveryFee", receipt.deliveryFee);
        result.put("total", receipt.total);

        List<Map<String, Object>> items = new ArrayList<>();
        for (OrderItem item : receipt.items) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("productName", item.getProductName());
            entry.put("quantity", item.getQuantity());
            entry.put("unitType", item.getUnitType());
            entry.put("unitPrice", item.getUnitPrice());
            entry.put("itemTotal", item.getItemTotal());
            items.add(entry);
        }
        result.put("items", items);
        return GSON.toJson(result);
    }

    public static JsonObject decode(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw new IllegalArgumentException("Empty JSON");
        }
        JsonObject obj = new JsonParser().parse(json).getAsJsonObject();
        if (!obj.has("schemaVersion") || obj.get("schemaVersion").getAsInt() != 1
                || !obj.has("orderId") || !obj.has("items") || !obj.get("items").isJsonArray()) {
            throw new IllegalArgumentException("Invalid ButterQueen JSON summary");
        }
        return obj;
    }
}
