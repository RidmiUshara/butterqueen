package lk.jiat.butterqueen.fragment;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import lk.jiat.butterqueen.R;
import lk.jiat.butterqueen.activity.SignInActivity;

public class ProfileFragment extends Fragment {

    private ImageView profileImageView;
    private TextView profileNameText;
    private TextView profileEmailText;
    private TextInputEditText firstNameProfileEditText;
    private TextInputEditText lastNameProfileEditText;
    private TextInputEditText emailProfileEditText;
    private TextInputEditText birthdayProfileEditText;
    private MaterialButton changePhotoButton;
    private MaterialButton updateProfileButton;
    private MaterialButton deleteAccountButton;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseStorage firebaseStorage;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    uploadProfileImage(uri);
                } else {
                    Toast.makeText(requireContext(), "No image selected", Toast.LENGTH_SHORT).show();
                }
            });

    public ProfileFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        profileImageView = view.findViewById(R.id.profileImageView);
        profileNameText = view.findViewById(R.id.profileNameText);
        profileEmailText = view.findViewById(R.id.profileEmailText);
        firstNameProfileEditText = view.findViewById(R.id.firstNameProfileEditText);
        lastNameProfileEditText = view.findViewById(R.id.lastNameProfileEditText);
        emailProfileEditText = view.findViewById(R.id.emailProfileEditText);
        birthdayProfileEditText = view.findViewById(R.id.birthdayProfileEditText);
        changePhotoButton = view.findViewById(R.id.changePhotoButton);
        updateProfileButton = view.findViewById(R.id.updateProfileButton);
        deleteAccountButton = view.findViewById(R.id.deleteAccountButton);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();

        listenToUserProfile();

        birthdayProfileEditText.setOnClickListener(v -> showDatePicker());

        changePhotoButton.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        updateProfileButton.setOnClickListener(v -> updateProfile());

        deleteAccountButton.setOnClickListener(v -> showDeleteAccountDialog());

        return view;
    }

    private void listenToUserProfile() {
        if (firebaseAuth.getCurrentUser() == null) {
            return;
        }

        String userId = firebaseAuth.getCurrentUser().getUid();

        firebaseFirestore.collection("users")
                .document(userId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (!isAdded()) {
                        return;
                    }

                    if (error != null) {
                        Toast.makeText(requireContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String firstName = documentSnapshot.getString("firstName");
                        String lastName = documentSnapshot.getString("lastName");
                        String email = documentSnapshot.getString("email");
                        String birthday = documentSnapshot.getString("birthday");
                        String imageUrl = documentSnapshot.getString("profileImageUrl");

                        String fullName = ((firstName != null ? firstName : "") + " " +
                                (lastName != null ? lastName : "")).trim();

                        if (fullName.isEmpty()) {
                            fullName = "ButterQueen User";
                        }

                        profileNameText.setText(fullName);
                        profileEmailText.setText(email != null ? email : "");

                        firstNameProfileEditText.setText(firstName != null ? firstName : "");
                        lastNameProfileEditText.setText(lastName != null ? lastName : "");
                        emailProfileEditText.setText(email != null ? email : "");
                        birthdayProfileEditText.setText(birthday != null ? birthday : "");

                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.account_circle_24px)
                                    .error(R.drawable.account_circle_24px)
                                    .into(profileImageView);
                        } else {
                            profileImageView.setImageResource(R.drawable.account_circle_24px);
                        }
                    }
                });
    }

    private void updateProfile() {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String firstName = String.valueOf(firstNameProfileEditText.getText()).trim();
        String lastName = String.valueOf(lastNameProfileEditText.getText()).trim();
        String birthday = String.valueOf(birthdayProfileEditText.getText()).trim();

        if (TextUtils.isEmpty(firstName)) {
            firstNameProfileEditText.setError("Enter first name");
            firstNameProfileEditText.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(lastName)) {
            lastNameProfileEditText.setError("Enter last name");
            lastNameProfileEditText.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(birthday)) {
            birthdayProfileEditText.setError("Select birthday");
            birthdayProfileEditText.requestFocus();
            return;
        }

        updateProfileButton.setEnabled(false);
        updateProfileButton.setText("Updating...");

        String userId = firebaseAuth.getCurrentUser().getUid();

        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", firstName);
        updates.put("lastName", lastName);
        updates.put("birthday", birthday);

        firebaseFirestore.collection("users")
                .document(userId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    updateProfileButton.setEnabled(true);
                    updateProfileButton.setText("Update Profile");
                    Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    updateProfileButton.setEnabled(true);
                    updateProfileButton.setText("Update Profile");
                    Toast.makeText(requireContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void uploadProfileImage(Uri imageUri) {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = firebaseAuth.getCurrentUser().getUid();

        StorageReference imageRef = firebaseStorage.getReference()
                .child("profile_images")
                .child(userId + ".jpg");

        changePhotoButton.setEnabled(false);
        changePhotoButton.setText("Uploading...");

        imageRef.putFile(imageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return imageRef.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> {
                    firebaseFirestore.collection("users")
                            .document(userId)
                            .update("profileImageUrl", downloadUri.toString())
                            .addOnSuccessListener(unused -> {
                                changePhotoButton.setEnabled(true);
                                changePhotoButton.setText("Change Photo");
                                Toast.makeText(requireContext(), "Profile photo updated", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                changePhotoButton.setEnabled(true);
                                changePhotoButton.setText("Change Photo");
                                Toast.makeText(requireContext(), "Failed to save image URL: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    changePhotoButton.setEnabled(true);
                    changePhotoButton.setText("Change Photo");
                    Toast.makeText(requireContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to permanently delete your account?")
                .setPositiveButton("Delete", (dialog, which) -> deleteAccount())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void deleteAccount() {
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = firebaseAuth.getCurrentUser().getUid();

        firebaseAuth.getCurrentUser()
                .delete()
                .addOnSuccessListener(unused -> {
                    firebaseFirestore.collection("users")
                            .document(userId)
                            .delete();

                    firebaseAuth.signOut();

                    Toast.makeText(requireContext(), "Account deleted", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(requireContext(), SignInActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                })
                .addOnFailureListener(e -> Toast.makeText(
                        requireContext(),
                        "Delete failed. Please log in again and retry.",
                        Toast.LENGTH_LONG
                ).show());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String date = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                    birthdayProfileEditText.setText(date);
                    birthdayProfileEditText.setError(null);
                },
                year, month, day
        );

        datePickerDialog.show();
    }
}