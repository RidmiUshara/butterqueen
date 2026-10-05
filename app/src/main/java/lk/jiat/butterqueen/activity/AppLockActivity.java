package lk.jiat.butterqueen.activity;

import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lk.jiat.butterqueen.AppLockGate;
import lk.jiat.butterqueen.R;
import lk.jiat.butterqueen.data.AppLockStore;

/** Opaque foreground gate. The account's saved six-digit code works when biometrics do not. */
public final class AppLockActivity extends AppCompatActivity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private BiometricPrompt biometricPrompt;
    private TextInputEditText codeInput;
    private String ownerUid;
    private boolean prompted;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || !AppLockStore.enabled(this, user.getUid())) {
            finish();
            return;
        }
        ownerUid = user.getUid();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_app_lock);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { moveTaskToBack(true); }
        });
        codeInput = findViewById(R.id.lockCode);
        biometricPrompt = new BiometricPrompt(this, ContextCompat.getMainExecutor(this),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult result) {
                        if (sameAccount()) unlock();
                    }

                    @Override public void onAuthenticationFailed() {
                        message(R.string.lock_biometric_failed);
                    }

                    @Override public void onAuthenticationError(int code, @NonNull CharSequence error) {
                        message(R.string.lock_use_code);
                    }
                });

        findViewById(R.id.lockUseBiometric).setOnClickListener(v -> showBiometric());
        findViewById(R.id.lockSubmit).setOnClickListener(v -> verifyCode());
        findViewById(R.id.lockForgot).setOnClickListener(v -> resetUsingAccountPassword());
        boolean available = BiometricManager.from(this).canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS;
        findViewById(R.id.lockUseBiometric).setVisibility(available ? View.VISIBLE : View.GONE);
        ((TextView) findViewById(R.id.lockHint)).setText(available
                ? R.string.lock_biometric_or_code : R.string.lock_code_only);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ownerUid == null) return;
        if (!sameAccount()) {
            finish();
            return;
        }
        if (!prompted) {
            prompted = true;
            findViewById(R.id.lockUseBiometric).post(() -> {
                if (!isFinishing() && !isDestroyed()
                        && findViewById(R.id.lockUseBiometric).getVisibility() == View.VISIBLE)
                    showBiometric();
            });
        }
    }

    private void showBiometric() {
        if (!sameAccount() || BiometricManager.from(this).canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_WEAK) != BiometricManager.BIOMETRIC_SUCCESS) {
            message(R.string.lock_use_code);
            return;
        }
        biometricPrompt.authenticate(new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.lock_biometric_title))
                .setSubtitle(getString(R.string.lock_biometric_subtitle))
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .setNegativeButtonText(getString(R.string.lock_use_code))
                .build());
    }

    private void verifyCode() {
        if (!sameAccount()) return;
        Editable value = codeInput.getText();
        char[] code = value == null ? new char[0] : value.toString().toCharArray();
        codeInput.setText("");
        if (code.length != 6) {
            Arrays.fill(code, '\0');
            message(R.string.lock_six_digits);
            return;
        }
        findViewById(R.id.lockSubmit).setEnabled(false);
        worker.execute(() -> {
            AppLockStore.Result result;
            try { result = AppLockStore.verify(getApplicationContext(), ownerUid, code); }
            catch (RuntimeException error) { result = AppLockStore.Result.WRONG; }
            finally { Arrays.fill(code, '\0'); }
            final AppLockStore.Result checked = result;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed() || !sameAccount()) return;
                findViewById(R.id.lockSubmit).setEnabled(true);
                if (checked == AppLockStore.Result.MATCH) unlock();
                else message(checked == AppLockStore.Result.COOLDOWN
                        ? R.string.lock_cooldown : R.string.lock_wrong_code);
            });
        });
    }

    private void resetUsingAccountPassword() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (!sameAccount() || user == null || user.getEmail() == null) {
            message(R.string.lock_recovery_unavailable);
            return;
        }
        View form = getLayoutInflater().inflate(R.layout.dialog_lock_password, null);
        TextInputEditText password = form.findViewById(R.id.lockAccountPassword);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.lock_reset_title)
                .setMessage(getString(R.string.lock_reset_message, user.getEmail()))
                .setView(form)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.lock_reset_action, (dialog, which) -> {
                    String input = password.getText() == null ? "" : password.getText().toString();
                    if (input.isEmpty()) {
                        message(R.string.lock_password_required);
                        return;
                    }
                    user.reauthenticate(EmailAuthProvider.getCredential(user.getEmail(), input))
                            .addOnSuccessListener(this, unused -> {
                                if (!sameAccount()) return;
                                if (AppLockStore.disable(this, ownerUid)) {
                                    AppLockGate.unlock(ownerUid);
                                    message(R.string.lock_reset_done);
                                    finish();
                                } else message(R.string.lock_save_failed);
                            })
                            .addOnFailureListener(this, error -> message(R.string.lock_reset_failed));
                })
                .show();
    }

    private boolean sameAccount() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null && ownerUid != null && ownerUid.equals(user.getUid());
    }

    private void unlock() {
        AppLockGate.unlock(ownerUid);
        finish();
    }

    private void message(int resource) {
        Toast.makeText(this, resource, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        worker.shutdown();
        super.onDestroy();
    }
}
