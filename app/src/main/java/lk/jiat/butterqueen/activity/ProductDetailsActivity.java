package lk.jiat.butterqueen.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

import lk.jiat.butterqueen.R;
import lk.jiat.butterqueen.data.CartManager;
import lk.jiat.butterqueen.data.WishlistManager;
import lk.jiat.butterqueen.model.CartItem;
import lk.jiat.butterqueen.model.Product;
import lk.jiat.butterqueen.model.UnitPricing;

public class ProductDetailsActivity extends AppCompatActivity {

    private ImageButton backButton, wishlistButton, minusQtyButton, plusQtyButton;
    private ImageView productImageView;
    private TextView productNameTextView, categoryTextView, cakeTypeTextView,
            productPriceTextView, productDescriptionTextView, quantityTextView,
            quantitySectionTitle, selectionTotalTextView, availabilityTextView;
    private MaterialButton addToCartButton;
    private Product selectedProduct;
    private String unitType = UnitPricing.PCS;
    private double quantity = 1.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_product_details);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            var systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        backButton = findViewById(R.id.backButton);
        wishlistButton = findViewById(R.id.wishlistButton);
        minusQtyButton = findViewById(R.id.minusQtyButton);
        plusQtyButton = findViewById(R.id.plusQtyButton);
        productImageView = findViewById(R.id.detailsProductImageView);
        productNameTextView = findViewById(R.id.detailsProductNameTextView);
        categoryTextView = findViewById(R.id.detailsCategoryTextView);
        cakeTypeTextView = findViewById(R.id.detailsCakeTypeTextView);
        productPriceTextView = findViewById(R.id.detailsProductPriceTextView);
        productDescriptionTextView = findViewById(R.id.detailsProductDescriptionTextView);
        quantityTextView = findViewById(R.id.quantityTextView);
        quantitySectionTitle = findViewById(R.id.quantitySectionTitle);
        selectionTotalTextView = findViewById(R.id.selectionTotalTextView);
        availabilityTextView = findViewById(R.id.availabilityTextView);
        addToCartButton = findViewById(R.id.addToCartButton);

        backButton.setOnClickListener(v -> finish());
        wishlistButton.setOnClickListener(v -> {
            if (selectedProduct == null) return;
            boolean added = WishlistManager.toggleWishlist(selectedProduct);
            updateWishlistIcon();
            Toast.makeText(this, added ? "Added to wishlist" : "Removed from wishlist",
                    Toast.LENGTH_SHORT).show();
        });
        minusQtyButton.setOnClickListener(v -> {
            quantity = Math.max(UnitPricing.step(unitType), quantity - UnitPricing.step(unitType));
            updateSelection();
        });
        plusQtyButton.setOnClickListener(v -> {
            quantity += UnitPricing.step(unitType);
            updateSelection();
        });
        addToCartButton.setOnClickListener(v -> addToCart());

        String productId = getIntent().getStringExtra("product_id");
        if (productId == null || productId.trim().isEmpty()) {
            Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        FirebaseFirestore.getInstance().collection("products").document(productId)
                .get()
                .addOnSuccessListener(doc -> {
                    selectedProduct = doc.toObject(Product.class);
                    if (!doc.exists() || selectedProduct == null) {
                        Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    selectedProduct.setId(doc.getId());
                    if (doc.getBoolean("available") == null) {
                        selectedProduct.setAvailable(true);
                    }
                    bindData();
                })
                .addOnFailureListener(error -> {
                    Toast.makeText(this, "Failed to load product", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void bindData() {
        unitType = UnitPricing.normalize(selectedProduct.getUnitType(),
                selectedProduct.getCategoryName());
        quantity = UnitPricing.step(unitType);
        productNameTextView.setText(selectedProduct.getName());
        categoryTextView.setText(selectedProduct.getCategoryName());
        productDescriptionTextView.setText(selectedProduct.getDescription());
        productPriceTextView.setText(UnitPricing.priceLabel(selectedProduct.getPrice(), unitType));
        quantitySectionTitle.setText(UnitPricing.isKg(unitType)
                ? "Choose weight • 0.5 kg steps" : "Choose number of pieces");
        String cakeType = selectedProduct.getCakeType();
        cakeTypeTextView.setVisibility(UnitPricing.isKg(unitType)
                && cakeType != null && !cakeType.isEmpty() ? View.VISIBLE : View.GONE);
        if (cakeType != null && !cakeType.isEmpty()) {
            cakeTypeTextView.setText(cakeType.substring(0, 1).toUpperCase(Locale.getDefault())
                    + cakeType.substring(1) + " cake");
        }
        boolean available = selectedProduct.isAvailable()
                && UnitPricing.isSupported(selectedProduct.getUnitType())
                && Double.isFinite(selectedProduct.getPrice())
                && selectedProduct.getPrice() > 0;
        addToCartButton.setEnabled(available);
        availabilityTextView.setVisibility(available ? View.GONE : View.VISIBLE);
        updateSelection();
        updateWishlistIcon();

        if (selectedProduct.getImageUrl() != null && !selectedProduct.getImageUrl().trim().isEmpty()) {
            Glide.with(this).load(selectedProduct.getImageUrl())
                    .placeholder(R.drawable.cake_24px)
                    .error(R.drawable.cake_24px).into(productImageView);
        } else if (selectedProduct.getImageResId() != 0) {
            productImageView.setImageResource(selectedProduct.getImageResId());
        } else {
            productImageView.setImageResource(R.drawable.cake_24px);
        }
    }

    private void updateSelection() {
        quantityTextView.setText(UnitPricing.quantityLabel(quantity, unitType));
        minusQtyButton.setEnabled(quantity > UnitPricing.step(unitType));
        if (selectedProduct != null) {
            selectionTotalTextView.setText(String.format(Locale.getDefault(),
                    "Selection total: Rs. %.2f",
                    UnitPricing.itemTotal(selectedProduct.getPrice(), quantity)));
        }
    }

    private void addToCart() {
        if (selectedProduct == null || !addToCartButton.isEnabled()) return;
        CartItem item = new CartItem("", selectedProduct.getId(), selectedProduct.getName(),
                selectedProduct.getImageUrl(), selectedProduct.getPrice(), unitType,
                quantity, "", "", "", System.currentTimeMillis());

        addToCartButton.setEnabled(false);
        addToCartButton.setText("Adding...");
        CartManager.addToCart(item, new CartManager.SimpleCallback() {
            @Override public void onSuccess() {
                addToCartButton.setEnabled(true);
                addToCartButton.setText("Add to Cart");
                Toast.makeText(ProductDetailsActivity.this,
                        UnitPricing.quantityLabel(quantity, unitType) + " added to cart",
                        Toast.LENGTH_SHORT).show();
            }
            @Override public void onFailure(String message) {
                addToCartButton.setEnabled(true);
                addToCartButton.setText("Add to Cart");
                Toast.makeText(ProductDetailsActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateWishlistIcon() {
        if (selectedProduct == null) return;
        wishlistButton.setColorFilter(ContextCompat.getColor(this,
                WishlistManager.isWishlisted(selectedProduct.getId())
                        ? R.color.md_theme_primary : android.R.color.white));
    }
}
