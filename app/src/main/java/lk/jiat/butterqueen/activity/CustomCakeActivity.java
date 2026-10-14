package lk.jiat.butterqueen.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import lk.jiat.butterqueen.R;

public class CustomCakeActivity extends AppCompatActivity {
    private EditText messageInput, dateInput, notesInput;
    private Spinner flavourInput, weightInput;
    private ImageView photoPreview;
    private Button submitButton, takePhotoButton, galleryButton, removePhotoButton;
    private byte[] photoBytes;

    private final ActivityResultLauncher<Void> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap != null) setPhoto(bitmap);
            });

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                try (java.io.InputStream stream = getContentResolver().openInputStream(uri)) {
                    if (stream == null) throw new java.io.IOException("Image unavailable");
                    android.graphics.Bitmap image = android.graphics.BitmapFactory.decodeStream(stream);
                    if (image == null) throw new java.io.IOException("Unsupported image");
                    setPhoto(image);
                } catch (Exception e) {
                    Toast.makeText(this, "Could not load the photo", Toast.LENGTH_LONG).show();
                }
            });

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_custom_cake);
        findViewById(R.id.customBack).setOnClickListener(v -> finish());
        messageInput = findViewById(R.id.customMessage);
        dateInput = findViewById(R.id.customDate);
        notesInput = findViewById(R.id.customNotes);
        flavourInput = findViewById(R.id.customFlavour);
        weightInput = findViewById(R.id.customWeight);
        photoPreview = findViewById(R.id.customPhotoPreview);
        submitButton = findViewById(R.id.customSubmit);
        takePhotoButton = findViewById(R.id.customTakePhoto);
        galleryButton = findViewById(R.id.customGallery);
        removePhotoButton = findViewById(R.id.customRemovePhoto);

        flavourInput.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Vanilla", "Chocolate", "Ribbon", "Red Velvet", "Other"}));
        weightInput.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"0.5 kg", "1 kg", "1.5 kg", "2 kg", "3 kg", "Other"}));
        dateInput.setOnClickListener(v -> {
            java.util.Calendar c = java.util.Calendar.getInstance();
            android.app.DatePickerDialog picker = new android.app.DatePickerDialog(this,
                    (view, year, month, day) -> dateInput.setText(String.format(java.util.Locale.US,
                            "%04d-%02d-%02d", year, month + 1, day)),
                    c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH),
                    c.get(java.util.Calendar.DAY_OF_MONTH));
            picker.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            picker.show();
        });
        takePhotoButton.setOnClickListener(v -> cameraLauncher.launch(null));
        galleryButton.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        removePhotoButton.setOnClickListener(v -> clearSelectedPhoto());
        submitButton.setOnClickListener(v -> submitRequest());
    }

    private void setPhoto(Bitmap bitmap) {
        // Reduce memory usage and upload a compact reference photo.
        int max = 1280;
        float scale = Math.min(1f, max / (float) Math.max(bitmap.getWidth(), bitmap.getHeight()));
        Bitmap sized = scale < 1 ? Bitmap.createScaledBitmap(bitmap,
                Math.max(1, Math.round(bitmap.getWidth() * scale)),
                Math.max(1, Math.round(bitmap.getHeight() * scale)), true) : bitmap;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        sized.compress(Bitmap.CompressFormat.JPEG, 80, out);
        photoBytes = out.toByteArray();
        photoPreview.setImageBitmap(sized);
        photoPreview.setVisibility(View.VISIBLE);
        removePhotoButton.setVisibility(View.VISIBLE);
    }

    // Removes the *selected* image before submission, not an image already uploaded to Firebase.
    private void clearSelectedPhoto() {
        if (!submitButton.isEnabled()) return;
        photoBytes = null;
        photoPreview.setImageDrawable(null);
        photoPreview.setVisibility(View.GONE);
        removePhotoButton.setVisibility(View.GONE);
        Toast.makeText(this, "Reference photo removed", Toast.LENGTH_SHORT).show();
    }

    private void submitRequest() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please sign in before sending a request", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, SignInActivity.class));
            return;
        }
        String date = dateInput.getText().toString().trim();
        if (date.isEmpty()) {
            dateInput.setError("Please choose a required date");
            return;
        }
        submitButton.setEnabled(false);
        submitButton.setText("Sending request...");
        // Prevent the selected image changing during an active upload.
        takePhotoButton.setEnabled(false);
        galleryButton.setEnabled(false);
        removePhotoButton.setEnabled(false);

        if (photoBytes != null) {
            String path = "custom_cake_requests/" + user.getUid() + "/" + UUID.randomUUID() + ".jpg";
            StorageReference ref = FirebaseStorage.getInstance().getReference().child(path);
            ref.putBytes(photoBytes).continueWithTask(task -> {
                        if (!task.isSuccessful()) throw task.getException() != null
                                ? task.getException() : new IllegalStateException("Upload failed");
                        return ref.getDownloadUrl();
                    }).addOnSuccessListener(url -> saveRequest(user, date, url.toString(), path))
                    .addOnFailureListener(error -> showFailure("Photo upload failed. Check Firebase Storage rules."));
        } else {
            saveRequest(user, date, "", "");
        }
    }

    private void saveRequest(FirebaseUser user, String date, String photoUrl, String photoPath) {
        Map<String, Object> request = new HashMap<>();
        request.put("userId", user.getUid());
        request.put("userEmail", user.getEmail() == null ? "" : user.getEmail());
        request.put("flavour", flavourInput.getSelectedItem().toString());
        request.put("weight", weightInput.getSelectedItem().toString());
        request.put("requiredDate", date);
        request.put("cakeMessage", messageInput.getText().toString().trim());
        request.put("notes", notesInput.getText().toString().trim());
        request.put("referenceImageUrl", photoUrl);
        request.put("referenceImagePath", photoPath);
        request.put("status", "pending");
        request.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
        FirebaseFirestore.getInstance().collection("customCakeRequests").add(request)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "Request sent! The bakery will review it.", Toast.LENGTH_LONG).show();
                    finish();
                }).addOnFailureListener(error -> showFailure("Could not submit request: " + error.getMessage()));
    }

    private void showFailure(String message) {
        submitButton.setEnabled(true);
        submitButton.setText("Submit Custom Cake Request");
        takePhotoButton.setEnabled(true);
        galleryButton.setEnabled(true);
        removePhotoButton.setEnabled(true);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
