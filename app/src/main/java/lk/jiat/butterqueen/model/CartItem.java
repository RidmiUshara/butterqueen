package lk.jiat.butterqueen.model;

public class CartItem {

    private String cartItemId;
    private String productId;
    private String productName;
    private String productImageUrl;
    private double productPrice;
    private String unitType;

    private double quantity; // supports 0.5kg, 1kg, 2pcs etc.
    private String selectedSize;
    private String selectedFlavor;
    private String cakeMessage;

    private long addedAtMillis;

    public CartItem() {
    }

    public CartItem(String cartItemId, String productId, String productName,
                    String productImageUrl, double productPrice, String unitType,
                    double quantity, String selectedSize, String selectedFlavor,
                    String cakeMessage, long addedAtMillis) {
        this.cartItemId = cartItemId;
        this.productId = productId;
        this.productName = productName;
        this.productImageUrl = productImageUrl;
        this.productPrice = productPrice;
        this.unitType = unitType;
        this.quantity = quantity;
        this.selectedSize = selectedSize;
        this.selectedFlavor = selectedFlavor;
        this.cakeMessage = cakeMessage;
        this.addedAtMillis = addedAtMillis;
    }

    public String getCartItemId() {
        return cartItemId;
    }

    public void setCartItemId(String cartItemId) {
        this.cartItemId = cartItemId;
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

    public double getProductPrice() {
        return productPrice;
    }

    public void setProductPrice(double productPrice) {
        this.productPrice = productPrice;
    }

    public String getUnitType() {
        return unitType;
    }

    public void setUnitType(String unitType) {
        this.unitType = unitType;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
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

    public long getAddedAtMillis() {
        return addedAtMillis;
    }

    public void setAddedAtMillis(long addedAtMillis) {
        this.addedAtMillis = addedAtMillis;
    }

    public double getItemTotal() {
        return productPrice * quantity;
    }
}