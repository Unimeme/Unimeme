package com.example.cse476assignment2;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CameraXActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "LOGIN_PREFS";
    private static final String KEY_USERNAME = "USERNAME";
    private static final String KEY_SHARE_LOCATION = "SHARE_LOCATION";
    public static final String KEY_LAST_POST_URI = "LAST_POST_URI";
    public static final String KEY_LAST_POST_LOCATION = "LAST_POST_LOCATION";
    public static final String KEY_LAST_POST_TIMESTAMP = "LAST_POST_TIMESTAMP";
    public static final String KEY_LAST_POST_USERNAME = "LAST_POST_USERNAME";
    private static final String STATE_PHOTO_URI = "state_photo_uri";
    private static final String STATE_HAS_PHOTO = "state_has_photo";
    private static final String STATE_LOCATION_TEXT = "state_location_text";

    private ImageView postImagePreview;
    private MaterialButton takePhotoButton;
    private TextView locationValueText;
    private View locationContainer;

    private Uri photoUri;
    private boolean hasPhoto;
    private boolean shareLocationEnabled;
    private String currentLocationText;
    private String username;

    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_x);

        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        // UPDATED: Use the correct key for location tracking preference
        shareLocationEnabled = getSharedPreferences("USER_PREFS_" + getLoggedInUser(), MODE_PRIVATE)
                .getBoolean("LOCATION_TRACKING", false);
        username = getIntent().getStringExtra("USERNAME");
        if (username == null || username.trim().isEmpty()) {
            username = sharedPreferences.getString(KEY_USERNAME, getString(R.string.user_default));
        }

        TextView postingAsText = findViewById(R.id.posting_as_text);
        postImagePreview = findViewById(R.id.post_image_preview);
        locationValueText = findViewById(R.id.location_value);
        takePhotoButton = findViewById(R.id.take_photo_button);
        MaterialButton uploadButton = findViewById(R.id.upload_post_button);
        MaterialButton cancelButton = findViewById(R.id.cancel_post_button);
        locationContainer = findViewById(R.id.location_container);

        postingAsText.setText(getString(R.string.create_post_posting_as, username));

        setupActivityResultLaunchers();

        takePhotoButton.setOnClickListener(v -> checkCameraPermissionAndLaunch());
        uploadButton.setOnClickListener(v -> finishAndReturnData());
        cancelButton.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        if (!shareLocationEnabled) {
            locationContainer.setVisibility(View.GONE);
            locationValueText.setText(R.string.create_post_location_disabled);
        } else {
            locationContainer.setVisibility(View.VISIBLE);
            locationValueText.setText(R.string.create_post_location_loading);
            requestLocationIfNeeded();
        }

        if (savedInstanceState != null) {
            restoreInstanceState(savedInstanceState);
        }
    }

    private String getLoggedInUser() {
        SharedPreferences loginPrefs = getSharedPreferences("LOGIN_PREFS", MODE_PRIVATE);
        return loginPrefs.getString("USERNAME", "default_user");
    }

    private void setupActivityResultLaunchers() {
        takePictureLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), result -> {
            if (Boolean.TRUE.equals(result)) {
                hasPhoto = true;
                if (photoUri != null) {
                    postImagePreview.setImageURI(photoUri);
                    postImagePreview.setContentDescription(getString(R.string.create_post_image_preview_content_description));
                }
                takePhotoButton.setText(R.string.create_post_retake_photo);
            } else {
                hasPhoto = false;
                photoUri = null;
                postImagePreview.setImageDrawable(null);
                postImagePreview.setContentDescription(getString(R.string.create_post_image_preview_content_description));
                takePhotoButton.setText(R.string.create_post_take_photo);
            }
        });

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (Boolean.TRUE.equals(isGranted)) {
                        launchCamera();
                    } else {
                        Toast.makeText(this, R.string.create_post_camera_permission_explanation, Toast.LENGTH_LONG).show();
                    }
                }
        );

        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean fineGranted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
                    boolean coarseGranted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                    if (fineGranted || coarseGranted) {
                        fetchLocation();
                    } else {
                        currentLocationText = getString(R.string.create_post_location_permission_explanation);
                        locationValueText.setText(currentLocationText);
                        Toast.makeText(this, R.string.create_post_location_permission_explanation, Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void checkCameraPermissionAndLaunch() {
        if (!isCameraAvailable()) {
            Toast.makeText(this, R.string.create_post_camera_unavailable, Toast.LENGTH_LONG).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        if (!isCameraAvailable()) {
            Toast.makeText(this, R.string.create_post_camera_unavailable, Toast.LENGTH_LONG).show();
            return;
        }

        try {
            File photoFile = createImageFile();
            photoUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", photoFile);
            takePictureLauncher.launch(photoUri);
        } catch (IOException e) {
            Toast.makeText(this, R.string.create_post_camera_error, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isCameraAvailable() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "POST_" + timeStamp;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir == null) {
            storageDir = getFilesDir();
        }
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void requestLocationIfNeeded() {
        if (!shareLocationEnabled) {
            return;
        }

        if (hasLocationPermission()) {
            fetchLocation();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void fetchLocation() {
        if (!hasLocationPermission()) {
            return;
        }

        if (!isLocationHardwareAvailable()) {
            currentLocationText = getString(R.string.create_post_location_hardware_unavailable);
            locationValueText.setText(currentLocationText);
            return;
        }

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager == null) {
            currentLocationText = getString(R.string.create_post_location_hardware_unavailable);
            locationValueText.setText(currentLocationText);
            return;
        }

        Location location = null;
        try {
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location == null) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
        } catch (SecurityException ignored) {
            currentLocationText = getString(R.string.create_post_location_permission_explanation);
            locationValueText.setText(currentLocationText);
            return;
        }

        if (location != null) {
            String latitude = String.format(Locale.getDefault(), "%.4f", location.getLatitude());
            String longitude = String.format(Locale.getDefault(), "%.4f", location.getLongitude());
            currentLocationText = getString(R.string.create_post_location_value, latitude, longitude);
        } else {
            currentLocationText = getString(R.string.create_post_location_unavailable);
        }
        locationValueText.setText(currentLocationText);
    }

    private boolean isLocationHardwareAvailable() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager == null) {
            return false;
        }

        try {
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (SecurityException ignored) {
            return false;
        }
    }

    private void finishAndReturnData() {
        if (!hasPhoto || photoUri == null) {
            Toast.makeText(this, R.string.create_post_no_photo_error, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent resultData = new Intent();
        resultData.putExtra("photoUri", photoUri.toString());

        String locationToReturn = "";
        if (shareLocationEnabled && currentLocationText != null) {
            if (!currentLocationText.equals(getString(R.string.create_post_location_loading)) &&
                    !currentLocationText.equals(getString(R.string.create_post_location_unavailable))) {
                locationToReturn = currentLocationText;
            }
        }
        resultData.putExtra("location", locationToReturn);

        setResult(RESULT_OK, resultData);
        finish();
    }


    private void restoreInstanceState(Bundle savedInstanceState) {
        String savedUri = savedInstanceState.getString(STATE_PHOTO_URI);
        hasPhoto = savedInstanceState.getBoolean(STATE_HAS_PHOTO, false);
        currentLocationText = savedInstanceState.getString(STATE_LOCATION_TEXT);

        if (savedUri != null) {
            photoUri = Uri.parse(savedUri);
            postImagePreview.setImageURI(photoUri);
            postImagePreview.setContentDescription(getString(R.string.create_post_image_preview_content_description));
            if (hasPhoto) {
                takePhotoButton.setText(R.string.create_post_retake_photo);
            }
        }

        if (currentLocationText != null && shareLocationEnabled) {
            locationValueText.setText(currentLocationText);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (photoUri != null) {
            outState.putString(STATE_PHOTO_URI, photoUri.toString());
        }
        outState.putBoolean(STATE_HAS_PHOTO, hasPhoto);
        if (currentLocationText != null) {
            outState.putString(STATE_LOCATION_TEXT, currentLocationText);
        }
    }
}