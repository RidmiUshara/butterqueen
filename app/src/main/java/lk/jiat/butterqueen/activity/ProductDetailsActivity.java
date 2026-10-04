package lk.jiat.butterqueen.activity;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

import lk.jiat.butterqueen.R;
import lk.jiat.butterqueen.data.CartManager;
import lk.jiat.butterqueen.data.WishlistManager;
import lk.jiat.butterqueen.model.CartItem;
import lk.jiat.butterqueen.model.Product;

public class ProductDetailsActivity extends AppCompatActivity {

    private ImageButton backButton, wishlistButton, minusQtyButton, plusQtyButton;
    private ImageView productImageView;
    private TextView productNameTextView, categoryTextView, productPriceTextView,
            productDescriptionTextView, quantityTextView;
    private LinearLayout weightSectionLayout, quantitySectionLayout;
    private ChipGroup weightChipGroup;
    private MaterialButton addToCartButton;

    private FirebaseFirestore firestore;

    private Product selectedProduct;
    private String selectedUnit = "500g";
    private int selectedQuantity = 1;

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

        firestore = FirebaseFirestore.getInstance();

        initViews();
        setupActions();

        String productId = getIntent().getStringExtra("product_id");
        if (productId == null || productId.trim().isEmpty()) {
            Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadProduct(productId);
    }

    private void initViews() {
        backButton = findViewById(R.id.backButton);
        wishlistButton = findViewById(R.id.wishlistButton);
        minusQtyButton = findViewById(R.id.minusQtyButton);
        plusQtyButton = findViewById(R.id.plusQtyButton);

        productImageView = findViewById(R.id.detailsProductImageView);
        productNameTextView = findViewById(R.id.detailsProductNameTextView);
        categoryTextView = findViewById(R.id.detailsCategoryTextView);
        productPriceTextView = findViewById(R.id.detailsProductPriceTextView);
        productDescriptionTextView = findViewById(R.id.detailsProductDescriptionTextView);
        quantityTextView = findViewById(R.id.quantityTextView);

        weightSectionLayout = findViewById(R.id.weightSectionLayout);
        quantitySectionLayout = findViewById(R.id.quantitySectionLayout);
        weightChipGroup = findViewById(R.id.weightChipGroup);
        addToCartButton = findViewById(R.id.addToCartButton);
    }

    private void loadProduct(String productId) {
        firestore.collection("products")
                .document(productId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    selectedProduct = documentSnapshot.toObject(Product.class);
                    if (selectedProduct == null) {
                        Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    selectedProduct.setId(documentSnapshot.getId());
                    bindData();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load product", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void bindData() {
        productNameTextView.setText(selectedProduct.getName());
        categoryTextView.setText(selectedProduct.getCategoryName());
        productDescriptionTextView.setText(selectedProduct.getDescription());
        productPriceTextView.setText(
                String.format(Locale.getDefault(), "Rs. %.2f", selectedProduct.getPrice())
        );

        if (selectedProduct.getImageUrl() != null && !selectedProduct.getImageUrl().trim().isEmpty()) {
            Glide.with(this)
                    .load(selectedProduct.getImageUrl())
                    .placeholder(R.drawable.cake_24px)
                    .error(R.drawable.cake_24px)
                    .into(productImageView);
        } else if (selectedProduct.getImageResId() != 0) {
            productImageView.setImageResource(selectedProduct.getImageResId());
        } else {
            productImageView.setImageResource(R.drawable.cake_24px);
        }

        boolean isCake = isKgProduct();

        weightSectionLayout.setVisibility(isCake ? android.view.View.VISIBLE : android.view.View.GONE);
        quantitySectionLayout.setVisibility(isCake ? android.view.View.GONE : android.view.View.VISIBLE);

        quantityTextView.setText(String.valueOf(selectedQuantity));
        updateWishlistIcon();

        int checkedChipId = weightChipGroup.getCheckedChipId();
        if (checkedChipId != -1) {
            Chip checkedChip = findViewById(checkedChipId);
            if (checkedChip != null) {
                selectedUnit = checkedChip.getText().toString().trim();
            }
        }
    }

    private void setupActions() {
        backButton.setOnClickListener(v -> finish());

        wishlistButton.setOnClickListener(v -> {
            if (selectedProduct == null) return;

            boolean isWishlisted = WishlistManager.toggleWishlist(selectedProduct);
            updateWishlistIcon();

            Toast.makeText(
                    this,
                    isWishlisted ? "Added to wishlist" : "Removed from wishlist",
                    Toast.LENGTH_SHORT
            ).show();
        });

        weightChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds != null && !checkedIds.isEmpty()) {
                Chip selectedChip = findViewById(checkedIds.get(0));
                if (selectedChip != null) {
                    selectedUnit = selectedChip.getText().toString().trim();
                }
            }
        });

        minusQtyButton.setOnClickListener(v -> {
            if (selectedQuantity > 1) {
                selectedQuantity--;
                quantityTextView.setText(String.valueOf(selectedQuantity));
            }
        });

        plusQtyButton.setOnClickListener(v -> {
            selectedQuantity++;
            quantityTextView.setText(String.valueOf(selectedQuantity));
        });

        addToCartButton.setOnClickListener(v -> {
            if (selectedProduct == null) {
                Toast.makeText(this, "Product not loaded yet", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean isCake = isKgProduct();

            double cartQuantity = isCake ? parseWeightToKg(selectedUnit) : selectedQuantity;
            String unitType = selectedProduct.getUnitType() != null
                    ? selectedProduct.getUnitType()
                    : (isCake ? "kg" : "qty");

            CartItem cartItem = new CartItem(
                    "",
                    selectedProduct.getId(),
                    selectedProduct.getName(),
                    selectedProduct.getImageUrl(),
                    selectedProduct.getPrice(),
                    unitType,
                    cartQuantity,
                    isCake ? selectedUnit : "",
                    "",
                    "",
                    System.currentTimeMillis()
            );

            addToCartButton.setEnabled(false);
            addToCartButton.setText("Adding...");

            CartManager.addToCart(cartItem, new CartManager.SimpleCallback() {
                @Override
                public void onSuccess() {
                    addToCartButton.setEnabled(true);
                    addToCartButton.setText("Add to Cart");
                    Toast.makeText(
                            ProductDetailsActivity.this,
                            selectedProduct.getName() + " added to cart",
                            Toast.LENGTH_SHORT
                    ).show();
                }

                @Override
                public void onFailure(String message) {
                    addToCartButton.setEnabled(true);
                    addToCartButton.setText("Add to Cart");
                    Toast.makeText(
                            ProductDetailsActivity.this,
                            message,
                            Toast.LENGTH_LONG
                    ).show();
                }
            });
        });
    }

    private boolean isKgProduct() {
        return (selectedProduct.getUnitType() != null
                && selectedProduct.getUnitType().equalsIgnoreCase("kg"))
                || (selectedProduct.getCategoryName() != null
                && selectedProduct.getCategoryName().equalsIgnoreCase("Cakes"));
    }

    private double parseWeightToKg(String weightText) {
        if (weightText == null) return 1.0;

        String value = weightText.trim().toLowerCase(Locale.getDefault());

        try {
            if (value.endsWith("kg")) {
                return Double.parseDouble(value.replace("kg", "").trim());
            } else if (value.endsWith("g")) {
                double grams = Double.parseDouble(value.replace("g", "").trim());
                return grams / 1000.0;
            }
        } catch (Exception ignored) {
        }

        return 1.0;
    }

    private void updateWishlistIcon() {
        if (selectedProduct == null) return;

        boolean isWishlisted = WishlistManager.isWishlisted(selectedProduct.getId());

        int color = isWishlisted
                ? ContextCompat.getColor(this, R.color.md_theme_primary)
                : ContextCompat.getColor(this, android.R.color.white);

        wishlistButton.setColorFilter(color);
    }
}