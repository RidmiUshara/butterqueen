package lk.jiat.butterqueen.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lk.jiat.butterqueen.R;
import lk.jiat.butterqueen.data.AdminAccess;
import lk.jiat.butterqueen.model.Category;
import lk.jiat.butterqueen.model.Product;
import lk.jiat.butterqueen.model.UnitPricing;

public class AdminProductActivity extends AppCompatActivity {
    private final List<Category> categories = new ArrayList<>();
    private FirebaseFirestore firestore;
    private Product editingProduct;
    private String productId;
    private Spinner categorySpinner, cakeTypeSpinner;
    private TextInputEditText nameInput, descriptionInput, priceInput, imageUrlInput;
    private TextView unitHint, cakeTypeLabel;
    private MaterialSwitch availableSwitch;
    private MaterialButton saveButton;
    private boolean authorized;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_admin_product);
        firestore = FirebaseFirestore.getInstance();
        productId = getIntent().getStringExtra("product_id");
        categorySpinner = findViewById(R.id.adminCategorySpinner);
        cakeTypeSpinner = findViewById(R.id.adminCakeTypeSpinner);
        nameInput = findViewById(R.id.adminProductName);
        descriptionInput = findViewById(R.id.adminProductDescription);
        priceInput = findViewById(R.id.adminProductPrice);
        imageUrlInput = findViewById(R.id.adminImageUrl);
        unitHint = findViewById(R.id.adminUnitHint);
        cakeTypeLabel = findViewById(R.id.adminCakeTypeLabel);
        availableSwitch = findViewById(R.id.adminAvailableSwitch);
        saveButton = findViewById(R.id.adminSaveProductButton);
        findViewById(R.id.adminProductBack).setOnClickListener(v -> finish());

        ArrayAdapter<String> cakeTypes = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Birthday cake", "Wedding cake", "Other cake"});
        cakeTypeSpinner.setAdapter(cakeTypes);
        saveButton.setEnabled(false);
        saveButton.setOnClickListener(v -> saveProduct());
        AdminAccess.verify(allowed -> {
            if (isFinishing() || isDestroyed()) return;
            if (!allowed) {
                Toast.makeText(this, "Admin access required", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            authorized = true;
            loadCategories();
        });
    }

    private void loadCategories() {
        firestore.collection("categories").get()
                .addOnSuccessListener(snapshot -> {
                    categories.clear();
                    List<String> labels = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot) {
                        Category category = doc.toObject(Category.class);
                        if (category != null) {
                            category.setId(doc.getId());
                            if (doc.getBoolean("active") == null) category.setActive(true);
                            categories.add(category);
                            labels.add(category.getName() + (category.isActive() ? "" : " (hidden)"));
                        }
                    }
                    if (categories.isEmpty()) {
                        Toast.makeText(this, "Create a category in Bakery Studio first",
                                Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                    categorySpinner.setAdapter(new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_dropdown_item, labels));
                    categorySpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                        @Override public void onItemSelected(android.widget.AdapterView<?> parent,
                                View view, int position, long id) {
                            updateUnitHint();
                        }
                        @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                    });
                    if (productId == null || productId.isEmpty()) {
                        saveButton.setEnabled(true);
                    } else {
                        loadProduct();
                    }
                })
                .addOnFailureListener(error -> {
                    Toast.makeText(this, "Could not load categories", Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void loadProduct() {
        firestore.collection("products").document(productId).get()
                .addOnSuccessListener(doc -> {
                    editingProduct = doc.toObject(Product.class);
                    if (!doc.exists() || editingProduct == null) {
                        Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    nameInput.setText(editingProduct.getName());
                    descriptionInput.setText(editingProduct.getDescription());
                    if (UnitPricing.isSupported(editingProduct.getUnitType())) {
                        priceInput.setText(String.format(Locale.US, "%.2f", editingProduct.getPrice()));
                    } else {
                        priceInput.setText("");
                        priceInput.setHint("Enter reviewed price per new unit");
                        Toast.makeText(this, "Review this old box/pack price before saving",
                                Toast.LENGTH_LONG).show();
                    }
                    imageUrlInput.setText(editingProduct.getImageUrl());
                    availableSwitch.setChecked(!Boolean.FALSE.equals(doc.getBoolean("available")));
                    for (int i = 0; i < categories.size(); i++) {
                        if (categories.get(i).getId().equals(editingProduct.getCategoryId())
                                || categories.get(i).getName().equalsIgnoreCase(
                                        editingProduct.getCategoryName())) {
                            categorySpinner.setSelection(i);
                            break;
                        }
                    }
                    if ("wedding".equalsIgnoreCase(editingProduct.getCakeType())) {
                        cakeTypeSpinner.setSelection(1);
                    } else if ("other".equalsIgnoreCase(editingProduct.getCakeType())) {
                        cakeTypeSpinner.setSelection(2);
                    }
                    updateUnitHint();
                    saveButton.setEnabled(true);
                })
                .addOnFailureListener(error -> {
                    Toast.makeText(this, "Could not load product", Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private String unitFor(Category category) {
        String name = category.getName() == null ? "" : category.getName().trim();
        if ("Cakes".equalsIgnoreCase(name)) return UnitPricing.KG;
        if ("Cupcakes".equalsIgnoreCase(name) || "Brownies".equalsIgnoreCase(name)) {
            return UnitPricing.PCS;
        }
        return UnitPricing.normalize(category.getUnitType(), name);
    }

    private void updateUnitHint() {
        int position = categorySpinner.getSelectedItemPosition();
        if (position < 0 || position >= categories.size()) return;
        boolean kg = UnitPricing.isKg(unitFor(categories.get(position)));
        unitHint.setText(kg ? "Price for 1 kg • customer chooses 0.5 kg steps"
                : "Price for 1 piece • customer chooses whole pieces");
        cakeTypeLabel.setVisibility(kg ? View.VISIBLE : View.GONE);
        cakeTypeSpinner.setVisibility(kg ? View.VISIBLE : View.GONE);
    }

    private String value(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private void saveProduct() {
        if (!authorized || categories.isEmpty()) return;
        String name = value(nameInput);
        String description = value(descriptionInput);
        String imageUrl = value(imageUrlInput);
        if (name.isEmpty()) {
            nameInput.setError("Enter a product name");
            return;
        }
        if (description.isEmpty()) {
            descriptionInput.setError("Enter a description");
            return;
        }
        double price;
        try {
            price = Double.parseDouble(value(priceInput));
        } catch (NumberFormatException error) {
            priceInput.setError("Enter a valid price");
            return;
        }
        if (!Double.isFinite(price) || price <= 0 || price > 10000000
                || !value(priceInput).matches("[0-9]+(\\.[0-9]{1,2})?")) {
            priceInput.setError("Enter a price above zero with at most 2 decimals");
            return;
        }
        if (!imageUrl.isEmpty() && !imageUrl.startsWith("https://")) {
            imageUrlInput.setError("Use an HTTPS image URL");
            return;
        }
        int selected = categorySpinner.getSelectedItemPosition();
        if (selected < 0 || selected >= categories.size()) return;
        Category category = categories.get(selected);
        String unit = unitFor(category);
        String cakeType = "";
        if (UnitPricing.isKg(unit)) {
            cakeType = new String[]{"birthday", "wedding", "other"}[
                    cakeTypeSpinner.getSelectedItemPosition()];
        }
        DocumentReference ref = productId == null || productId.isEmpty()
                ? firestore.collection("products").document()
                : firestore.collection("products").document(productId);
        Map<String, Object> fields = new HashMap<>();
        fields.put("id", ref.getId());
        fields.put("name", name);
        fields.put("description", description);
        fields.put("categoryId", category.getId());
        fields.put("categoryName", category.getName());
        fields.put("price", UnitPricing.roundMoney(price));
        fields.put("unitType", unit);
        fields.put("cakeType", cakeType);
        fields.put("imageUrl", imageUrl);
        fields.put("available", availableSwitch.isChecked());
        fields.put("customizable", UnitPricing.isKg(unit));
        saveButton.setEnabled(false);
        if (editingProduct == null) {
            fields.put("createdAtMillis", System.currentTimeMillis());
            fields.put("rating", 0.0);
            fields.put("stock", 0);
            ref.set(fields).addOnSuccessListener(unused -> saved())
                    .addOnFailureListener(error -> failed(error.getMessage()));
        } else {
            ref.update(fields).addOnSuccessListener(unused -> saved())
                    .addOnFailureListener(error -> failed(error.getMessage()));
        }
    }

    private void saved() {
        Toast.makeText(this, "Product saved", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void failed(String message) {
        saveButton.setEnabled(true);
        Toast.makeText(this, "Save failed: " + message, Toast.LENGTH_LONG).show();
    }
}
