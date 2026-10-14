package lk.jiat.butterqueen.activity;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import lk.jiat.butterqueen.R;

/** Per-user notification inbox. Opening it marks unread records as seen. */
public final class NotificationInboxActivity extends AppCompatActivity {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration listener;
    private LinearLayout content;
    private String uid;
    private boolean marking;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(color(R.color.md_theme_background));
        root.setPadding(dp(18), 0, dp(18), dp(12));
        setContentView(root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(dp(18) + bars.left, bars.top, dp(18) + bars.right, dp(12) + bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(64)));
        MaterialButton back = new MaterialButton(this);
        back.setText("‹ Back");
        back.setAllCaps(false);
        header.addView(back, new LinearLayout.LayoutParams(dp(92), dp(48)));
        back.setOnClickListener(v -> finish());
        TextView title = text("Notifications", 23, true);
        header.addView(title);
        TextView subtitle = text("New updates are marked as read when you open this page", 12, false);
        subtitle.setTextColor(color(R.color.md_theme_onSurfaceVariant));
        root.addView(subtitle);
        ScrollView scroll = new ScrollView(this);
        LinearLayout.LayoutParams sc = new LinearLayout.LayoutParams(-1, 0, 1);
        sc.topMargin = dp(15);
        root.addView(scroll, sc);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!sameUser()) return;
        listener = db.collection("users").document(uid).collection("notifications")
                .orderBy("createdAtMillis", Query.Direction.DESCENDING).limit(100)
                .addSnapshotListener((value, error) -> {
                    if (!sameUser() || isFinishing() || isDestroyed()) return;
                    if (error != null) {
                        content.removeAllViews();
                        content.addView(text("Could not load notifications. Check connection and rules.",
                                14, false));
                        return;
                    }
                    if (value != null) render(value.getDocuments());
                });
        markAllUnread();
    }

    @Override
    protected void onStop() {
        if (listener != null) listener.remove();
        listener = null;
        super.onStop();
    }

    private boolean sameUser() {
        return FirebaseAuth.getInstance().getCurrentUser() != null
                && uid.equals(FirebaseAuth.getInstance().getCurrentUser().getUid());
    }

    private void markAllUnread() {
        if (marking || !sameUser()) return;
        marking = true;
        db.collection("users").document(uid).collection("notifications")
                .whereEqualTo("read", false).limit(300).get()
                .addOnSuccessListener(snapshot -> {
                    if (!sameUser()) { marking = false; return; }
                    if (snapshot.isEmpty()) { marking = false; return; }
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : snapshot) batch.update(doc.getReference(), "read", true);
                    batch.commit().addOnCompleteListener(result -> {
                        marking = false;
                        if (result.isSuccessful() && snapshot.size() == 300 && sameUser())
                            markAllUnread();
                    });
                })
                .addOnFailureListener(error -> {
                    marking = false;
                    if (!isFinishing()) Toast.makeText(this,
                            "Cannot mark notifications read", Toast.LENGTH_SHORT).show();
                });
    }

    private void render(List<DocumentSnapshot> docs) {
        content.removeAllViews();
        if (docs.isEmpty()) {
            content.addView(text("You're all caught up! New updates will appear here.", 15, false));
            return;
        }
        for (DocumentSnapshot doc : docs) {
            String title = doc.getString("title");
            String body = doc.getString("body");
            MaterialCardView card = new MaterialCardView(this);
            card.setRadius(dp(18));
            card.setCardElevation(0);
            card.setCardBackgroundColor(color(R.color.md_theme_surfaceContainerLow));
            card.setStrokeColor(color(R.color.md_theme_outlineVariant));
            card.setStrokeWidth(dp(1));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
            cp.bottomMargin = dp(12);
            content.addView(card, cp);
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(dp(16), dp(14), dp(16), dp(14));
            card.addView(item);
            TextView heading = text(title == null ? "Butter Queen" : title, 16, true);
            item.addView(heading);
            TextView detail = text(body == null ? "" : body, 13, false);
            detail.setTextColor(color(R.color.md_theme_onSurfaceVariant));
            LinearLayout.LayoutParams details = new LinearLayout.LayoutParams(-1, -2);
            details.topMargin = dp(6);
            item.addView(detail, details);
            Long stamp = doc.getLong("createdAtMillis");
            TextView date = text(stamp == null ? "" : new SimpleDateFormat(
                    "dd MMM yyyy · h:mm a", Locale.getDefault()).format(new Date(stamp)), 11, false);
            date.setTextColor(color(R.color.md_theme_onSurfaceVariant));
            LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(-1, -2);
            dateParams.topMargin = dp(8);
            item.addView(date, dateParams);
            card.setOnClickListener(v -> openTarget(doc));
        }
    }

    private void openTarget(DocumentSnapshot doc) {
        if (!sameUser()) { finish(); return; }
        String type = doc.getString("type");
        String ref = doc.getString("targetId");
        Intent target = null;
        switch (type == null ? "" : type) {
            case "new_order":
                target = new Intent(this, AdminDashboardActivity.class)
                        .putExtra("open_orders", true);
                break;
            case "new_product":
                if (ref != null && !ref.isEmpty())
                    target = new Intent(this, ProductDetailsActivity.class)
                            .putExtra("product_id", ref);
                break;
            case "order_status":
                if (ref != null && !ref.isEmpty())
                    target = OrderReceiptActivity.intentFor(this, ref);
                break;
            case "custom_request":
                target = new Intent(this, AdminCustomCakeRequestsActivity.class);
                break;
            case "custom_status":
                target = new Intent(this, MyCustomCakeRequestsActivity.class);
                break;
            default:
                break;
        }
        if (target != null) startActivity(target);
    }

    private TextView text(String value, int size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color(R.color.md_theme_onBackground));
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    private int color(int id) { return ContextCompat.getColor(this, id); }
    private int dp(int d) {
        return Math.round(d * getResources().getDisplayMetrics().density);
    }
}
