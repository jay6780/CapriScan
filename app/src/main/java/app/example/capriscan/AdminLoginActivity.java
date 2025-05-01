package app.example.capriscan;

import android.content.Intent;
import android.content.SharedPreferences;
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

public class AdminLoginActivity extends AppCompatActivity {

    private MaterialButton loginButton;
    private ProgressBar progressBar;
    private static final String ADMIN_EMAIL = "admin@gmail.com";
    private static final String ADMIN_PASSWORD = "adminadmin";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_login);

        // Set system bar (status bar) color to green
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimary));

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI elements
        loginButton = findViewById(R.id.login_button);
        progressBar = findViewById(R.id.progress_bar);
        TextInputEditText emailInput = findViewById(R.id.email_input);
        TextInputEditText passwordInput = findViewById(R.id.password_input);

        // Login button click listener
        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
            String password = passwordInput.getText() != null ? passwordInput.getText().toString().trim() : "";

            // Validate inputs
            if (email.isEmpty() || password.isEmpty()) {
                showToast("Please fill in all fields", false);
                return;
            }

            // Show loading state
            setLoadingState(true);

            // Check if credentials match the static admin login
            if (email.equals(ADMIN_EMAIL) && password.equals(ADMIN_PASSWORD)) {
                // Login successful
                showToast("Admin login successful", true);

                // Set admin login status
                SharedPreferences prefs = getSharedPreferences("AdminPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("isLoggedIn", true);
                editor.apply();

                Intent intent = new Intent(AdminLoginActivity.this, AdminHomePage.class);
                startActivity(intent);
                finish(); // Close LoginActivity
            } else {
                // Login failed
                showToast("Login failed: Incorrect email or password", false);
                setLoadingState(false);
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