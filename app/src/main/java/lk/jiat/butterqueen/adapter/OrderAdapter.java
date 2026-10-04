package lk.jiat.butterqueen.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import lk.jiat.butterqueen.R;
import lk.jiat.butterqueen.databinding.ItemOrderBinding;
import lk.jiat.butterqueen.model.Order;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    public interface OnOrderClickListener {
        void onOrderClick(Order order);
    }

    private final List<Order> orderList;
    private final OnOrderClickListener listener;

    public OrderAdapter(List<Order> orderList, OnOrderClickListener listener) {
        this.orderList = orderList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOrderBinding binding = ItemOrderBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new OrderViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orderList.get(position);

        String shortOrderId = order.getOrderId() != null && order.getOrderId().length() >= 6
                ? order.getOrderId().substring(0, 6).toUpperCase(Locale.getDefault())
                : "ORDER";

        holder.binding.orderNameTextView.setText("Order #" + shortOrderId);
        holder.binding.orderQtyTextView.setText("Products: " + order.getTotalItems());
        holder.binding.orderPriceTextView.setText(
                String.format(Locale.getDefault(), "Rs. %.2f", order.getTotalAmount())
        );

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
        holder.binding.orderDateTextView.setText(
                "Date: " + sdf.format(order.getCreatedAtMillis())
        );

        holder.binding.orderPaymentTextView.setText(
                "Payment: " + formatPaymentMethod(order.getPaymentMethod())
        );

        holder.binding.orderStatusTextView.setText(
                "Status: " + formatStatus(order.getOrderStatus())
        );
        holder.binding.orderStatusTextView.setTextColor(getStatusColor(order.getOrderStatus()));

        holder.binding.orderImageView.setImageResource(R.drawable.cake_24px);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onOrderClick(order);
        });
    }

    @Override
    public int getItemCount() {
        return orderList != null ? orderList.size() : 0;
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        ItemOrderBinding binding;

        public OrderViewHolder(@NonNull ItemOrderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private String formatPaymentMethod(String paymentMethod) {
        if (paymentMethod == null) return "Unknown";

        if (paymentMethod.equalsIgnoreCase("cash_on_delivery")) {
            return "Cash on Delivery";
        } else if (paymentMethod.equalsIgnoreCase("payhere")) {
            return "PayHere sandbox";
        } else if (paymentMethod.equalsIgnoreCase("stripe_card")) {
            return "Card Payment";
        }

        return paymentMethod;
    }

    private String formatStatus(String status) {
        if (status == null) return "Pending";

        if (status.equalsIgnoreCase("placed")) {
            return "Placed";
        } else if (status.equalsIgnoreCase("confirmed")) {
            return "Confirmed";
        } else if (status.equalsIgnoreCase("payment_pending")) {
            return "Payment Verification Pending";
        } else if (status.equalsIgnoreCase("draft")) {
            return "Pending Payment";
        } else if (status.equalsIgnoreCase("awaiting_webhook_confirmation")) {
            return "Payment Confirming";
        } else if (status.equalsIgnoreCase("payment_failed")) {
            return "Payment Failed";
        } else if (status.equalsIgnoreCase("payment_cancelled")) {
            return "Payment Cancelled";
        } else if (status.equalsIgnoreCase("preparing")) {
            return "Preparing";
        } else if (status.equalsIgnoreCase("out_for_delivery")) {
            return "Out for Delivery";
        } else if (status.equalsIgnoreCase("delivered")) {
            return "Delivered";
        } else if (status.equalsIgnoreCase("cancelled")) {
            return "Cancelled";
        }

        return status;
    }

    private int getStatusColor(String status) {
        if (status == null) {
            return Color.parseColor("#FFA500");
        }

        if (status.equalsIgnoreCase("placed")
                || status.equalsIgnoreCase("confirmed")
                || status.equalsIgnoreCase("preparing")
                || status.equalsIgnoreCase("awaiting_webhook_confirmation")
                || status.equalsIgnoreCase("payment_pending")) {
            return Color.parseColor("#FFA500");
        } else if (status.equalsIgnoreCase("delivered")) {
            return Color.parseColor("#2E7D32");
        } else if (status.equalsIgnoreCase("payment_failed")
                || status.equalsIgnoreCase("payment_cancelled")
                || status.equalsIgnoreCase("cancelled")) {
            return Color.parseColor("#C62828");
        } else if (status.equalsIgnoreCase("out_for_delivery")) {
            return Color.parseColor("#1565C0");
        }

        return Color.parseColor("#FFA500");
    }
}