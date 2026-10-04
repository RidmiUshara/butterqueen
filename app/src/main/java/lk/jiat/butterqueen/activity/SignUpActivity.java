package lk.jiat.butterqueen.activity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;

import lk.jiat.butterqueen.R;
import lk.jiat.butterqueen.model.User;

public class SignUpActivity extends AppCompatActivity {

    private static final String DEFAULT_PROFILE_IMAGE_URL =
            "https://openclipart.org/image/800px/346569";

    private TextInputEditText firstNameEditText;
    private TextInputEditText lastNameEditText;
    private TextInputEditText emailEditText;
    private TextInputEditText birthdayEditText;
    private TextInputEditText passwordEditText;
    private MaterialButton signUpButton;
    private TextView signInText;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        firstNameEditText = findViewById(R.id.firstNameEditText);
        lastNameEditText = findViewById(R.id.lastNameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        birthdayEditText = findViewById(R.id.birthdayEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        signUpButton = findViewById(R.id.signUpButton);
        signInText = findViewById(R.id.signInText);

        birthdayEditText.setOnClickListener(v -> showDatePicker());

        signUpButton.setOnClickListener(v -> signUpUser());

        signInText.setOnClickListener(v -> {
            Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void signUpUser() {
        String firstName = String.valueOf(firstNameEditText.getText()).trim();
        String lastName = String.valueOf(lastNameEditText.getText()).trim();
        String email = String.valueOf(emailEditText.getText()).trim();
        String birthday = String.valueOf(birthdayEditText.getText()).trim();
        String password = String.valueOf(passwordEditText.getText()).trim();

        if (firstName.isEmpty()) {
            firstNameEditText.setError("Enter first name");
            firstNameEditText.requestFocus();
            return;
        }

        if (lastName.isEmpty()) {
            lastNameEditText.setError("Enter last name");
            lastNameEditText.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            emailEditText.setError("Enter email");
            emailEditText.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Enter a valid email");
            emailEditText.requestFocus();
            return;
        }

        if (birthday.isEmpty()) {
            birthdayEditText.setError("Select birthday");
            birthdayEditText.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            passwordEditText.setError("Enter password");
            passwordEditText.requestFocus();
            return;
        }

        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            passwordEditText.requestFocus();
            return;
        }

        signUpButton.setEnabled(false);
        signUpButton.setText("Creating Account...");

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {

                        String userId = firebaseAuth.getCurrentUser() != null
                                ? firebaseAuth.getCurrentUser().getUid()
                                : null;

                        if (userId == null) {
                            signUpButton.setEnabled(true);
                            signUpButton.setText("Sign Up");
                            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        User user = new User(
                                userId,
                                firstName,
                                lastName,
                                email,
                                birthday,
                                "",
                                "",
                                DEFAULT_PROFILE_IMAGE_URL,
                                System.currentTimeMillis()
                        );

                        firebaseFirestore.collection("users")
                                .document(userId)
                                .set(user)
                                .addOnSuccessListener(unused -> {
                                    signUpButton.setEnabled(true);
                                    signUpButton.setText("Sign Up");

                                    Toast.makeText(this, "Account created successfully", Toast.LENGTH_SHORT).show();

                                    Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    signUpButton.setEnabled(true);
                                    signUpButton.setText("Sign Up");
                                    Toast.makeText(this, "Profile save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });

                    } else {
                        signUpButton.setEnabled(true);
                        signUpButton.setText("Sign Up");

                        String errorMessage = task.getException() != null
                                ? task.getException().getMessage()
                                : "Sign up failed";

                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                SignUpActivity.this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String date = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                    birthdayEditText.setText(date);
                    birthdayEditText.setError(null);
                },
                year, month, day
        );

        datePickerDialog.show();
    }
}