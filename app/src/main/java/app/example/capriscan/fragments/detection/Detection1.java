package app.example.capriscan.fragments.detection;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import app.example.capriscan.R;
import app.example.capriscan.fragments.DetectionPages;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Detection1 extends Fragment {

    private ImageView imageView;
    private TextView capriNameTextView, qualityTextView, firstAidTextView, pinkEyeInfoTextView;
    private int imageSize = 32;
    private Button goToVetFinderButton, backButton;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;

    private final Map<String, String> firstAidInfo = new HashMap<String, String>() {{
        put("Pink Eye", "Rinse eyes with saline solution and isolate if possible.");
        put("Healthy", "No issues detected. Continue providing regular care and a healthy diet.");
    }};

    private final Map<String, String> descriptionInfo = new HashMap<String, String>() {{
        put("Pink Eye", "Pink Eye is a contagious eye infection causing redness, swelling, and discharge.");
        put("Healthy", "The goat appears to be in good health with no visible signs of disease.");
    }};

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        Bitmap image = (Bitmap) data.getExtras().get("data");
                        imageView.setImageBitmap(image);
                        try {
                            File tempFile = File.createTempFile("camera_temp", ".jpg", requireContext().getCacheDir());
                            try (FileOutputStream out = new FileOutputStream(tempFile)) {
                                image.compress(Bitmap.CompressFormat.JPEG, 90, out);
                            }
                            classifyImage(Uri.fromFile(tempFile));
                        } catch (IOException e) {
                            Toast.makeText(requireContext(), "Failed to process image", Toast.LENGTH_SHORT).show();
                            Log.e("CameraError", "Failed to save temp file", e);
                        }
                    }
                });

        galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        Uri imageUri = data.getData();
                        imageView.setImageURI(imageUri);
                        classifyImage(imageUri);
                    }
                });
    }

    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.detection1, container, false);

        imageView = view.findViewById(R.id.imageView);
        capriNameTextView = view.findViewById(R.id.capriNameTextView);
        qualityTextView = view.findViewById(R.id.qualityTextView);
        firstAidTextView = view.findViewById(R.id.firstAidTextView);
        goToVetFinderButton = view.findViewById(R.id.goToVetFinderButton);
        progressBar = view.findViewById(R.id.progressBar);
        backButton = view.findViewById(R.id.backButton);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        pinkEyeInfoTextView = view.findViewById(R.id.pinkEyeInfoTextView);

        backButton.setOnClickListener(v -> requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new DetectionPages())
                .addToBackStack(null)
                .commit());

        goToVetFinderButton.setOnClickListener(v -> {
            Uri gmmIntentUri = Uri.parse("geo:0,0?q=vet");
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");

            if (mapIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                Toast.makeText(requireContext(), "Google Maps app is not available", Toast.LENGTH_SHORT).show();
            }
        });

        swipeRefreshLayout.setOnRefreshListener(() -> {
            imageView.setImageResource(0);
            capriNameTextView.setText("");
            qualityTextView.setText("");
            firstAidTextView.setText("");
            pinkEyeInfoTextView.setText("");
            progressBar.setVisibility(View.GONE);
            goToVetFinderButton.setVisibility(View.GONE);
            selectImageSource();
            swipeRefreshLayout.setRefreshing(false);
        });

        selectImageSource();
        return view;
    }

    private void selectImageSource() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Select Image Source")
                .setItems(new CharSequence[]{"Capture Image", "Upload Image"}, (dialog, which) -> {
                    if (which == 0) {
                        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            cameraLauncher.launch(cameraIntent);
                        } else {
                            requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
                        }
                    } else {
                        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        galleryLauncher.launch(galleryIntent);
                    }
                })
                .show();
    }

    private void classifyImage(Uri imageUri) {
        progressBar.setVisibility(View.VISIBLE);
        capriNameTextView.setText("Analyzing...");

        new Thread(() -> {
            try {
                InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
                byte[] imageBytes = new byte[inputStream.available()];
                inputStream.read(imageBytes);
                inputStream.close();

                OkHttpClient client = new OkHttpClient();

                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", "upload.jpg", RequestBody.create(MediaType.parse("image/jpeg"), imageBytes))
                        .build();

                Request request = new Request.Builder()
                        .url("https://suddenly-clean-firefly.ngrok-free.app/predict")
                        .post(requestBody)
                        .build();

                Response response = client.newCall(request).execute();
                String responseData = response.body().string();

                requireActivity().runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(responseData);
                        String className = json.getString("class");
                        String confidence = json.getString("confidence");

                        capriNameTextView.setText(className);
                        qualityTextView.setText("Confidence: " + confidence);

                        // Set First Aid
                        String aidInfo = firstAidInfo.get(className);
                        if (aidInfo != null) {
                            firstAidTextView.setText(aidInfo);
                            firstAidTextView.setVisibility(View.VISIBLE);
                        } else {
                            firstAidTextView.setVisibility(View.GONE);
                        }

                        // Set Description
                        String description = descriptionInfo.get(className);
                        if (description != null) {
                            pinkEyeInfoTextView.setText(description);
                            pinkEyeInfoTextView.setTextColor(getResources().getColor(android.R.color.black));
                            pinkEyeInfoTextView.setVisibility(View.VISIBLE);
                        } else {
                            pinkEyeInfoTextView.setVisibility(View.GONE);
                        }

                        goToVetFinderButton.setVisibility(View.VISIBLE);

                    } catch (Exception e) {
                        capriNameTextView.setText("Error parsing response");
                        Log.e("API_ERROR", e.getMessage());
                    }
                    progressBar.setVisibility(View.GONE);
                });

            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    capriNameTextView.setText("Error: " + e.getMessage());
                    progressBar.setVisibility(View.GONE);
                });
                Log.e("UPLOAD_ERROR", e.getMessage());
            }
        }).start();
    }
}
