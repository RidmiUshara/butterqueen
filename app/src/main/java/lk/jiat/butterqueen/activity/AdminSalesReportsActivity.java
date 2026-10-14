package lk.jiat.butterqueen.activity;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import lk.jiat.butterqueen.R;
import lk.jiat.butterqueen.data.AdminAccess;

/**
 * Demonstration-friendly analytics based on the existing Firestore order schema.
 * Revenue is deliberately called "delivered order value": COD collection is not verified.
 * Full-collection aggregation is fine for small demo datasets, not production-scale datasets.
 */
public class AdminSalesReportsActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private LinearLayout content;
    private TextView status;
    private int loadVersion;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_admin_sales_reports);
        applyWindowInsets();
        db = FirebaseFirestore.getInstance();
        content = findViewById(R.id.salesReportsContent);
        status = findViewById(R.id.salesReportsStatus);
        ((com.google.android.material.appbar.MaterialToolbar)
                findViewById(R.id.salesReportsToolbar))
                .setNavigationOnClickListener(v -> finish());
        findViewById(R.id.salesReportsRefresh).setOnClickListener(v -> verifyAndLoad());
        verifyAndLoad();
    }

    // Keep the toolbar below the status bar on Android 15+ edge-to-edge displays.
    private void applyWindowInsets() {
        View root = findViewById(R.id.salesReportsRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    private void verifyAndLoad() {
        int version = ++loadVersion;
        content.removeAllViews();
        status.setText("Checking administrator permission...");
        AdminAccess.verify(allowed -> {
            if (!active(version)) return;
            if (!allowed) {
                Toast.makeText(this, "Admin access required", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            loadReports(version);
        });
    }

    private void loadReports(int version) {
        status.setText("Loading orders from Firestore...");
        db.collection("orders").get(Source.SERVER)
                .addOnSuccessListener(snapshot -> {
                    if (!active(version)) return;
                    showSummary(snapshot, version);
                })
                .addOnFailureListener(error -> {
                    if (!active(version)) return;
                    status.setText("Could not load reports: " + error.getMessage());
                });
    }

    private void showSummary(QuerySnapshot snapshot, int version) {
        content.removeAllViews();
        int total = snapshot.size();
        int delivered = 0;
        int cancelled = 0;
        double deliveredValue = 0;
        List<DocumentSnapshot> deliveredOrders = new ArrayList<>();

        LinkedHashMap<String, Integer> statusCounts = new LinkedHashMap<>();
        for (String value : new String[]{"placed", "preparing", "out_for_delivery",
                "delivered", "cancelled", "payment_pending", "other"}) {
            statusCounts.put(value, 0);
        }

        LinkedHashMap<String, Double> monthTotals = new LinkedHashMap<>();
        LinkedHashMap<String, String> monthNames = new LinkedHashMap<>();
        Calendar monthStart = Calendar.getInstance();
        monthStart.set(Calendar.DAY_OF_MONTH, 1);
        monthStart.add(Calendar.MONTH, -5);
        for (int i = 0; i < 6; i++) {
            String key = monthKey(monthStart);
            monthTotals.put(key, 0.0);
            monthNames.put(key, new SimpleDateFormat("MMM yy", Locale.getDefault())
                    .format(monthStart.getTime()));
            monthStart.add(Calendar.MONTH, 1);
        }

        for (DocumentSnapshot order : snapshot) {
            String orderStatus = order.getString("orderStatus");
            if (orderStatus == null || !statusCounts.containsKey(orderStatus)) {
                orderStatus = "other";
            }
            statusCounts.put(orderStatus, statusCounts.get(orderStatus) + 1);
            if ("cancelled".equals(orderStatus)) cancelled++;
            if (!"delivered".equals(orderStatus)) continue;

            delivered++;
            // Paid PayHere or delivered COD only. A delivered COD order is
            // NOT independent proof that cash was collected.
            String method = order.getString("paymentMethod");
            String paymentStatus = order.getString("paymentStatus");
            boolean eligible = "cash_on_delivery".equals(method)
                    || ("payhere".equals(method) && "paid".equals(paymentStatus));
            if (!eligible) continue;

            double value = Math.max(0, number(order, "totalAmount"));
            deliveredValue += value;
            deliveredOrders.add(order);
            Number millis = (Number) order.get("createdAtMillis");
            if (millis != null && millis.longValue() > 0) {
                Calendar date = Calendar.getInstance();
                date.setTimeInMillis(millis.longValue());
                String key = monthKey(date);
                if (monthTotals.containsKey(key)) {
                    monthTotals.put(key, monthTotals.get(key) + value);
                }
            }
        }

        status.setText("Based on " + total + " Firestore orders • refreshed now");
        sectionTitle("Overview");
        addMetricPair("TOTAL ORDERS", String.valueOf(total),
                "DELIVERED", String.valueOf(delivered));
        addMetricPair("CANCELLED", String.valueOf(cancelled),
                "DELIVERED ORDER VALUE", money(deliveredValue));

        MaterialCardView statusCard = createCard();
        LinearLayout statusBox = cardBody(statusCard);
        addHeading(statusBox, "Orders by status");
        addSubheading(statusBox, "All order records, including pending and cancelled");
        int maxCount = 1;
        for (Integer count : statusCounts.values()) maxCount = Math.max(maxCount, count);
        for (Map.Entry<String, Integer> entry : statusCounts.entrySet()) {
            // Keep zero-count categories visible: useful during a viva demonstration.
            addBar(statusBox, readableStatus(entry.getKey()),
                    entry.getValue(), maxCount, String.valueOf(entry.getValue()),
                    R.color.md_theme_primary);
        }

        MaterialCardView monthCard = createCard();
        LinearLayout monthBox = cardBody(monthCard);
        addHeading(monthBox, "Monthly delivered-order value");
        addSubheading(monthBox, "Grouped by order creation month • most recent 6 months");
        double maxMonth = 1.0;
        for (Double value : monthTotals.values()) maxMonth = Math.max(maxMonth, value);
        for (Map.Entry<String, Double> entry : monthTotals.entrySet()) {
            addBar(monthBox, monthNames.get(entry.getKey()), entry.getValue(),
                    maxMonth, money(entry.getValue()), R.color.md_theme_tertiary);
        }

        sectionTitle("Best-selling products");
        MaterialCardView productCard = createCard();
        LinearLayout productBox = cardBody(productCard);
        addSubheading(productBox, "Ranked by item value in eligible delivered orders");
        TextView productStatus = label("Loading order items...", 14,
                R.color.md_theme_onSurfaceVariant, false);
        productBox.addView(productStatus);
        loadProducts(deliveredOrders, productBox, productStatus, version);

        TextView note = label("Note: Delivered order value is not verified cash revenue. "
                + "Cash-on-delivery payments may still need manual reconciliation. "
                + "Custom cake requests are excluded until converted into actual orders.",
                12, R.color.md_theme_onSurfaceVariant, false);
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        noteParams.topMargin = dp(14);
        content.addView(note, noteParams);
    }

    private void loadProducts(List<DocumentSnapshot> orders, LinearLayout box,
                              TextView hint, int version) {
        if (orders.isEmpty()) {
            hint.setText("No eligible delivered orders yet.");
            return;
        }
        Map<String, Double> sales = new HashMap<>();
        AtomicInteger outstanding = new AtomicInteger(orders.size());
        AtomicInteger failures = new AtomicInteger(0);
        for (DocumentSnapshot order : orders) {
            db.collection("orders").document(order.getId()).collection("items")
                    .get(Source.SERVER)
                    .addOnSuccessListener(items -> {
                        if (!active(version)) return;
                        for (DocumentSnapshot item : items) {
                            String name = item.getString("productName");
                            if (name == null || name.trim().isEmpty()) name = "Unnamed product";
                            double amount = Math.max(0, number(item, "itemTotal"));
                            sales.put(name, sales.getOrDefault(name, 0.0) + amount);
                        }
                        finishProducts(box, hint, sales, outstanding, failures, version);
                    })
                    .addOnFailureListener(error -> {
                        if (!active(version)) return;
                        failures.incrementAndGet();
                        finishProducts(box, hint, sales, outstanding, failures, version);
                    });
        }
    }

    private void finishProducts(LinearLayout box, TextView hint,
                                Map<String, Double> sales, AtomicInteger outstanding,
                                AtomicInteger failures, int version) {
        if (outstanding.decrementAndGet() != 0 || !active(version)) return;
        if (sales.isEmpty()) {
            hint.setText(failures.get() > 0 ? "Could not load product sales details."
                    : "No item details found in delivered orders.");
            return;
        }
        hint.setText(failures.get() == 0
                ? "Top products by total item value"
                : "Partial results: some order items could not be loaded");
        List<Map.Entry<String, Double>> ranked = new ArrayList<>(sales.entrySet());
        Collections.sort(ranked, (a, b) -> Double.compare(b.getValue(), a.getValue()));
        double max = Math.max(1, ranked.get(0).getValue());
        for (int i = 0; i < Math.min(5, ranked.size()); i++) {
            Map.Entry<String, Double> item = ranked.get(i);
            addBar(box, (i + 1) + ". " + item.getKey(),
                    item.getValue(), max, money(item.getValue()), R.color.md_theme_secondary);
        }
    }

    private void sectionTitle(String title) {
        TextView text = label(title, 19, R.color.md_theme_onBackground, true);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(16);
        p.bottomMargin = dp(9);
        content.addView(text, p);
    }

    private void addMetricPair(String title1, String value1, String title2, String value2) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setWeightSum(2);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.bottomMargin = dp(10);
        content.addView(row, rowParams);
        addMetric(row, title1, value1, true);
        addMetric(row, title2, value2, false);
    }

    private void addMetric(LinearLayout row, String heading, String value, boolean first) {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, 1);
        if (first) params.rightMargin = dp(6);
        else params.leftMargin = dp(6);
        row.addView(card, params);
        styleCard(card);
        LinearLayout body = cardBodyDirect(card);
        body.addView(label(heading, 11, R.color.md_theme_onSurfaceVariant, true));
        TextView number = label(value, value.length() > 15 ? 18 : 22,
                R.color.md_theme_primary, true);
        LinearLayout.LayoutParams n = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        n.topMargin = dp(7);
        body.addView(number, n);
    }

    private MaterialCardView createCard() {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.bottomMargin = dp(12);
        content.addView(card, p);
        styleCard(card);
        return card;
    }

    private void styleCard(MaterialCardView card) {
        card.setRadius(dp(16));
        card.setCardElevation(0);
        card.setCardBackgroundColor(color(R.color.md_theme_surfaceContainerLow));
        card.setStrokeColor(color(R.color.md_theme_outlineVariant));
        card.setStrokeWidth(dp(1));
    }

    private LinearLayout cardBody(MaterialCardView card) {
        return cardBodyDirect(card);
    }

    private LinearLayout cardBodyDirect(MaterialCardView card) {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.addView(body);
        return body;
    }

    private void addHeading(LinearLayout parent, String text) {
        parent.addView(label(text, 17, R.color.md_theme_onBackground, true));
    }

    private void addSubheading(LinearLayout parent, String text) {
        TextView caption = label(text, 12, R.color.md_theme_onSurfaceVariant, false);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(4);
        p.bottomMargin = dp(12);
        parent.addView(caption, p);
    }

    private void addBar(LinearLayout parent, String name, double value,
                        double maximum, String valueLabel, int barColor) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.bottomMargin = dp(13);
        parent.addView(row, rowParams);

        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);
        TextView caption = label(name, 13, R.color.md_theme_onSurface, false);
        caption.setMaxLines(2);
        line.addView(caption, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView amount = label(valueLabel, 12, R.color.md_theme_primary, true);
        amount.setGravity(Gravity.END);
        line.addView(amount);
        row.addView(line);

        // Horizontal bar chart built using standard Android Views. No chart SDK needed.
        LinearLayout track = new LinearLayout(this);
        track.setOrientation(LinearLayout.HORIZONTAL);
        track.setWeightSum(100f);
        track.setBackground(rounded(R.color.md_theme_surfaceContainerHighest, 10));
        LinearLayout.LayoutParams trackParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(11));
        trackParams.topMargin = dp(5);
        row.addView(track, trackParams);
        float fraction = maximum <= 0 ? 0f :
                (float) Math.max(0, Math.min(100, 100.0 * value / maximum));
        if (fraction > 0) {
            View fill = new View(this);
            fill.setBackground(rounded(barColor, 10));
            track.addView(fill, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.MATCH_PARENT, fraction));
        }
        View rest = new View(this);
        track.addView(rest, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, 100f - fraction));
    }

    private GradientDrawable rounded(int colorResource, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color(colorResource));
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    private TextView label(String value, int sp, int colorResource, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color(colorResource));
        if (bold) view.setTypeface(null, Typeface.BOLD);
        return view;
    }

    private double number(DocumentSnapshot doc, String key) {
        Object value = doc.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : 0;
    }

    private String monthKey(Calendar date) {
        return String.format(Locale.ROOT, "%04d-%02d",
                date.get(Calendar.YEAR), date.get(Calendar.MONTH) + 1);
    }

    private String readableStatus(String status) {
        switch (status) {
            case "placed": return "Placed";
            case "preparing": return "Preparing";
            case "out_for_delivery": return "Out for delivery";
            case "delivered": return "Delivered";
            case "cancelled": return "Cancelled";
            case "payment_pending": return "Payment pending";
            default: return "Other";
        }
    }

    private String money(double amount) {
        return String.format(Locale.US, "Rs. %,.2f", amount);
    }

    private int color(int res) {
        return ContextCompat.getColor(this, res);
    }

    private int dp(int value) {
        return (int) (getResources().getDisplayMetrics().density * value + 0.5f);
    }

    private boolean active(int version) {
        return version == loadVersion && !isFinishing() && !isDestroyed();
    }
}
