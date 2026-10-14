package lk.jiat.butterqueen.notification;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Private device-token registration for the currently signed-in Firebase account. */
public final class PushTokenRegistrar {
    private static final String PREFS = "butterqueen_push_device";
    private static final String INSTALLATION = "installation_id";

    private PushTokenRegistrar() {}

    private static String id(Context app) {
        SharedPreferences prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String id = prefs.getString(INSTALLATION, null);
        if (id == null) {
            id = UUID.randomUUID().toString();
            prefs.edit().putString(INSTALLATION, id).apply();
        }
        return id;
    }

    public static void register(Context context) {
        FirebaseUser signedIn = FirebaseAuth.getInstance().getCurrentUser();
        if (signedIn == null) return;
        String uid = signedIn.getUid();
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token ->
                saveCurrentToken(context.getApplicationContext(), uid, token));
    }

    public static void refreshed(Context context, String token) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) saveCurrentToken(context.getApplicationContext(), user.getUid(), token);
    }

    private static void saveCurrentToken(Context app, String uid, String token) {
        FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
        if (current == null || !uid.equals(current.getUid())
                || token == null || token.length() < 20) return;
        Map<String, Object> fields = new HashMap<>();
        fields.put("token", token);
        fields.put("platform", "android");
        fields.put("updatedAtMillis", System.currentTimeMillis());
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .collection("pushTokens").document(id(app)).set(fields);
    }

    public static void unregister(Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                    .collection("pushTokens").document(id(context.getApplicationContext()))
                    .delete();
        }
        // The server also includes recipientUid. The MessagingService checks it,
        // so an outdated token cannot show someone else's push after account change.
    }
}
