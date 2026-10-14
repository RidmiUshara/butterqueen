package lk.jiat.butterqueen.activity;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import lk.jiat.butterqueen.R;
import lk.jiat.butterqueen.data.AdminAccess;

/** Admin-only view for Firestore customCakeRequests. Never writes customer details. */
public class AdminCustomCakeRequestsActivity extends AppCompatActivity {

    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private LinearLayout rows;
    private TextView summary;
    private boolean authorized = false;

    private static final String[] STATUS_CODES = {
            "pending", "reviewing", "quoted", "approved", "rejected"
    };
    private static final String[] STATUS_LABELS = {
            "Pending", "Reviewing", "Quote discussed", "Approved", "Rejected"
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_admin_custom_cake_requests);
        applyWindowInsets();
        rows = findViewById(R.id.adminCustomRequestsRows);
        summary = findViewById(R.id.adminCustomRequestsStatus);
        findViewById(R.id.adminCustomRequestsBack).setOnClickListener(v -> finish());
        findViewById(R.id.adminCustomRequestsRefresh).setOnClickListener(v -> loadRequests());

        summary.setText("Checking admin access...");
        AdminAccess.verify(allowed -> {
            if (isFinishing() || isDestroyed()) return;
            if (!allowed) {
                Toast.makeText(this, "Admin access required", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            authorized = true;
            loadRequests();
        });
    }

    private void applyWindowInsets() {
        // Keep the toolbar clear of the clock on Android 15+ edge-to-edge devices.
        View root = findViewById(R.id.adminCustomRequestsRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    private void loadRequests() {
        if (!authorized || isFinishing() || isDestroyed()) return;
        summary.setText("Loading custom cake requests...");
        rows.removeAllViews();

        // SERVER ensures the admin does not unknowingly display a stale offline snapshot.
        firestore.collection("customCakeRequests").get(Source.SERVER)
                .addOnSuccessListener(snapshot -> {
                    if (isFinishing() || isDestroyed()) return;
                    List<DocumentSnapshot> documents = new ArrayList<>(snapshot.getDocuments());
                    documents.sort((a, b) -> Long.compare(createdAt(b), createdAt(a)));
                    summary.setText(documents.isEmpty()
                            ? "No custom cake requests yet."
                            : documents.size() + " custom cake request(s) — tap to review");
                    for (DocumentSnapshot doc : documents) {
                        addRequestCard(doc);
                    }
                })
                .addOnFailureListener(error -> {
                    if (isFinishing() || isDestroyed()) return;
                    summary.setText("Could not load requests. Check internet and admin permissions.");
                    Toast.makeText(this, "Load failed: " + error.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void addRequestCard(DocumentSnapshot doc) {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.bottomMargin = dp(12);
        rows.addView(card, p);
        card.setRadius(dp(16));
        card.setCardElevation(0);
        card.setStrokeWidth(dp(1));
        card.setCardBackgroundColor(ContextCompat.getColor(this,
                R.color.md_theme_surfaceContainerLow));
        card.setStrokeColor(ContextCompat.getColor(this,
                R.color.md_theme_outlineVariant));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.addView(content);

        TextView title = new TextView(this);
        title.setText(value(doc.getString("flavour")) + " • "
                + value(doc.getString("weight")));
        title.setTextColor(ContextCompat.getColor(this, R.color.md_theme_onSurface));
        title.setTextSize(17);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        content.addView(title);

        TextView detail = new TextView(this);
        detail.setText("For " + value(doc.getString("requiredDate"))
                + "\n" + value(doc.getString("userEmail"))
                + "\nStatus: " + value(doc.getString("status")));
        detail.setTextSize(14);
        detail.setTextColor(ContextCompat.getColor(this, R.color.md_theme_onSurfaceVariant));
        detail.setPadding(0, dp(6), 0, 0);
        content.addView(detail);

        card.setOnClickListener(v -> showDetails(doc));
    }

    private void showDetails(DocumentSnapshot doc) {
        String created = "-";
        Timestamp stamp = doc.getTimestamp("createdAt");
        if (stamp != null) {
            created = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                    .format(stamp.toDate());
        }
        boolean hasPhoto = !value(doc.getString("referenceImagePath")).equals("-");
        String message = "Email: " + value(doc.getString("userEmail"))
                + "\nFlavour: " + value(doc.getString("flavour"))
                + "\nWeight: " + value(doc.getString("weight"))
                + "\nRequired date: " + value(doc.getString("requiredDate"))
                + "\nCake message: " + value(doc.getString("cakeMessage"))
                + "\nDesign notes: " + value(doc.getString("notes"))
                + "\nReference photo: " + (hasPhoto ? "Attached" : "None")
                + "\nCreated: " + created
                + "\nStatus: " + value(doc.getString("status"));

        new AlertDialog.Builder(this)
                .setTitle("Custom Cake Request")
                .setMessage(message)
                .setNegativeButton("Close", null)
                .setPositiveButton("Update status", (dialog, which) -> chooseStatus(doc))
                .show();
    }

    private void chooseStatus(DocumentSnapshot doc) {
        new AlertDialog.Builder(this)
                .setTitle("Update request status")
                .setSingleChoiceItems(STATUS_LABELS, statusIndex(doc.getString("status")),
                        (dialog, selected) -> {
                            dialog.dismiss();
                            String newStatus = STATUS_CODES[selected];
                            if (newStatus.equals(doc.getString("status"))) return;
                            firestore.collection("customCakeRequests")
                                    .document(doc.getId())
                                    .update("status", newStatus)
                                    .addOnSuccessListener(unused -> {
                                        if (!isFinishing() && !isDestroyed()) {
                                            Toast.makeText(this, "Status updated", Toast.LENGTH_SHORT).show();
                                            loadRequests();
                                        }
                                    })
                                    .addOnFailureListener(error -> {
                                        if (!isFinishing() && !isDestroyed()) {
                                            Toast.makeText(this,
                                                    "Update failed: " + error.getMessage(),
                                                    Toast.LENGTH_LONG).show();
                                        }
                                    });
                        })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private int statusIndex(String status) {
        for (int i = 0; i < STATUS_CODES.length; i++) {
            if (STATUS_CODES[i].equals(status)) return i;
        }
        return -1;
    }

    private long createdAt(DocumentSnapshot doc) {
        Timestamp timestamp = doc.getTimestamp("createdAt");
        Date date = timestamp == null ? null : timestamp.toDate();
        return date == null ? 0 : date.getTime();
    }

    private String value(String s) {
        return s == null || s.trim().isEmpty() ? "-" : s.trim();
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }
}
