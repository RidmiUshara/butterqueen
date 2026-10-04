package lk.jiat.butterqueen.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;
import java.util.Locale;

import lk.jiat.butterqueen.R;
import lk.jiat.butterqueen.data.WishlistManager;
import lk.jiat.butterqueen.model.Product;
import lk.jiat.butterqueen.model.UnitPricing;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    public interface OnWishlistChangedListener {
        void onWishlistChanged(Product product, boolean isWishlisted, int position);
    }

    private final List<Product> productList;
    private final OnProductClickListener productClickListener;
    private final OnWishlistChangedListener wishlistChangedListener;

    public ProductAdapter(List<Product> productList,
                          OnProductClickListener productClickListener,
                          OnWishlistChangedListener wishlistChangedListener) {
        this.productList = productList;
        this.productClickListener = productClickListener;
        this.wishlistChangedListener = wishlistChangedListener;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cake, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);

        holder.productNameTextView.setText(product.getName());
        String description = product.getDescription() == null ? "" : product.getDescription();
        if (product.getCakeType() != null && !product.getCakeType().isEmpty()) {
            description = Character.toUpperCase(product.getCakeType().charAt(0))
                    + product.getCakeType().substring(1) + " cake • " + description;
        }
        holder.productDescriptionTextView.setText(description);
        holder.productPriceTextView.setText(
                UnitPricing.priceLabel(product.getPrice(), UnitPricing.normalize(product.getUnitType(), product.getCategoryName()))
        );

        if (product.getImageUrl() != null && !product.getImageUrl().trim().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(product.getImageUrl())
                    .placeholder(R.drawable.cake_24px)
                    .error(R.drawable.cake_24px)
                    .into(holder.productImageView);
        } else if (product.getImageResId() != 0) {
            holder.productImageView.setImageResource(product.getImageResId());
        } else {
            holder.productImageView.setImageResource(R.drawable.cake_24px);
        }

        updateWishlistIcon(holder.wishlistButton, WishlistManager.isWishlisted(product.getId()));

        holder.itemView.setOnClickListener(v -> {
            if (productClickListener != null) {
                productClickListener.onProductClick(product);
            }
        });

        holder.wishlistButton.setOnClickListener(v -> {
            boolean isWishlistedNow = WishlistManager.toggleWishlist(product);
            updateWishlistIcon(holder.wishlistButton, isWishlistedNow);

            Toast.makeText(
                    holder.itemView.getContext(),
                    isWishlistedNow ? "Added to wishlist" : "Removed from wishlist",
                    Toast.LENGTH_SHORT
            ).show();

            if (wishlistChangedListener != null) {
                int currentPosition = holder.getBindingAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION) {
                    wishlistChangedListener.onWishlistChanged(product, isWishlistedNow, currentPosition);
                }
            }
        });
    }

    private void updateWishlistIcon(ImageButton button, boolean isWishlisted) {
        int color = isWishlisted
                ? ContextCompat.getColor(button.getContext(), R.color.md_theme_primary)
                : ContextCompat.getColor(button.getContext(), android.R.color.darker_gray);

        button.setColorFilter(color);
    }

    @Override
    public int getItemCount() {
        return productList != null ? productList.size() : 0;
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {

        ImageView productImageView;
        TextView productNameTextView;
        TextView productDescriptionTextView;
        TextView productPriceTextView;
        ImageButton wishlistButton;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productImageView = itemView.findViewById(R.id.cakeImageView);
            productNameTextView = itemView.findViewById(R.id.cakeNameTextView);
            productDescriptionTextView = itemView.findViewById(R.id.cakeDescriptionTextView);
            productPriceTextView = itemView.findViewById(R.id.cakePriceTextView);
            wishlistButton = itemView.findViewById(R.id.wishlistButton);
        }
    }
}