package lk.jiat.butterqueen.model;

public class OrderItem {

    private String orderItemId;
    private String productId;
    private String productName;
    private String productImageUrl;
    private double unitPrice;
    private double quantity;
    private String unitType;
    private String selectedSize;
    private String selectedFlavor;
    private String cakeMessage;
    private double itemTotal;

    public OrderItem() {
    }

    public OrderItem(String orderItemId, String productId, String productName,
                     String productImageUrl, double unitPrice, double quantity,
                     String unitType, String selectedSize, String selectedFlavor,
                     String cakeMessage, double itemTotal) {
        this.orderItemId = orderItemId;
        this.productId = productId;
        this.productName = productName;
        this.productImageUrl = productImageUrl;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.unitType = unitType;
        this.selectedSize = selectedSize;
        this.selectedFlavor = selectedFlavor;
        this.cakeMessage = cakeMessage;
        this.itemTotal = itemTotal;
    }

    public String getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(String orderItemId) {
        this.orderItemId = orderItemId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductImageUrl() {
        return productImageUrl;
    }

    public void setProductImageUrl(String productImageUrl) {
        this.productImageUrl = productImageUrl;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public String getUnitType() {
        return unitType;
    }

    public void setUnitType(String unitType) {
        this.unitType = unitType;
    }

    public String getSelectedSize() {
        return selectedSize;
    }

    public void setSelectedSize(String selectedSize) {
        this.selectedSize = selectedSize;
    }

    public String getSelectedFlavor() {
        return selectedFlavor;
    }

    public void setSelectedFlavor(String selectedFlavor) {
        this.selectedFlavor = selectedFlavor;
    }

    public String getCakeMessage() {
        return cakeMessage;
    }

    public void setCakeMessage(String cakeMessage) {
        this.cakeMessage = cakeMessage;
    }

    public double getItemTotal() {
        return itemTotal;
    }

    public void setItemTotal(double itemTotal) {
        this.itemTotal = itemTotal;
    }
}