package lk.jiat.butterqueen;

import org.junit.Test;

import static org.junit.Assert.*;

import com.google.gson.JsonObject;

import lk.jiat.butterqueen.data.OrderReceipt;
import lk.jiat.butterqueen.data.OrderSummaryJson;
import lk.jiat.butterqueen.model.OrderItem;

public class OrderSummaryJsonTest {
    @Test
    public void gsonRoundTripExcludesCustomerContactInformation() {
        OrderReceipt receipt = new OrderReceipt();
        receipt.id = "ORDER-123";
        receipt.customerName = "Private Customer";
        receipt.customerPhone = "0779999999";
        receipt.deliveryAddress = "Private address";
        receipt.note = "Sensitive note";
        receipt.paymentMethod = "cash_on_delivery";
        receipt.paymentStatus = "pending";
        receipt.orderStatus = "placed";
        receipt.subtotal = 1500;
        receipt.deliveryFee = 250;
        receipt.total = 1750;

        OrderItem item = new OrderItem();
        item.setProductName("Vanilla Cake");
        item.setQuantity(1.5);
        item.setUnitType("kg");
        item.setUnitPrice(1000);
        item.setItemTotal(1500);
        receipt.items.add(item);

        String encoded = OrderSummaryJson.encode(receipt);
        JsonObject decoded = OrderSummaryJson.decode(encoded);

        assertEquals("ORDER-123", decoded.get("orderId").getAsString());
        assertEquals(1750, decoded.get("total").getAsDouble(), 0.001);
        assertEquals(1, decoded.getAsJsonArray("items").size());
        assertEquals("Vanilla Cake", decoded.getAsJsonArray("items")
                .get(0).getAsJsonObject().get("productName").getAsString());
        assertFalse(encoded.contains("0779999999"));
        assertFalse(encoded.contains("Private Customer"));
        assertFalse(encoded.contains("Private address"));
        assertFalse(encoded.contains("Sensitive note"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void decodeRejectsUnsupportedSchema() {
        OrderSummaryJson.decode("{\"schemaVersion\":9,\"orderId\":\"X\",\"items\":[]}");
    }
}
