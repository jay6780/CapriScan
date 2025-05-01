package app.example.capriscan.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import app.example.capriscan.R;
import app.example.capriscan.fragments.detection.Detection1;
import app.example.capriscan.fragments.detection.Detection1p1;
import app.example.capriscan.fragments.detection.Detection1p2;
import app.example.capriscan.fragments.detection.Detection2;
import app.example.capriscan.fragments.detection.Detection3;
import app.example.capriscan.fragments.detection.Detection4;
import app.example.capriscan.fragments.detection.Detection5;

public class DetectionPages extends Fragment {

    private Button scanHeadButton;
    private Button scanHead2Button;
    private Button scanHead3Button;
    private Button scanBodyButton;
    private Button scanKneeButton;
    private Button scanHoofButton;
    private Button scanStoolButton;

    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_detection_page, container, false);

        scanHeadButton = view.findViewById(R.id.scanHeadButton);
        scanHead2Button = view.findViewById(R.id.scanHead2Button);
        scanHead3Button = view.findViewById(R.id.scanHead3Button);
        scanBodyButton = view.findViewById(R.id.scanBodyButton);
        scanKneeButton = view.findViewById(R.id.scanKneeButton);
        scanHoofButton = view.findViewById(R.id.scanHoofButton);
        scanStoolButton = view.findViewById(R.id.scanStoolButton);

        // Set onClick listeners for each button to open respective fragment
        scanHeadButton.setOnClickListener(v -> openFragment(new Detection1()));
        scanHead2Button.setOnClickListener(v -> openFragment(new Detection1p1()));
        scanHead3Button.setOnClickListener(v -> openFragment(new Detection1p2()));
        scanBodyButton.setOnClickListener(v -> openFragment(new Detection2()));
        scanKneeButton.setOnClickListener(v -> openFragment(new Detection3()));
        scanHoofButton.setOnClickListener(v -> openFragment(new Detection4()));
        scanStoolButton.setOnClickListener(v -> openFragment(new Detection5()));

        return view;
    }

    private void openFragment(Fragment fragment) {
        // Begin the fragment transaction
        getActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)  // Replace with the target fragment
                .addToBackStack(null)  // Add transaction to back stack (optional)
                .commit();  // Commit the transaction
    }
}
