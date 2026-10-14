package lk.jiat.butterqueen.notification;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Firebase Spark fallback: synthesizes user-visible alerts from authenticated
 * customCakeRequests reads. No client writes into anyone's notifications inbox.
 * Read state belongs to this installation/account only.
 */
public final class SparkCakeNotifications {
    private static final String PREFS = "bq_cake_notifications_";
    private SparkCakeNotifications() {}

    public static final class Notice {
        public final String key;
        public final String requestId;
        public final String type;
        public final String title;
        public final String body;
        public final long atMillis;

        private Notice(String key, String requestId, String type,
                       String title, String body, long atMillis) {
            this.key = key;
            this.requestId = requestId;
            this.type = type;
            this.title = title;
            this.body = body;
            this.atMillis = atMillis;
        }
    }

    public interface Listener {
        void onNotices(List<Notice> notices);
        void onError(Exception error);
    }

    private static SharedPreferences preferences(Context context, String uid) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS + uid, Context.MODE_PRIVATE);
    }

    public static boolean isRead(Context context, String uid, Notice notice) {
        return preferences(context, uid).getBoolean(notice.key, false);
    }

    public static int unread(Context context, String uid, List<Notice> notices) {
        int count = 0;
        for (Notice notice : notices) if (!isRead(context, uid, notice)) count++;
        return count;
    }

    public static void markRead(Context context, String uid, List<Notice> notices) {
        SharedPreferences.Editor edit = preferences(context, uid).edit();
        for (Notice notice : notices) edit.putBoolean(notice.key, true);
        edit.apply();
    }

    public static SharedPreferences getReadPreferences(Context context, String uid) {
        return preferences(context, uid);
    }

    /** Admins can list all requests. Customers must filter to their own UID. */
    public static ListenerRegistration watch(String uid, boolean admin, Listener listener) {
        Query query = FirebaseFirestore.getInstance().collection("customCakeRequests");
        if (!admin) query = query.whereEqualTo("userId", uid);
        // Do not orderBy here: customer whereEqualTo + orderBy would require an index.
        return query.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                listener.onError(error);
                return;
            }
            if (snapshot == null) return;
            List<Notice> notices = new ArrayList<>();
            for (DocumentSnapshot doc : snapshot) {
                if (!admin && !uid.equals(doc.getString("userId"))) continue;
                String id = doc.getId();
                Timestamp timestamp = doc.getTimestamp("createdAt");
                long at = timestamp == null ? 0L : timestamp.toDate().getTime();
                String flavour = doc.getString("flavour");
                if (flavour == null || flavour.trim().isEmpty()) flavour = "cake";
                String status = doc.getString("status");
                if (status == null) status = "pending";
                if (admin) {
                    notices.add(new Notice("admin_created_" + id, id, "admin_new",
                            "New custom cake request",
                            "A customer requested a " + flavour + " cake. Tap to review.",
                            at));
                } else {
                    notices.add(new Notice("customer_created_" + id, id, "customer_sent",
                            "Custom cake request received",
                            "Your " + flavour + " cake request was submitted. The bakery will review it.",
                            at));
                    if (!"pending".equals(status)) {
                        String label = status.replace('_', ' ').toLowerCase(Locale.ROOT);
                        notices.add(new Notice("customer_status_" + id + "_" + status,
                                id, "customer_status",
                                "Custom cake request updated",
                                "Your cake request is now " + label + ".", at + 1));
                    }
                }
            }
            Collections.sort(notices, (left, right) ->
                    Long.compare(right.atMillis, left.atMillis));
            listener.onNotices(notices);
        });
    }
}
