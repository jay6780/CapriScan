package app.example.capriscan;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class UserLoginActivity extends AppCompatActivity {

    private MaterialButton loginButton;
    private ProgressBar progressBar;
    private FirebaseAuth auth;
    private TextView forgotPasswordLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_login);

        auth = FirebaseAuth.getInstance();

        // Set up status bar color
        Window window = getWindow();
        if (window != null) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimary));
        }

        loginButton = findViewById(R.id.login_button);
        progressBar = findViewById(R.id.progress_bar);
        TextInputEditText emailInput = findViewById(R.id.email_input);
        TextInputEditText passwordInput = findViewById(R.id.password_input);
        forgotPasswordLink = findViewById(R.id.forgot_password_link);

        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
            String password = passwordInput.getText() != null ? passwordInput.getText().toString().trim() : "";

            if (email.isEmpty() || password.isEmpty()) {
                showToast("Please fill in all fields", false);
                return;
            }

            setLoadingState(true);
            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        setLoadingState(false);

                        if (task.isSuccessful()) {
                            FirebaseUser user = auth.getCurrentUser();
                            if (user != null) {
                                // Reload user data to check for email verification
                                user.reload().addOnCompleteListener(reloadTask -> {
                                    if (reloadTask.isSuccessful()) {
                                        // Check if the user is verified every time they log in
                                        if (user.isEmailVerified()) {
                                            showToast("Login successful", true);
                                            Intent intent = new Intent(UserLoginActivity.this, MainActivity.class);
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            showToast("Please verify your email.", false);
                                            Intent intent = new Intent(UserLoginActivity.this, OTPVerificationActivity.class);
                                            intent.putExtra("email", email);
                                            startActivity(intent);
                                        }
                                    } else {
                                        showToast("Failed to reload user. Try again.", false);
                                    }
                                });
                            }
                        } else {
                            showToast("Login failed: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"), false);
                        }
                    });
        });

        forgotPasswordLink.setOnClickListener(v -> promptPasswordReset());

        TextView signupLink = findViewById(R.id.signup_link);
        signupLink.setOnClickListener(v -> {
            Intent intent = new Intent(UserLoginActivity.this, UserSignUpActivity.class);
            startActivity(intent);
        });
    }

    private void promptPasswordReset() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password");

        // Create a custom layout for the dialog
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_reset_password, null);
        builder.setView(dialogView);

        final TextInputEditText emailInput = dialogView.findViewById(R.id.dialog_email_input);
        emailInput.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailInput.setHint("Enter your email");

        builder.setPositiveButton("Send", (dialog, which) -> {
            String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
            if (!email.isEmpty()) {
                sendPasswordResetEmail(email);
            } else {
                showToast("Please enter your email.", false);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();

        // Customize the dialog buttons
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.dark_red));
    }

    private void sendPasswordResetEmail(String email) {
        setLoadingState(true);
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    setLoadingState(false);
                    if (task.isSuccessful()) {
                        showToast("Password reset email sent.", true);
                    } else {
                        showToast("Failed to send reset email: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"), false);
                    }
                });
    }

    private void setLoadingState(boolean isLoading) {
        if (loginButton != null && progressBar != null) {
            if (isLoading) {
                loginButton.setVisibility(View.INVISIBLE);
                progressBar.setVisibility(View.VISIBLE);
            } else {
                progressBar.setVisibility(View.GONE);
                loginButton.setVisibility(View.VISIBLE);
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
