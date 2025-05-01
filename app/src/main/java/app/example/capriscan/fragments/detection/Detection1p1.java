package app.example.capriscan.fragments.detection;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import app.example.capriscan.R;
import app.example.capriscan.fragments.DetectionPages;
import app.example.capriscan.ml.Model;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Detection1p1 extends Fragment {

    private ImageView imageView;
    private TextView capriNameTextView, qualityTextView, firstAidTextView, pinkEyeInfoTextView;
    private int imageSize = 32;
    private Button goToVetFinderButton, backButton;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;

    // Updated first aid information for two classes
    private final Map<String, String> firstAidInfo = new HashMap<String, String>() {{
        put("Orf", "Rinse eyes with saline solution and isolate if possible.");
        put("Healthy", "No issues detected. Continue providing regular care and a healthy diet.");
    }};

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        Bitmap image = (Bitmap) data.getExtras().get("data");

                        // 1. Display the thumbnail in ImageView
                        imageView.setImageBitmap(image);

                        // 2. Save full-quality image to a temporary file
                        try {
                            File tempFile = File.createTempFile("camera_temp", ".jpg", requireContext().getCacheDir());
                            try (FileOutputStream out = new FileOutputStream(tempFile)) {
                                image.compress(Bitmap.CompressFormat.JPEG, 90, out); // 90% quality
                            }

                            // 3. Pass the file Uri to classifyImage
                            classifyImage(Uri.fromFile(tempFile));

                        } catch (IOException e) {
                            Toast.makeText(requireContext(), "Failed to process image", Toast.LENGTH_SHORT).show();
                            Log.e("CameraError", "Failed to save temp file", e);
                        }
                    }
                });
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        Uri imageUri = data.getData();
                        imageView.setImageURI(imageUri);  // Display selected image
                        classifyImage(imageUri);  // Pass Uri directly
                    }
                }
        );
    }

    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout that now has SwipeRefreshLayout as the root container.
        View view = inflater.inflate(R.layout.detection1, container, false);

        // Initialize views from the layout
        imageView = view.findViewById(R.id.imageView);
        capriNameTextView = view.findViewById(R.id.capriNameTextView);
        qualityTextView = view.findViewById(R.id.qualityTextView);
        firstAidTextView = view.findViewById(R.id.firstAidTextView);
        goToVetFinderButton = view.findViewById(R.id.goToVetFinderButton);
        progressBar = view.findViewById(R.id.progressBar);
        backButton = view.findViewById(R.id.backButton);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        pinkEyeInfoTextView = view.findViewById(R.id.pinkEyeInfoTextView);

        // Back button functionality
        backButton.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new DetectionPages())
                    .addToBackStack(null)
                    .commit();
        });

        // Set up "Find Nearby Vets" button
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

        // Set up SwipeRefreshLayout for pull-to-refresh functionality
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // Reset UI elements for a fresh scan
            imageView.setImageResource(0);
            capriNameTextView.setText("");
            qualityTextView.setText("");
            firstAidTextView.setText("");
            pinkEyeInfoTextView.setText(""); // Reset the info text
            progressBar.setVisibility(View.GONE);
            goToVetFinderButton.setVisibility(View.GONE);

            // Re-open the image source selection dialog to allow a new scan
            selectImageSource();

            // Stop the refresh animation
            swipeRefreshLayout.setRefreshing(false);
        });

        // Open the image source selection dialog on initial load
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
                // 1. Get original image file from Uri (without Bitmap conversion)
                InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
                byte[] imageBytes = new byte[inputStream.available()];
                inputStream.read(imageBytes);
                inputStream.close();

                // 2. Use OkHttp for reliable multipart upload
                OkHttpClient client = new OkHttpClient();

                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart(
                                "file",
                                "upload.jpg",  // Filename (match your cURL)
                                RequestBody.create(
                                        MediaType.parse("image/jpeg"),
                                        imageBytes
                                )
                        )
                        .build();

                Request request = new Request.Builder()
                        .url("https://suddenly-clean-firefly.ngrok-free.app/predict/orf")  // Use 10.0.2.2 for Android emulator
                        .post(requestBody)
                        .build();

                // 3. Execute the request
                Response response = client.newCall(request).execute();
                String responseData = response.body().string();

                // 4. Update UI with the response
                requireActivity().runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(responseData);
                        String className = json.getString("class");
                        String confidence = json.getString("confidence");



                        capriNameTextView.setText(className);
                        qualityTextView.setText("Confidence: " + confidence);

                        // Show first-aid tips based on detection
                        String aidInfo = firstAidInfo.get(className);
                        if (aidInfo != null) {
                            firstAidTextView.setText(aidInfo);
                            firstAidTextView.setVisibility(View.VISIBLE);
                        } else {
                            firstAidTextView.setVisibility(View.GONE);
                        }

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