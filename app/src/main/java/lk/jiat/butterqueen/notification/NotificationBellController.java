package lk.jiat.butterqueen.notification;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import lk.jiat.butterqueen.R;
import lk.jiat.butterqueen.activity.NotificationInboxActivity;

/** Unread counter backed by the signed-in user's private Firestore inbox. */
public final class NotificationBellController {
    private final FrameLayout host;
    private final TextView badge;
    private ListenerRegistration registration;

    public NotificationBellController(Activity activity, FrameLayout host) {
        this.host = host;
        host.removeAllViews();
        ImageButton bell = new ImageButton(activity);
        bell.setImageResource(R.drawable.ic_bq_bell);
        bell.setImageTintList(ColorStateList.valueOf(
                ContextCompat.getColor(activity, R.color.md_theme_onSurface)));
        bell.setBackgroundResource(android.R.color.transparent);
        bell.setContentDescription("Open notifications");
        host.addView(bell, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        bell.setOnClickListener(v ->
                activity.startActivity(new Intent(activity, NotificationInboxActivity.class)));

        badge = new TextView(activity);
        badge.setTextColor(0xFFFFFFFF);
        badge.setTextSize(10);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(activity, 5), 0, dp(activity, 5), 0);
        badge.setMinWidth(dp(activity, 19));
        badge.setMinHeight(dp(activity, 19));
        android.graphics.drawable.GradientDrawable background =
                new android.graphics.drawable.GradientDrawable();
        background.setColor(0xFFE53935);
        background.setCornerRadius(dp(activity, 14));
        badge.setBackground(background);
        FrameLayout.LayoutParams b = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.END);
        b.topMargin = dp(activity, 3);
        b.rightMargin = dp(activity, 2);
        host.addView(badge, b);
        badge.setVisibility(View.GONE);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        registration = FirebaseFirestore.getInstance()
                .collection("users").document(uid).collection("notifications")
                .whereEqualTo("read", false).limit(100)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null || FirebaseAuth.getInstance()
                            .getCurrentUser() == null || !uid.equals(
                            FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                        badge.setVisibility(View.GONE);
                        return;
                    }
                    int count = snapshot.size();
                    badge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
                    badge.setText(count >= 100 ? "99+" : String.valueOf(count));
                    bell.setContentDescription(count + " unread notifications");
                });
    }

    private static int dp(Activity host, int val) {
        return Math.round(val * host.getResources().getDisplayMetrics().density);
    }

    public void close() {
        if (registration != null) registration.remove();
        registration = null;
        host.removeAllViews();
    }
}
