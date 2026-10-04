package lk.jiat.butterqueen.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lk.jiat.butterqueen.activity.ProductDetailsActivity;
import lk.jiat.butterqueen.adapter.CategoryAdapter;
import lk.jiat.butterqueen.adapter.ProductAdapter;
import lk.jiat.butterqueen.databinding.FragmentHomeBinding;
import lk.jiat.butterqueen.model.Category;
import lk.jiat.butterqueen.model.Product;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    private CategoryAdapter categoryAdapter;
    private ProductAdapter productAdapter;

    private final List<Category> categoryList = new ArrayList<>();
    private final List<Product> allProducts = new ArrayList<>();
    private final List<Product> filteredProducts = new ArrayList<>();

    private FirebaseFirestore firestore;
    private String selectedCategory = "ALL";

    public HomeFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firestore = FirebaseFirestore.getInstance();

        setupCategoryRecycler();
        setupProductRecycler();
        setupSearch();
        setupActions();

        loadCategories();
        loadProducts();
    }

    private void setupCategoryRecycler() {
        binding.recyclerViewHomeCategories.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        );

        categoryAdapter = new CategoryAdapter(categoryList, category -> {
            selectedCategory = category.getName() != null ? category.getName() : "ALL";
            filterProducts();
        });

        binding.recyclerViewHomeCategories.setAdapter(categoryAdapter);
    }

    private void setupProductRecycler() {
        binding.recyclerViewCakes.setLayoutManager(new LinearLayoutManager(requireContext()));

        productAdapter = new ProductAdapter(
                filteredProducts,
                product -> {
                    Intent intent = new Intent(requireContext(), ProductDetailsActivity.class);
                    intent.putExtra("product_id", product.getId());
                    startActivity(intent);
                },
                null
        );

        binding.recyclerViewCakes.setAdapter(productAdapter);
    }

    private void setupSearch() {
        binding.searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterProducts();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupActions() {
        binding.orderNowButton.setOnClickListener(v ->
                binding.recyclerViewCakes.smoothScrollToPosition(0)
        );

        binding.seeAllTextView.setOnClickListener(v -> {
            selectedCategory = "ALL";
            binding.searchEditText.setText("");
            filterProducts();
        });
    }

    private void loadCategories() {
        firestore.collection("categories")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    categoryList.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Category category = doc.toObject(Category.class);
                        if (category != null) {
                            category.setId(doc.getId());
                            categoryList.add(category);
                        }
                    }

                    categoryAdapter.notifyDataSetChanged();

                    if (categoryList.isEmpty() && isAdded()) {
                        Toast.makeText(requireContext(), "No categories found", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Failed to load categories: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void loadProducts() {
        firestore.collection("products")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allProducts.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Product product = doc.toObject(Product.class);
                        if (product != null) {
                            product.setId(doc.getId());
                            allProducts.add(product);
                        }
                    }

                    filterProducts();

                    if (allProducts.isEmpty() && isAdded()) {
                        Toast.makeText(requireContext(), "No products found", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Failed to load products: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void filterProducts() {
        String keyword = "";

        if (binding.searchEditText.getText() != null) {
            keyword = binding.searchEditText.getText().toString().trim().toLowerCase(Locale.getDefault());
        }

        filteredProducts.clear();

        for (Product product : allProducts) {
            boolean matchesCategory = selectedCategory.equalsIgnoreCase("ALL")
                    || (product.getCategoryName() != null
                    && product.getCategoryName().equalsIgnoreCase(selectedCategory));

            boolean matchesKeyword =
                    (product.getName() != null
                            && product.getName().toLowerCase(Locale.getDefault()).contains(keyword))
                            || (product.getDescription() != null
                            && product.getDescription().toLowerCase(Locale.getDefault()).contains(keyword))
                            || (product.getCategoryName() != null
                            && product.getCategoryName().toLowerCase(Locale.getDefault()).contains(keyword));

            if (matchesCategory && matchesKeyword) {
                filteredProducts.add(product);
            }
        }

        productAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}