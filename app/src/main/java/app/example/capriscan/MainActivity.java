package app.example.capriscan;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import android.widget.Toast;

import app.example.capriscan.fragments.CommunityPage;
import app.example.capriscan.fragments.DetectionPages;
import app.example.capriscan.fragments.HomePage;
import app.example.capriscan.fragments.AdminAnnouncementPage;
import app.example.capriscan.fragments.AboutUs;

public class MainActivity extends AppCompatActivity {

    private long backPressedTime;
    private Toast backToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(navListener);

        // Default fragment (HomePage)
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new HomePage()).commit();
    }

    private final NavigationBarView.OnItemSelectedListener navListener =
            item -> {
                int itemId = item.getItemId();
                Fragment selectedFragment = null;

                // Handle navigation item clicks
                if (itemId == R.id.nav_home) {
                    selectedFragment = new HomePage();
                } else if (itemId == R.id.nav_detectionPage) {
                    selectedFragment = new DetectionPages();  // Use DetectionPages (or modify later)
                } else if (itemId == R.id.nav_community_page) {
                    selectedFragment = new CommunityPage();
                } else if (itemId == R.id.nav_annoucement_page) {
                    selectedFragment = new AdminAnnouncementPage();
                } else if (itemId == R.id.nav_about_us) {
                    selectedFragment = new AboutUs();
                }

                // Replace the current fragment with the selected one
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selectedFragment).commit();
                return true;
            };

    @Override
    public void onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            backToast.cancel();
            super.onBackPressed();
            return;
        } else {
            backToast = Toast.makeText(getBaseContext(), "Press back again to exit", Toast.LENGTH_SHORT);
            backToast.show();
        }
        backPressedTime = System.currentTimeMillis();
    }
}
