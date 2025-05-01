package app.example.capriscan;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LogInAsActivity extends AppCompatActivity {

    private MaterialButton btnUser;
    private MaterialButton btnAdmin;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_as);

        mAuth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            if (currentUser.isEmailVerified()) {
                // User is logged in and verified, redirect to MainActivity
                Intent intent = new Intent(LogInAsActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            } else {
                // User is logged in but not verified, log them out and show message
                mAuth.signOut();
                showToast("Please verify your email before logging in.", false);
            }
            finish();
            return;
        }

        // Check if admin is logged in
        SharedPreferences prefs = getSharedPreferences("AdminPrefs", MODE_PRIVATE);
        boolean isAdminLoggedIn = prefs.getBoolean("isLoggedIn", false);
        if (isAdminLoggedIn) {
            Intent intent = new Intent(LogInAsActivity.this, AdminHomePage.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        initializeViews();

        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimary));

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnUser.setOnClickListener(v -> {
            Intent intent = new Intent(LogInAsActivity.this, UserLoginActivity.class);
            startActivity(intent);
        });

        btnAdmin.setOnClickListener(v -> {
            Intent intent = new Intent(LogInAsActivity.this, AdminLoginActivity.class);
            startActivity(intent);
        });
    }

    private void initializeViews() {
        btnUser = findViewById(R.id.btn_user);
        btnAdmin = findViewById(R.id.btn_admin);
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
