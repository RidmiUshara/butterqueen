package lk.jiat.butterqueen.data;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lk.jiat.butterqueen.model.CartItem;
import lk.jiat.butterqueen.model.UnitPricing;

public class CartManager {

    public interface SimpleCallback {
        void onSuccess();
        void onFailure(String message);
    }

    public interface CartLoadListener {
        void onCartUpdated(List<CartItem> cartItems);
        void onError(String message);
    }

    private static final List<CartItem> cartItems = new ArrayList<>();
    private static final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private static final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    private static ListenerRegistration cartListener;

    public static List<CartItem> getCartItems() {
        return cartItems;
    }

    public static void loadCartOnce(CartLoadListener listener) {
        String userId = getCurrentUserId();

        if (userId == null) {
            cartItems.clear();
            if (listener != null) {
                listener.onError("User not signed in");
                listener.onCartUpdated(new ArrayList<>(cartItems));
            }
            return;
        }

        firestore.collection("users")
                .document(userId)
                .collection("cart")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    updateLocalCart(queryDocumentSnapshots.getDocuments());

                    if (listener != null) {
                        listener.onCartUpdated(new ArrayList<>(cartItems));
                    }
                })
                .addOnFailureListener(e -> {
                    if (listener != null) {
                        listener.onError(e.getMessage() != null ? e.getMessage() : "Failed to load cart");
                    }
                });
    }

    public static void startCartListener(CartLoadListener listener) {
        stopCartListener();

        String userId = getCurrentUserId();

        if (userId == null) {
            cartItems.clear();
            if (listener != null) {
                listener.onError("User not signed in");
                listener.onCartUpdated(new ArrayList<>(cartItems));
            }
            return;
        }

        cartListener = firestore.collection("users")
                .document(userId)
                .collection("cart")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        if (listener != null) {
                            listener.onError(error.getMessage() != null
                                    ? error.getMessage()
                                    : "Failed to listen to cart");
                        }
                        return;
                    }

                    if (value != null) {
                        updateLocalCart(value.getDocuments());
                    } else {
                        cartItems.clear();
                    }

                    if (listener != null) {
                        listener.onCartUpdated(new ArrayList<>(cartItems));
                    }
                });
    }

    public static void stopCartListener() {
        if (cartListener != null) {
            cartListener.remove();
            cartListener = null;
        }
    }

    public static void addToCart(CartItem newItem, SimpleCallback callback) {
        String userId = getCurrentUserId();

        if (userId == null) {
            if (callback != null) callback.onFailure("User not signed in");
            return;
        }

        if (newItem == null || isBlank(newItem.getProductId())) {
            if (callback != null) callback.onFailure("Invalid cart item");
            return;
        }

        normalizeCartItem(newItem);
        newItem.setQuantity(UnitPricing.normalizeQuantity(newItem.getQuantity(), newItem.getUnitType()));

        firestore.collection("users")
                .document(userId)
                .collection("cart")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    DocumentSnapshot matchedDoc = null;
                    CartItem matchedItem = null;

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        CartItem existingItem = doc.toObject(CartItem.class);
                        if (existingItem == null) continue;

                        existingItem.setCartItemId(doc.getId());

                        if (sameCartItem(existingItem, newItem)) {
                            matchedDoc = doc;
                            matchedItem = existingItem;
                            break;
                        }
                    }

                    if (matchedDoc != null && matchedItem != null) {
                        double updatedQuantity = matchedItem.getQuantity() + newItem.getQuantity();

                        firestore.collection("users")
                                .document(userId)
                                .collection("cart")
                                .document(matchedDoc.getId())
                                .update(
                                        "quantity", updatedQuantity,
                                        "addedAtMillis", System.currentTimeMillis()
                                )
                                .addOnSuccessListener(unused -> {
                                    if (callback != null) callback.onSuccess();
                                })
                                .addOnFailureListener(e -> {
                                    if (callback != null) {
                                        callback.onFailure(e.getMessage() != null
                                                ? e.getMessage()
                                                : "Failed to update cart item");
                                    }
                                });
                    } else {
                        String cartItemId = firestore.collection("users")
                                .document(userId)
                                .collection("cart")
                                .document()
                                .getId();

                        newItem.setCartItemId(cartItemId);
                        if (newItem.getAddedAtMillis() <= 0) {
                            newItem.setAddedAtMillis(System.currentTimeMillis());
                        }

                        firestore.collection("users")
                                .document(userId)
                                .collection("cart")
                                .document(cartItemId)
                                .set(newItem)
                                .addOnSuccessListener(unused -> {
                                    if (callback != null) callback.onSuccess();
                                })
                                .addOnFailureListener(e -> {
                                    if (callback != null) {
                                        callback.onFailure(e.getMessage() != null
                                                ? e.getMessage()
                                                : "Failed to add item to cart");
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onFailure(e.getMessage() != null
                                ? e.getMessage()
                                : "Failed to access cart");
                    }
                });
    }

    public static void increaseQuantity(int position, SimpleCallback callback) {
        if (position < 0 || position >= cartItems.size()) {
            if (callback != null) callback.onFailure("Invalid cart item");
            return;
        }

        CartItem item = cartItems.get(position);

        if (isBlank(item.getCartItemId())) {
            if (callback != null) callback.onFailure("Cart item ID missing");
            return;
        }

        String userId = getCurrentUserId();
        if (userId == null) {
            if (callback != null) callback.onFailure("User not signed in");
            return;
        }

        double newQuantity = item.getQuantity() + UnitPricing.step(item.getUnitType());

        firestore.collection("users")
                .document(userId)
                .collection("cart")
                .document(item.getCartItemId())
                .update("quantity", newQuantity)
                .addOnSuccessListener(unused -> {
                    item.setQuantity(newQuantity);
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onFailure(e.getMessage() != null
                                ? e.getMessage()
                                : "Failed to increase quantity");
                    }
                });
    }

    public static void decreaseQuantity(int position, SimpleCallback callback) {
        if (position < 0 || position >= cartItems.size()) {
            if (callback != null) callback.onFailure("Invalid cart item");
            return;
        }

        CartItem item = cartItems.get(position);

        if (isBlank(item.getCartItemId())) {
            if (callback != null) callback.onFailure("Cart item ID missing");
            return;
        }

        String userId = getCurrentUserId();
        if (userId == null) {
            if (callback != null) callback.onFailure("User not signed in");
            return;
        }

        double minQuantity = UnitPricing.step(item.getUnitType());
        if (item.getQuantity() <= minQuantity) {
            if (callback != null) callback.onSuccess();
            return;
        }

        double newQuantity = item.getQuantity() - UnitPricing.step(item.getUnitType());

        firestore.collection("users")
                .document(userId)
                .collection("cart")
                .document(item.getCartItemId())
                .update("quantity", newQuantity)
                .addOnSuccessListener(unused -> {
                    item.setQuantity(newQuantity);
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onFailure(e.getMessage() != null
                                ? e.getMessage()
                                : "Failed to decrease quantity");
                    }
                });
    }

    public static void removeItem(int position, SimpleCallback callback) {
        if (position < 0 || position >= cartItems.size()) {
            if (callback != null) callback.onFailure("Invalid cart item");
            return;
        }

        CartItem item = cartItems.get(position);

        if (isBlank(item.getCartItemId())) {
            if (callback != null) callback.onFailure("Cart item ID missing");
            return;
        }

        String userId = getCurrentUserId();
        if (userId == null) {
            if (callback != null) callback.onFailure("User not signed in");
            return;
        }

        firestore.collection("users")
                .document(userId)
                .collection("cart")
                .document(item.getCartItemId())
                .delete()
                .addOnSuccessListener(unused -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onFailure(e.getMessage() != null
                                ? e.getMessage()
                                : "Failed to remove item");
                    }
                });
    }

    public static double getTotalPrice() {
        double total = 0;
        for (CartItem item : cartItems) {
            total += item.getItemTotal();
        }
        return UnitPricing.roundMoney(total);
    }

    public static int getTotalItems() {
        return cartItems.size();
    }

    public static boolean isEmpty() {
        return cartItems.isEmpty();
    }

    public static void clearLocalCache() {
        cartItems.clear();
    }

    private static void updateLocalCart(List<DocumentSnapshot> documents) {
        cartItems.clear();

        for (DocumentSnapshot doc : documents) {
            CartItem item = doc.toObject(CartItem.class);
            if (item != null) {
                item.setCartItemId(doc.getId());
                normalizeCartItem(item);
                cartItems.add(item);
            }
        }

        Collections.sort(cartItems, (a, b) ->
                Long.compare(b.getAddedAtMillis(), a.getAddedAtMillis()));
    }

    private static boolean sameCartItem(CartItem a, CartItem b) {
        return equalsNormalized(a.getProductId(), b.getProductId())
                && equalsNormalized(a.getSelectedSize(), b.getSelectedSize())
                && equalsNormalized(a.getSelectedFlavor(), b.getSelectedFlavor())
                && equalsNormalized(a.getCakeMessage(), b.getCakeMessage());
    }

    private static void normalizeCartItem(CartItem item) {
        if (item.getSelectedSize() == null) item.setSelectedSize("");
        if (item.getSelectedFlavor() == null) item.setSelectedFlavor("");
        if (item.getCakeMessage() == null) item.setCakeMessage("");
        if (item.getProductImageUrl() == null) item.setProductImageUrl("");
        if (item.getUnitType() == null) item.setUnitType("");
    }

    private static boolean equalsNormalized(String a, String b) {
        String valueA = a == null ? "" : a.trim();
        String valueB = b == null ? "" : b.trim();
        return valueA.equals(valueB);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String getCurrentUserId() {
        return firebaseAuth.getCurrentUser() != null
                ? firebaseAuth.getCurrentUser().getUid()
                : null;
    }
}