package app.example.capriscan;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class OTPVerificationActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private ProgressBar progressBar;
    private TextView timerTextView;
    private static final long VERIFICATION_TIMEOUT = 30000; // 30 seconds
    private CountDownTimer countDownTimer;
    private Handler handler;
    private boolean isVerifying;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otpverification);

        auth = FirebaseAuth.getInstance();
        progressBar = findViewById(R.id.login_progress_bar);
        timerTextView = findViewById(R.id.timer_textview);
        handler = new Handler();
        isVerifying = true;

        startVerificationTimer();
    }

    private void startVerificationTimer() {
        countDownTimer = new CountDownTimer(VERIFICATION_TIMEOUT, 1000) {
            public void onTick(long millisUntilFinished) {
                timerTextView.setText("Time remaining: " + millisUntilFinished / 1000 + " seconds");
            }

            public void onFinish() {
                isVerifying = false;
                timerTextView.setText("Verification time expired");
                progressBar.setVisibility(View.GONE);
                Toast.makeText(OTPVerificationActivity.this, "Verification time expired. Please try again.", Toast.LENGTH_LONG).show();
                navigateToLogin();
            }
        }.start();

        // Start checking email verification
        checkEmailVerification();
    }

    private void checkEmailVerification() {
        if (!isVerifying) return;

        progressBar.setVisibility(View.VISIBLE);
        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            user.reload().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    if (user.isEmailVerified()) {
                        countDownTimer.cancel();
                        isVerifying = false;
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Email verified successfully!", Toast.LENGTH_SHORT).show();
                        navigateToLogin(); // Navigate to MainActivity upon successful verification
                    } else {
                        handler.postDelayed(this::checkEmailVerification, 3000); // Check every 3 seconds
                    }
                } else {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to verify email: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    navigateToLogin();
                }
            });
        } else {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "User not found. Please try signing up again.", Toast.LENGTH_SHORT).show();
            navigateToLogin();
        }
    }

    private void navigateToMain() {
        Intent intent = new Intent(OTPVerificationActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(OTPVerificationActivity.this, UserLoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isVerifying = false;
    }
}
