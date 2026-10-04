package lk.jiat.butterqueen.data;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;

/** UI gating only; Firestore Security Rules must enforce the same role server-side. */
public final class AdminAccess {
    public interface Callback {
        void onResult(boolean allowed);
    }

    private AdminAccess() {}

    public static void verify(Callback callback) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            callback.onResult(false);
            return;
        }
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .get(Source.SERVER)
                .addOnSuccessListener(doc ->
                        callback.onResult("admin".equals(doc.getString("role"))))
                .addOnFailureListener(error -> callback.onResult(false));
    }
}
