package lk.jiat.butterqueen.activity;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lk.jiat.butterqueen.R;

/** Customer-only, realtime view of custom cake requests stored in Firestore. */
public class MyCustomCakeRequestsActivity extends AppCompatActivity {
    private LinearLayout rows;
    private TextView statusText;
    private ListenerRegistration requestListener;
    private boolean signInPromptShown;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_my_custom_cake_requests);
        rows = findViewById(R.id.myRequestsRows);
        statusText = findViewById(R.id.myRequestsStatus);
        ((com.google.android.material.appbar.MaterialToolbar)
                findViewById(R.id.myRequestsToolbar)).setNavigationOnClickListener(v -> finish());
        applyWindowInsets();
    }

    // Android 15+ draws content edge-to-edge: keep the toolbar clear of the clock.
    private void applyWindowInsets() {
        View root = findViewById(R.id.myRequestsRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            rows.removeAllViews();
            statusText.setText("Please sign in to view your cake requests.");
            if (!signInPromptShown) {
                signInPromptShown = true;
                new AlertDialog.Builder(this)
                        .setTitle("Sign in required")
                        .setMessage("Sign in to view only your own custom cake requests.")
                        .setPositiveButton("Sign in", (dialog, which) -> {
                            startActivity(new Intent(this, SignInActivity.class));
                            finish();
                        })
                        .setNegativeButton("Not now", (dialog, which) -> finish())
                        .show();
            }
            return;
        }
        observeRequests(user.getUid());
    }

    /** Restrict the query by userId: required by the existing Firestore read rule. */
    private void observeRequests(String uid) {
        if (requestListener != null) requestListener.remove();
        statusText.setText("Loading your requests...");
        requestListener = FirebaseFirestore.getInstance()
                .collection("customCakeRequests")
                .whereEqualTo("userId", uid)
                .addSnapshotListener((snapshot, error) -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (error != null) {
                        rows.removeAllViews();
                        statusText.setText("Could not load requests: " + error.getMessage());
                        return;
                    }
                    if (snapshot == null) return;

                    List<DocumentSnapshot> documents = new ArrayList<>(snapshot.getDocuments());
                    // Client-side sorting avoids a Firestore composite-index requirement.
                    documents.sort((left, right) ->
                            Long.compare(createdAtMillis(right), createdAtMillis(left)));
                    rows.removeAllViews();
                    boolean fromCache = snapshot.getMetadata().isFromCache();
                    if (documents.isEmpty()) {
                        statusText.setText(fromCache
                                ? "No locally cached requests. Connect to check Firebase."
                                : "No custom cake requests yet. Try Request a Custom Cake!");
                        return;
                    }
                    statusText.setText(documents.size() + " request(s)"
                            + (fromCache ? " • Offline/cached data" : " • Synced with Firestore"));
                    for (DocumentSnapshot doc : documents) addRequestCard(doc);
                });
    }

    private void addRequestCard(DocumentSnapshot doc) {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(12);
        rows.addView(card, cardParams);
        card.setRadius(dp(18));
        card.setCardElevation(0);
        card.setCardBackgroundColor(ContextCompat.getColor(this,
                R.color.md_theme_surfaceContainerLow));
        card.setStrokeColor(ContextCompat.getColor(this, R.color.md_theme_outlineVariant));
        card.setStrokeWidth(dp(1));

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(14), dp(16), dp(16));
        card.addView(box);

        String flavour = value(doc.getString("flavour"), "Custom cake");
        String weight = value(doc.getString("weight"), "Weight unspecified");
        addText(box, flavour + " • " + weight, 17, R.color.md_theme_onBackground, true, 0);

        String status = value(doc.getString("status"), "pending").toLowerCase(Locale.ROOT);
        addText(box, "Status: " + readableStatus(status), 15,
                R.color.md_theme_primary, true, 6);
        addText(box, statusDescription(status), 13,
                R.color.md_theme_onSurfaceVariant, false, 2);

        addText(box, "Required date: " + value(doc.getString("requiredDate"), "Not set"),
                14, R.color.md_theme_onBackground, false, 12);
        String cakeMessage = doc.getString("cakeMessage");
        if (cakeMessage != null && !cakeMessage.trim().isEmpty()) {
            addText(box, "Cake message: " + cakeMessage.trim(), 14,
                    R.color.md_theme_onBackground, false, 5);
        }
        String notes = doc.getString("notes");
        if (notes != null && !notes.trim().isEmpty()) {
            addText(box, "Design notes: " + notes.trim(), 14,
                    R.color.md_theme_onBackground, false, 5);
        }
        Timestamp createdAt = doc.getTimestamp("createdAt");
        if (createdAt != null) {
            String date = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                    .format(createdAt.toDate());
            addText(box, "Submitted: " + date, 12,
                    R.color.md_theme_onSurfaceVariant, false, 11);
        }
        addText(box, "Request ID: " + doc.getId().substring(0, Math.min(8, doc.getId().length())),
                12, R.color.md_theme_onSurfaceVariant, false, 3);
    }

    private void addText(LinearLayout parent, String text, int sp, int color, boolean bold, int topDp) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextSize(sp);
        label.setTextColor(ContextCompat.getColor(this, color));
        if (bold) label.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(topDp);
        parent.addView(label, params);
    }

    private static String value(String input, String fallback) {
        return input == null || input.trim().isEmpty() ? fallback : input.trim();
    }

    private static long createdAtMillis(DocumentSnapshot doc) {
        Timestamp stamp = doc.getTimestamp("createdAt");
        return stamp == null ? 0L : stamp.toDate().getTime();
    }

    private static String readableStatus(String status) {
        switch (status) {
            case "pending": return "Pending";
            case "reviewing": return "Reviewing";
            case "quoted": return "Quoted";
            case "approved": return "Approved";
            case "rejected": return "Rejected";
            default: return status;
        }
    }

    private static String statusDescription(String status) {
        switch (status) {
            case "pending": return "The bakery has received your request.";
            case "reviewing": return "The bakery is reviewing your cake design.";
            case "quoted": return "The bakery has marked this request as quoted. Contact the bakery for pricing details.";
            case "approved": return "Your request is approved. Confirm the final details with the bakery.";
            case "rejected": return "This request was declined. You can contact the bakery for alternatives.";
            default: return "Please contact the bakery if you need an update.";
        }
    }

    private int dp(int value) {
        return (int) (getResources().getDisplayMetrics().density * value + 0.5f);
    }

    @Override
    protected void onStop() {
        if (requestListener != null) {
            requestListener.remove();
            requestListener = null;
        }
        super.onStop();
    }
}
