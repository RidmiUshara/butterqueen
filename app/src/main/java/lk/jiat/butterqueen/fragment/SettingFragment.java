package lk.jiat.butterqueen.fragment;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.app.AlertDialog;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lk.jiat.butterqueen.AppLockGate;
import lk.jiat.butterqueen.R;
import lk.jiat.butterqueen.activity.MainActivity;
import lk.jiat.butterqueen.data.AppLockStore;
import lk.jiat.butterqueen.data.AppSettings;
import lk.jiat.butterqueen.data.ContactActions;
import lk.jiat.butterqueen.data.PasscodeHash;

public class SettingFragment extends Fragment {
    private enum BiometricAction { NONE, ENABLE, VERIFY }

    private final ExecutorService lockWorker = Executors.newSingleThreadExecutor();
    private BiometricPrompt biometricPrompt;
    private BiometricAction biometricAction = BiometricAction.NONE;
    private char[] pendingCode;
    private String pendingUid;

    private static final int[] THEME_MODES = {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            AppCompatDelegate.MODE_NIGHT_NO,
            AppCompatDelegate.MODE_NIGHT_YES
    };

    @Override
    public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        biometricPrompt = new BiometricPrompt(this, ContextCompat.getMainExecutor(requireContext()),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult result) {
                        if (biometricAction == BiometricAction.ENABLE && pendingCode != null)
                            completeSetup();
                        else if (biometricAction == BiometricAction.VERIFY && isAdded())
                            Toast.makeText(requireContext(), R.string.settings_biometric_verified,
                                    Toast.LENGTH_SHORT).show();
                        biometricAction = BiometricAction.NONE;
                    }

                    @Override public void onAuthenticationFailed() {
                        if (isAdded()) Toast.makeText(requireContext(), R.string.lock_biometric_failed,
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override public void onAuthenticationError(int errorCode,
                                                                 @NonNull CharSequence error) {
                        clearPendingCode();
                        biometricAction = BiometricAction.NONE;
                        if (getView() != null) refreshValues(requireView());
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_setting, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.settingsThemeRow).setOnClickListener(v -> showThemeDialog());
        view.findViewById(R.id.settingsBiometricSwitch).setOnClickListener(v -> toggleAppLock());
        view.findViewById(R.id.settingsVerifyBiometric).setOnClickListener(v ->
                promptBiometric(BiometricAction.VERIFY));
        view.findViewById(R.id.settingsChangeCode).setOnClickListener(v -> changeCode());
        view.findViewById(R.id.settingsShakeSwitch).setOnClickListener(v -> {
            SensorManager manager = (SensorManager) requireContext()
                    .getSystemService(android.content.Context.SENSOR_SERVICE);
            boolean supported = manager != null
                    && manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null;
            if (supported) {
                boolean enabled = ((MaterialSwitch) v).isChecked();
                AppSettings.setShakeRefresh(requireContext(), enabled);
            }
            refreshValues(view);
        });
        view.findViewById(R.id.settingsHelp).setOnClickListener(v ->
                ((MainActivity) requireActivity()).openHelpScreen());
        view.findViewById(R.id.settingsAbout).setOnClickListener(v ->
                ((MainActivity) requireActivity()).openAboutScreen());
        view.findViewById(R.id.settingsCall).setOnClickListener(v ->
                ContactActions.call(requireContext()));
        view.findViewById(R.id.settingsEmail).setOnClickListener(v ->
                ContactActions.email(requireContext(), "ButterQueen support"));
        refreshValues(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) refreshValues(requireView());
    }

    private void refreshValues(View view) {
        int mode = AppSettings.themeMode(requireContext());
        ((TextView) view.findViewById(R.id.settingsThemeValue)).setText(
                mode == AppCompatDelegate.MODE_NIGHT_YES ? "Dark"
                        : mode == AppCompatDelegate.MODE_NIGHT_NO ? "Light"
                        : "Follow device");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        boolean enabled = user != null && AppLockStore.enabled(requireContext(), user.getUid());
        int biometric = BiometricManager.from(requireContext()).canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_WEAK);
        MaterialSwitch appLock = view.findViewById(R.id.settingsBiometricSwitch);
        appLock.setChecked(enabled);
        appLock.setEnabled(user != null);
        view.findViewById(R.id.settingsVerifyBiometric).setEnabled(
                biometric == BiometricManager.BIOMETRIC_SUCCESS);
        view.findViewById(R.id.settingsChangeCode).setVisibility(enabled ? View.VISIBLE : View.GONE);
        int biometricLabel = enabled
                ? (biometric == BiometricManager.BIOMETRIC_SUCCESS
                ? R.string.settings_biometric_on : R.string.settings_biometric_code_only)
                : biometric == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
                ? R.string.settings_biometric_enroll : biometric == BiometricManager.BIOMETRIC_SUCCESS
                ? R.string.settings_biometric_off : R.string.settings_biometric_unavailable;
        ((TextView) view.findViewById(R.id.settingsBiometricValue)).setText(
                user == null ? "Sign in to set an app lock" : getString(biometricLabel));

        SensorManager manager = (SensorManager) requireContext()
                .getSystemService(android.content.Context.SENSOR_SERVICE);
        boolean motion = manager != null
                && manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null;
        MaterialSwitch shake = view.findViewById(R.id.settingsShakeSwitch);
        shake.setEnabled(motion);
        shake.setChecked(motion && AppSettings.shakeRefresh(requireContext()));
        ((TextView) view.findViewById(R.id.settingsMotionStatus)).setText(
                motion ? "Shake the phone on Home to reload the catalog. Off by default."
                        : "This device has no accelerometer.");
    }

    private String currentUid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user == null ? null : user.getUid();
    }

    private void toggleAppLock() {
        String uid = currentUid();
        if (uid == null) return;
        refreshValues(requireView()); // Do not show an unverified switch state.
        if (AppLockStore.enabled(requireContext(), uid)) {
            disableWithCode(uid);
        } else {
            int biometric = BiometricManager.from(requireContext()).canAuthenticate(
                    BiometricManager.Authenticators.BIOMETRIC_WEAK);
            if (biometric == BiometricManager.BIOMETRIC_SUCCESS) setupCode(uid);
            else if (biometric == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED)
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.settings_biometric_enroll_title)
                        .setMessage(R.string.settings_biometric_enroll)
                        .setPositiveButton(R.string.settings_open_security, (dialog, which) -> {
                            try { startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS)); }
                            catch (ActivityNotFoundException error) {
                                Toast.makeText(requireContext(), R.string.settings_cannot_open,
                                        Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton(R.string.cancel, null).show();
            else Toast.makeText(requireContext(), R.string.settings_biometric_unavailable,
                    Toast.LENGTH_LONG).show();
        }
    }

    private void setupCode(String uid) {
        View form = getLayoutInflater().inflate(R.layout.dialog_lock_codes, null);
        TextInputEditText first = form.findViewById(R.id.lockNewCode);
        TextInputEditText confirm = form.findViewById(R.id.lockConfirmCode);
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.lock_setup_title)
                .setMessage(R.string.lock_setup_message)
                .setView(form)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.settings_verify_biometric, null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    char[] code = chars(first);
                    char[] repeated = chars(confirm);
                    if (!PasscodeHash.valid(code)) {
                        first.setError(getString(R.string.lock_six_digits));
                    } else if (!Arrays.equals(code, repeated)) {
                        confirm.setError(getString(R.string.lock_code_mismatch));
                    } else if (!uid.equals(currentUid())) {
                        dialog.dismiss();
                    } else {
                        clearPendingCode();
                        pendingCode = code;
                        pendingUid = uid;
                        first.setText("");
                        confirm.setText("");
                        dialog.dismiss();
                        promptBiometric(BiometricAction.ENABLE);
                        code = null;
                    }
                    if (code != null) Arrays.fill(code, '\0');
                    Arrays.fill(repeated, '\0');
                }));
        dialog.show();
    }

    private void promptBiometric(BiometricAction action) {
        if (BiometricManager.from(requireContext()).canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_WEAK) != BiometricManager.BIOMETRIC_SUCCESS) {
            clearPendingCode();
            refreshValues(requireView());
            return;
        }
        biometricAction = action;
        biometricPrompt.authenticate(new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(action == BiometricAction.ENABLE
                        ? R.string.lock_biometric_setup : R.string.settings_verify_biometric))
                .setSubtitle(getString(R.string.lock_biometric_subtitle))
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .setNegativeButtonText(getString(R.string.cancel))
                .build());
    }

    private void completeSetup() {
        char[] code = pendingCode;
        String uid = pendingUid;
        pendingCode = null;
        pendingUid = null;
        if (uid == null || !uid.equals(currentUid())) {
            Arrays.fill(code, '\0');
            return;
        }
        Activity host = requireActivity();
        android.content.Context app = requireContext().getApplicationContext();
        lockWorker.execute(() -> {
            boolean saved;
            try { saved = uid.equals(currentUid()) && AppLockStore.enable(app, uid, code); }
            catch (RuntimeException error) { saved = false; }
            finally { Arrays.fill(code, '\0'); }
            final boolean resultSaved = saved;
            host.runOnUiThread(() -> {
                if (!isAdded()) return;
                if (resultSaved && uid.equals(currentUid())) {
                    AppLockGate.unlock(uid);
                    Toast.makeText(requireContext(), R.string.lock_enabled, Toast.LENGTH_SHORT).show();
                } else Toast.makeText(requireContext(), R.string.lock_save_failed, Toast.LENGTH_LONG).show();
                refreshValues(requireView());
            });
        });
    }

    private void disableWithCode(String uid) {
        android.content.Context app = requireContext().getApplicationContext();
        View form = getLayoutInflater().inflate(R.layout.dialog_lock_single_code, null);
        TextInputEditText entry = form.findViewById(R.id.lockOldCode);
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.lock_disable_title)
                .setView(form)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.lock_disable_action, null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    char[] code = chars(entry);
                    if (!PasscodeHash.valid(code)) {
                        entry.setError(getString(R.string.lock_six_digits));
                        Arrays.fill(code, '\0');
                        return;
                    }
                    entry.setText("");
                    dialog.dismiss();
                    runCodeTask(uid, () -> {
                        AppLockStore.Result result = AppLockStore.verify(app, uid, code);
                        return result == AppLockStore.Result.MATCH
                                && AppLockStore.disable(app, uid)
                                ? AppLockStore.Result.MATCH : result == AppLockStore.Result.MATCH
                                ? AppLockStore.Result.WRONG : result;
                    }, code, R.string.lock_disabled);
                }));
        dialog.show();
    }

    private void changeCode() {
        String uid = currentUid();
        if (uid == null || !AppLockStore.enabled(requireContext(), uid)) return;
        android.content.Context app = requireContext().getApplicationContext();
        View form = getLayoutInflater().inflate(R.layout.dialog_lock_codes, null);
        form.findViewById(R.id.lockOldContainer).setVisibility(View.VISIBLE);
        TextInputEditText old = form.findViewById(R.id.lockOldCode);
        TextInputEditText next = form.findViewById(R.id.lockNewCode);
        TextInputEditText confirm = form.findViewById(R.id.lockConfirmCode);
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_change_code)
                .setView(form)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.lock_save_code, null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    char[] previous = chars(old);
                    char[] proposed = chars(next);
                    char[] repeated = chars(confirm);
                    if (!PasscodeHash.valid(previous) || !PasscodeHash.valid(proposed)) {
                        old.setError(getString(R.string.lock_six_digits));
                    } else if (!Arrays.equals(proposed, repeated)) {
                        confirm.setError(getString(R.string.lock_code_mismatch));
                    } else {
                        old.setText(""); next.setText(""); confirm.setText("");
                        dialog.dismiss();
                        char[] oldCode = previous;
                        char[] newCode = proposed;
                        runCodeTask(uid, () -> AppLockStore.change(app, uid, oldCode, newCode),
                                oldCode, R.string.lock_code_changed, newCode);
                        proposed = null;
                        previous = null;
                    }
                    if (previous != null) Arrays.fill(previous, '\0');
                    if (proposed != null) Arrays.fill(proposed, '\0');
                    Arrays.fill(repeated, '\0');
                }));
        dialog.show();
    }

    private interface CodeTask { AppLockStore.Result run(); }

    private void runCodeTask(String uid, CodeTask task, char[] first, int successMessage,
                             char[]... others) {
        Activity host = requireActivity();
        lockWorker.execute(() -> {
            AppLockStore.Result result;
            try { result = uid.equals(currentUid()) ? task.run() : AppLockStore.Result.WRONG; }
            catch (RuntimeException error) { result = AppLockStore.Result.WRONG; }
            finally {
                Arrays.fill(first, '\0');
                for (char[] code : others) Arrays.fill(code, '\0');
            }
            final AppLockStore.Result checked = result;
            host.runOnUiThread(() -> {
                if (!isAdded()) return;
                int message = checked == AppLockStore.Result.MATCH ? successMessage
                        : checked == AppLockStore.Result.COOLDOWN ? R.string.lock_cooldown
                        : R.string.lock_wrong_code;
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                refreshValues(requireView());
            });
        });
    }

    private static char[] chars(TextInputEditText input) {
        Editable text = input.getText();
        return text == null ? new char[0] : text.toString().toCharArray();
    }

    private void clearPendingCode() {
        if (pendingCode != null) Arrays.fill(pendingCode, '\0');
        pendingCode = null;
        pendingUid = null;
    }

    @Override
    public void onDestroy() {
        clearPendingCode();
        lockWorker.shutdown();
        super.onDestroy();
    }

    private void showThemeDialog() {
        int current = AppSettings.themeMode(requireContext());
        int index = current == AppCompatDelegate.MODE_NIGHT_NO ? 1
                : current == AppCompatDelegate.MODE_NIGHT_YES ? 2 : 0;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Appearance")
                .setSingleChoiceItems(new String[]{"Follow device", "Light", "Dark"}, index,
                        (dialog, which) -> {
                            dialog.dismiss();
                            AppSettings.setThemeMode(requireContext(), THEME_MODES[which]);
                        })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
