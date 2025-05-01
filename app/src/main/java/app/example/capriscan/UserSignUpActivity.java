package app.example.capriscan;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class UserSignUpActivity extends AppCompatActivity {

    private MaterialButton signUpButton;
    private ProgressBar progressBar;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private TextInputEditText nameInput, emailInput, passwordInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_sign_up);

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Set system bar (status bar) color to green
        Window window = getWindow();
        if (window != null) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimary));
        }

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // Initialize UI elements
        signUpButton = findViewById(R.id.sign_up_button);
        progressBar = findViewById(R.id.progress_bar);
        nameInput = findViewById(R.id.name_input);
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);

        // Sign-up button click listener
        if (signUpButton != null) {
            signUpButton.setOnClickListener(v -> {
                String name = nameInput.getText() != null ? nameInput.getText().toString().trim() : "";
                String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
                String password = passwordInput.getText() != null ? passwordInput.getText().toString().trim() : "";

                // Validate inputs
                if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                    showToast("Please fill in all fields", false);
                    return;
                }

                setLoadingState(true);
                createAccount(name, email, password);
            });
        }

        // Find the login link TextView
        TextView loginLink = findViewById(R.id.login_link);
        if (loginLink != null) {
            loginLink.setOnClickListener(v -> {
                Intent intent = new Intent(UserSignUpActivity.this, UserLoginActivity.class);
                startActivity(intent);
                finish();
            });
        }
    }

    private void createAccount(String name, String email, String password) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            user.sendEmailVerification()
                                    .addOnCompleteListener(verificationTask -> {
                                        setLoadingState(false);
                                        if (verificationTask.isSuccessful()) {
                                            showToast("Verification email sent. Please check your inbox.", true);
                                            saveUserToFirestore(user, name, email);

                                            // Delay starting OTPVerificationActivity to ensure email is sent
                                            new android.os.Handler().postDelayed(() -> {
                                                Intent intent = new Intent(UserSignUpActivity.this, OTPVerificationActivity.class);
                                                intent.putExtra("email", email);
                                                intent.putExtra("name", name);
                                                startActivity(intent);
                                                finish();
                                            }, 1000); // 1 second delay for smoother transition
                                        } else {
                                            showToast("Failed to send verification email.", false);
                                            user.delete(); // Delete the user if email verification fails
                                        }
                                    });
                        } else {
                            setLoadingState(false);
                            showToast("Failed to retrieve user.", false);
                        }
                    } else {
                        setLoadingState(false);
                        showToast("Failed to create user: " + task.getException().getMessage(), false);
                    }
                });
    }

    private void saveUserToFirestore(FirebaseUser user, String name, String email) {
        String userId = user.getUid();
        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", userId);
        userData.put("name", name);
        userData.put("email", email);

        db.collection("users").document(userId).set(userData)
                .addOnSuccessListener(aVoid -> {
                    // User data saved successfully
                })
                .addOnFailureListener(e -> {
                    showToast("Failed to save user data: " + e.getMessage(), false);
                });
    }

    private void setLoadingState(boolean isLoading) {
        if (signUpButton != null && progressBar != null) {
            if (isLoading) {
                signUpButton.setVisibility(View.INVISIBLE);
                progressBar.setVisibility(View.VISIBLE);
            } else {
                progressBar.setVisibility(View.GONE);
                signUpButton.setVisibility(View.VISIBLE);
            }
        }
    }

    private void showToast(String message, boolean isSuccess) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        View toastView = toast.getView();
        if (toastView != null) {
            toastView.setBackgroundResource(isSuccess ? R.drawable.toast_success_background : R.drawable.toast_error_background);
            TextView toastText = toastView.findViewById(android.R.id.message);
            if (toastText != null) {
                toastText.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            }
        }
        toast.show();
    }
}
