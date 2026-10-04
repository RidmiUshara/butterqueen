package lk.jiat.butterqueen.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lk.jiat.butterqueen.model.Product;

public class WishlistManager {

    private static final Map<String, Product> wishlistMap = new LinkedHashMap<>();

    public static boolean toggleWishlist(Product product) {
        if (product == null || product.getId() == null || product.getId().trim().isEmpty()) {
            return false;
        }

        String productId = product.getId();

        if (wishlistMap.containsKey(productId)) {
            wishlistMap.remove(productId);
            return false;
        } else {
            wishlistMap.put(productId, product);
            return true;
        }
    }

    public static boolean isWishlisted(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            return false;
        }
        return wishlistMap.containsKey(productId);
    }

    public static List<Product> getWishlistProducts() {
        return new ArrayList<>(wishlistMap.values());
    }

    public static int getWishlistCount() {
        return wishlistMap.size();
    }

    public static void clearWishlist() {
        wishlistMap.clear();
    }
}