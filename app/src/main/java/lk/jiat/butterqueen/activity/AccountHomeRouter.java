package lk.jiat.butterqueen.activity;

import android.app.Activity;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;

/** Choose customer or admin UI from the signed-in user's Firestore role. */
public final class AccountHomeRouter {
    private AccountHomeRouter() {}

    public static void openHome(Activity activity) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Intent signIn = new Intent(activity, SignInActivity.class);
            signIn.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity.startActivity(signIn);
            activity.finish();
            return;
        }
        String uid = user.getUid();
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .get(Source.DEFAULT)
                .addOnSuccessListener(activity, profile -> {
                    if (activity.isFinishing() || activity.isDestroyed()) return;
                    FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
                    if (current == null || !uid.equals(current.getUid())) return;
                    if (!profile.exists()) {
                        showRetry(activity, "Account profile could not be found.");
                        return;
                    }
                    Class<?> home = "admin".equals(profile.getString("role"))
                            ? AdminOverviewActivity.class : MainActivity.class;
                    Intent destination = new Intent(activity, home);
                    destination.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    activity.startActivity(destination);
                    activity.finish();
                })
                .addOnFailureListener(activity, e -> {
                    if (!activity.isFinishing() && !activity.isDestroyed()) {
                        showRetry(activity, "Unable to verify account role. Check your internet.");
                    }
                });
    }

    private static void showRetry(Activity activity, String explanation) {
        new AlertDialog.Builder(activity)
                .setTitle("Account verification")
                .setMessage(explanation + " Please try again.")
                .setCancelable(false)
                .setPositiveButton("Retry", (dialog, which) -> openHome(activity))
                .setNegativeButton("Sign out", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent signIn = new Intent(activity, SignInActivity.class);
                    signIn.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    activity.startActivity(signIn);
                    activity.finish();
                })
                .show();
    }
}
