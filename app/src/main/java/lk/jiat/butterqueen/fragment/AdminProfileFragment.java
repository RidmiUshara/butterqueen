package lk.jiat.butterqueen.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;

import java.util.HashMap;
import java.util.Map;

import lk.jiat.butterqueen.AppLockGate;
import lk.jiat.butterqueen.R;
import lk.jiat.butterqueen.activity.SignInActivity;
import lk.jiat.butterqueen.notification.PushTokenRegistrar;

/** Separate admin profile; customer profiles continue to use ProfileFragment. */
public final class AdminProfileFragment extends Fragment {
    private TextView displayName, displayEmail;
    private TextInputEditText firstName, lastName;
    private MaterialButton saveButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup parent, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_profile, parent, false);
        displayName = view.findViewById(R.id.adminProfileName);
        displayEmail = view.findViewById(R.id.adminProfileEmail);
        firstName = view.findViewById(R.id.adminFirstName);
        lastName = view.findViewById(R.id.adminLastName);
        saveButton = view.findViewById(R.id.adminSaveProfile);
        saveButton.setOnClickListener(v -> saveProfile());
        view.findViewById(R.id.adminLogout).setOnClickListener(v -> confirmLogout());
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        loadProfile();
    }

    private void loadProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            signOut();
            return;
        }
        displayEmail.setText(user.getEmail() == null ? "" : user.getEmail());
        FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                .get(Source.DEFAULT)
                .addOnSuccessListener(doc -> {
                    if (!isAdded() || getView() == null) return;
                    if (!"admin".equals(doc.getString("role"))) {
                        Toast.makeText(requireContext(), "Admin access required",
                                Toast.LENGTH_LONG).show();
                        signOut();
                        return;
                    }
                    String first = doc.getString("firstName");
                    String last = doc.getString("lastName");
                    firstName.setText(first == null ? "" : first);
                    lastName.setText(last == null ? "" : last);
                    String combined = ((first == null ? "" : first) + " "
                            + (last == null ? "" : last)).trim();
                    displayName.setText(combined.isEmpty() ? "Bakery Administrator" : combined);
                })
                .addOnFailureListener(error -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Unable to load profile",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            signOut();
            return;
        }
        String first = String.valueOf(firstName.getText()).trim();
        String last = String.valueOf(lastName.getText()).trim();
        if (first.isEmpty()) {
            firstName.setError("First name is required");
            return;
        }
        if (last.isEmpty()) {
            lastName.setError("Last name is required");
            return;
        }
        saveButton.setEnabled(false);
        saveButton.setText("Saving...");
        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", first);
        updates.put("lastName", last);
        FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                .update(updates)
                .addOnSuccessListener(unused -> {
                    if (!isAdded()) return;
                    displayName.setText(first + " " + last);
                    resetSaveButton();
                    Toast.makeText(requireContext(), "Profile updated",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(error -> {
                    if (!isAdded()) return;
                    resetSaveButton();
                    Toast.makeText(requireContext(), "Save failed: " + error.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void resetSaveButton() {
        saveButton.setEnabled(true);
        saveButton.setText("Save changes");
    }

    private void confirmLogout() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Sign out of Bakery Studio?")
                .setMessage("You will return to the sign-in screen.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Sign out", (dialog, which) -> signOut())
                .show();
    }

    private void signOut() {
        if (!isAdded()) return;
        AppLockGate.lockNow();
        PushTokenRegistrar.unregister(requireContext());
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(requireContext(), SignInActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
