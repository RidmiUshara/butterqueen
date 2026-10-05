package lk.jiat.butterqueen.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import lk.jiat.butterqueen.R;
import lk.jiat.butterqueen.data.OrderReceipt;
import lk.jiat.butterqueen.data.OrderReceiptPdf;
import lk.jiat.butterqueen.model.OrderItem;
import lk.jiat.butterqueen.model.UnitPricing;

/** Loads an owned Firestore order and saves its current payment state as a PDF. */
public final class OrderReceiptActivity extends AppCompatActivity {
    private static final String EXTRA_ORDER_ID = "order_id";
    private final ExecutorService pdfWorker = Executors.newSingleThreadExecutor();
    private String ownerUid;
    private String orderId;
    private boolean loading;
    private boolean loaded;

    private final ActivityResultLauncher<String> createPdf = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/pdf"), uri -> {
                if (uri != null) exportLatest(uri);
            });

    public static Intent intentFor(Context context, String orderId) {
        return new Intent(context, OrderReceiptActivity.class).putExtra(EXTRA_ORDER_ID, orderId);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        orderId = getIntent().getStringExtra(EXTRA_ORDER_ID);
        if (user == null || orderId == null || orderId.trim().isEmpty()) {
            finish();
            return;
        }
        ownerUid = user.getUid();
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_order_receipt);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.receiptRoot), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        findViewById(R.id.receiptBack).setOnClickListener(v -> finish());
        findViewById(R.id.receiptDownload).setOnClickListener(v -> createPdf.launch(fileName()));
        load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ownerUid != null && !sameAccount()) finish();
    }

    @Override
    protected void onDestroy() {
        pdfWorker.shutdown();
        super.onDestroy();
    }

    private boolean sameAccount() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null && ownerUid.equals(user.getUid());
    }

    private String fileName() {
        String safe = orderId.replaceAll("[^A-Za-z0-9]", "");
        if (safe.length() > 12) safe = safe.substring(0, 12);
        return "ButterQueen-Order-" + safe + ".pdf";
    }

    private void load() {
        setLoading(true);
        fetch(receipt -> {
            if (!isFinishing() && !isDestroyed()) {
                bind(receipt);
                loaded = true;
                setLoading(false);
            }
        });
    }

    private void fetch(Consumer<OrderReceipt> onSuccess) {
        if (!sameAccount()) {
            finish();
            return;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("orders").document(orderId).get(Source.SERVER)
                .addOnSuccessListener(doc -> {
                    if (!sameAccount() || !doc.exists()
                            || !ownerUid.equals(doc.getString("userId"))) {
                        if (!isFinishing()) finish();
                        return;
                    }
                    OrderReceipt record = fromDocument(doc);
                    db.collection("orders").document(orderId).collection("items")
                            .get(Source.SERVER)
                            .addOnSuccessListener(items -> {
                                if (!sameAccount() || isFinishing() || isDestroyed()) return;
                                for (DocumentSnapshot itemDoc : items) {
                                    OrderItem item = itemDoc.toObject(OrderItem.class);
                                    if (item != null) record.items.add(item);
                                }
                                if (record.subtotal == 0 && record.total > 0)
                                    record.subtotal = Math.max(0, record.total - record.deliveryFee);
                                onSuccess.accept(record);
                            })
                            .addOnFailureListener(error -> fail());
                })
                .addOnFailureListener(error -> fail());
    }

    private OrderReceipt fromDocument(DocumentSnapshot doc) {
        OrderReceipt record = new OrderReceipt();
        record.id = doc.getId();
        Long millis = doc.getLong("createdAtMillis");
        Timestamp legacy = doc.getTimestamp("createdAt");
        record.createdAtMillis = millis == null
                ? (legacy == null ? 0L : legacy.toDate().getTime()) : millis;
        record.customerName = doc.getString("customerName");
        record.customerPhone = doc.getString("customerPhone");
        record.deliveryAddress = doc.getString("deliveryAddress");
        record.note = doc.getString("note");
        record.paymentMethod = doc.getString("paymentMethod");
        record.paymentStatus = doc.getString("paymentStatus");
        record.paymentReference = doc.getString("paymentReference");
        record.orderStatus = doc.getString("orderStatus");
        record.subtotal = number(doc, "subtotalAmount");
        record.deliveryFee = number(doc, "deliveryFee");
        record.total = number(doc, "totalAmount");
        if (record.total == 0) record.total = number(doc, "total");
        return record;
    }

    private double number(DocumentSnapshot doc, String field) {
        Object value = doc.get(field);
        return value instanceof Number ? ((Number) value).doubleValue() : 0;
    }

    private void bind(OrderReceipt record) {
        ((TextView) findViewById(R.id.receiptReference)).setText("Order " + record.id);
        ((TextView) findViewById(R.id.receiptDate)).setText(record.date());
        ((TextView) findViewById(R.id.receiptOrderStatus))
                .setText("Status: " + record.orderState());
        ((TextView) findViewById(R.id.receiptPayment))
                .setText(record.paymentLabel() + "  •  " + record.paymentState());
        ((TextView) findViewById(R.id.receiptNotice)).setText(record.paymentNotice());
        String customer = OrderReceipt.value(record.customerName) + "\n"
                + OrderReceipt.value(record.customerPhone) + "\n"
                + OrderReceipt.value(record.deliveryAddress);
        if (record.note != null && !record.note.trim().isEmpty())
            customer += "\nNote: " + record.note.trim();
        ((TextView) findViewById(R.id.receiptCustomer)).setText(customer);

        LinearLayout items = findViewById(R.id.receiptItems);
        items.removeAllViews();
        if (record.items.isEmpty()) addItemLine(items, "No line items recorded", "");
        for (OrderItem item : record.items) {
            addItemLine(items, OrderReceipt.value(item.getProductName()),
                    UnitPricing.quantityLabel(item.getQuantity(), item.getUnitType())
                            + " × " + OrderReceipt.money(item.getUnitPrice())
                            + " = " + OrderReceipt.money(item.getItemTotal()));
        }
        ((TextView) findViewById(R.id.receiptTotals)).setText(
                "Subtotal  " + OrderReceipt.money(record.subtotal) + "\n"
                        + "Delivery  " + OrderReceipt.money(record.deliveryFee) + "\n"
                        + "Total  " + OrderReceipt.money(record.total));
    }

    private void addItemLine(LinearLayout parent, String name, String detail) {
        TextView item = new TextView(this);
        item.setText(name + (detail.isEmpty() ? "" : "\n" + detail));
        item.setTextSize(14);
        item.setTextColor(ContextCompat.getColor(this, R.color.md_theme_onBackground));
        item.setPadding(0, dp(12), 0, dp(8));
        parent.addView(item);
    }

    private int dp(int value) {
        return (int) (getResources().getDisplayMetrics().density * value + 0.5f);
    }

    private void exportLatest(Uri uri) {
        if (loading || !sameAccount()) return;
        setLoading(true);
        fetch(record -> pdfWorker.execute(() -> {
            boolean saved = false;
            try (OutputStream out = getContentResolver().openOutputStream(uri, "w")) {
                if (out == null) throw new IOException("Cannot open PDF destination");
                OrderReceiptPdf.write(record, out);
                saved = true;
            } catch (IOException | RuntimeException error) {
                android.util.Log.w("OrderReceipt", "PDF export failed", error);
            }
            final boolean result = saved;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                setLoading(false);
                Toast.makeText(this, result ? "PDF saved" : "Could not save PDF",
                        Toast.LENGTH_LONG).show();
            });
        }));
    }

    private void setLoading(boolean value) {
        loading = value;
        findViewById(R.id.receiptDownload).setEnabled(!value && loaded);
    }

    private void fail() {
        if (isFinishing() || isDestroyed()) return;
        setLoading(false);
        Toast.makeText(this, "Could not load the latest order. Connect and retry.",
                Toast.LENGTH_LONG).show();
    }
}
