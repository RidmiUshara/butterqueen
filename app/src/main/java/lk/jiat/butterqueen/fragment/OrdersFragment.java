package lk.jiat.butterqueen.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lk.jiat.butterqueen.adapter.OrderAdapter;
import lk.jiat.butterqueen.databinding.FragmentOrdersBinding;
import lk.jiat.butterqueen.model.Order;
import lk.jiat.butterqueen.model.OrderItem;
import lk.jiat.butterqueen.model.UnitPricing;

public class OrdersFragment extends Fragment {

    private FragmentOrdersBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private OrderAdapter adapter;
    private final List<Order> orderList = new ArrayList<>();
    private ListenerRegistration listenerRegistration;

    public OrdersFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentOrdersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        binding.ordersRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new OrderAdapter(orderList, this::showOrderDetails);
        binding.ordersRecyclerView.setAdapter(adapter);

        loadOrders();
    }

    private void loadOrders() {
        if (firebaseAuth.getCurrentUser() == null) {
            binding.emptyOrdersTextView.setVisibility(View.VISIBLE);
            binding.emptyOrdersTextView.setText("Please sign in to view your orders");
            binding.ordersRecyclerView.setVisibility(View.GONE);
            return;
        }

        String userId = firebaseAuth.getCurrentUser().getUid();

        listenerRegistration = firestore.collection("orders")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((value, error) -> {
                    if (!isAdded() || binding == null) {
                        return;
                    }

                    if (error != null) {
                        Toast.makeText(requireContext(), "Failed to load orders: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    orderList.clear();

                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Order order = parseOrder(doc);
                            orderList.add(order);
                        }
                    }

                    orderList.sort((left, right) -> Long.compare(
                            right.getCreatedAtMillis(), left.getCreatedAtMillis()));
                    adapter.notifyDataSetChanged();

                    boolean isEmpty = orderList.isEmpty();
                    binding.emptyOrdersTextView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                    binding.ordersRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

                    if (isEmpty) {
                        binding.emptyOrdersTextView.setText("No orders yet");
                    }
                });
    }

    private void showOrderDetails(Order order) {
        firestore.collection("orders").document(order.getOrderId())
                .collection("items").get()
                .addOnSuccessListener(snapshot -> {
                    if (!isAdded()) return;
                    StringBuilder details = new StringBuilder();
                    details.append("Status: ").append(order.getOrderStatus())
                            .append("\nPayment: ").append(order.getPaymentMethod())
                            .append(" / ").append(order.getPaymentStatus() == null
                                    ? "pending" : order.getPaymentStatus())
                            .append("\n\n");
                    for (DocumentSnapshot doc : snapshot) {
                        OrderItem item = doc.toObject(OrderItem.class);
                        if (item == null) continue;
                        details.append(item.getProductName()).append(" • ")
                                .append(UnitPricing.quantityLabel(item.getQuantity(),
                                        item.getUnitType()))
                                .append(String.format(Locale.getDefault(),
                                        " × Rs. %.2f = Rs. %.2f\n",
                                        item.getUnitPrice(), item.getItemTotal()));
                    }
                    if (snapshot.isEmpty()) details.append("No item details recorded\n");
                    details.append(String.format(Locale.getDefault(),
                            "\nTotal: Rs. %.2f", order.getTotalAmount()));
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Order " + order.getOrderId())
                            .setMessage(details.toString())
                            .setPositiveButton("Close", null)
                            .show();
                })
                .addOnFailureListener(error -> {
                    if (isAdded()) Toast.makeText(requireContext(),
                            "Could not load order details", Toast.LENGTH_LONG).show();
                });
    }

    private Order parseOrder(DocumentSnapshot doc) {
        Order order = doc.toObject(Order.class);

        if (order == null) {
            order = new Order();
        }

        order.setOrderId(doc.getId());

        if (order.getCreatedAtMillis() == 0) {
            Timestamp timestamp = doc.getTimestamp("createdAt");
            if (timestamp != null) {
                order.setCreatedAtMillis(timestamp.toDate().getTime());
            } else {
                order.setCreatedAtMillis(System.currentTimeMillis());
            }
        }

        if (order.getTotalAmount() == 0) {
            Double oldTotal = doc.getDouble("total");
            if (oldTotal != null) {
                order.setTotalAmount(oldTotal);
            }
        }

        if (order.getTotalItems() == 0) {
            Long oldQty = doc.getLong("quantity");
            if (oldQty != null) {
                order.setTotalItems(oldQty.intValue());
            }
        }

        if (order.getOrderStatus() == null || order.getOrderStatus().trim().isEmpty()) {
            order.setOrderStatus("placed");
        }

        if (order.getPaymentMethod() == null || order.getPaymentMethod().trim().isEmpty()) {
            order.setPaymentMethod("cash_on_delivery");
        }

        return order;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }

        binding = null;
    }
}