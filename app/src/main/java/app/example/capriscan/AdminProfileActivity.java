package app.example.capriscan;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class AdminProfileActivity extends AppCompatActivity {

    private TextView emailTextView;
    private Button logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_profile);

        emailTextView = findViewById(R.id.admin_email);
        logoutButton = findViewById(R.id.logout_button);

        emailTextView.setText("admin@gmail.com");

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Clear admin login status
                SharedPreferences prefs = getSharedPreferences("AdminPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.clear();
                editor.apply();

                // Exit the app completely
                finishAffinity(); // Closes all activities and exits the app
            }
        });
    }
}