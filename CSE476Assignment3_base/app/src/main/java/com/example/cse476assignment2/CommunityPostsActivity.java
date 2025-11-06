package com.example.cse476assignment2;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.exifinterface.media.ExifInterface;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class CommunityPostsActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_LOCATION_PERMISSION = 101;

    private static final List<Post> userPosts = new ArrayList<>();

    private ActivityResultLauncher<Intent> cameraXLauncher;
    private ActivityResultLauncher<String> galleryLauncher;

    private LinearLayout postPreviewContainer;
    private ImageView imagePreview;
    private EditText captionInput;
    private Button postButton;
    private RecyclerView postsRecyclerView;
    private PostAdapter postAdapter;
    private Uri pendingImageUri;
    private String pendingCaption;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_community_posts);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        ImageButton backButton = findViewById(R.id.backButton);
        Button addPostButton = findViewById(R.id.btnAddPost);
        postPreviewContainer = findViewById(R.id.postPreviewContainer);
        imagePreview = findViewById(R.id.imagePreview);
        captionInput = findViewById(R.id.etCaption);
        postButton = findViewById(R.id.btnPost);
        postsRecyclerView = findViewById(R.id.recyclerPosts);

        postsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        postAdapter = new PostAdapter(userPosts);
        postsRecyclerView.setAdapter(postAdapter);

        ensureDefaultPosts();
        postAdapter.notifyDataSetChanged();

        registerActivityResultLaunchers();

        backButton.setOnClickListener(v -> finish());
        addPostButton.setOnClickListener(v -> showImageSourceChooser());
        postButton.setOnClickListener(v -> publishPendingPost());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void registerActivityResultLaunchers() {
        cameraXLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            String photoPath = data.getStringExtra("photoUri");
                            String caption = data.getStringExtra("caption");
                            if (photoPath != null) {
                                showPostPreview(Uri.parse(photoPath), caption);
                            }
                        }
                    }
                }
        );

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        showPostPreview(uri, null);
                    }
                }
        );
    }

    private void showImageSourceChooser() {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();

        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            options.add(getString(R.string.add_post_option_camera));
            actions.add(() -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    openCameraXActivity();
                } else {
                    requestCameraPermission();
                }
            });
        }

        options.add(getString(R.string.add_post_option_gallery));
        actions.add(() -> galleryLauncher.launch("image/*"));

        new AlertDialog.Builder(this)
                .setTitle(R.string.add_post_title)
                .setItems(options.toArray(new String[0]), (dialog, which) -> actions.get(which).run())
                .show();
    }

    private void openCameraXActivity() {
        Intent intent = new Intent(this, CameraXActivity.class);
        cameraXLauncher.launch(intent);
    }

    private void publishPendingPost() {
        if (pendingImageUri == null) {
            Toast.makeText(this, R.string.no_image_selected, Toast.LENGTH_SHORT).show();
            return;
        }

        pendingCaption = captionInput.getText().toString();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fetchLocationAndPublish();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION
            );
        }
    }

    private void showPostPreview(Uri photoUri, String caption) {
        pendingImageUri = photoUri;

        Bitmap processedImage = processCapturedImage(photoUri);
        if (processedImage != null) {
            imagePreview.setImageBitmap(processedImage);
        } else {
            imagePreview.setImageURI(photoUri);
        }

        imagePreview.setVisibility(View.VISIBLE);
        captionInput.setText(caption != null ? caption : "");
        captionInput.setVisibility(View.VISIBLE);
        postButton.setVisibility(View.VISIBLE);
        postPreviewContainer.setVisibility(View.VISIBLE);
    }

    private void hidePostPreview() {
        postPreviewContainer.setVisibility(View.GONE);
        imagePreview.setVisibility(View.GONE);
        captionInput.setVisibility(View.GONE);
        postButton.setVisibility(View.GONE);
        captionInput.setText("");
        pendingImageUri = null;
        pendingCaption = null;
    }

    private void addPostToFeed(Uri imageUri, String caption, String location) {
        Post newPost = new Post(imageUri, caption, getString(R.string.you_as_user), location);
        userPosts.add(0, newPost);
        postAdapter.notifyItemInserted(0);
        postsRecyclerView.scrollToPosition(0);
    }

    private void ensureDefaultPosts() {
        if (!userPosts.isEmpty()) {
            return;
        }

        userPosts.add(new Post(
                R.drawable.squirrel_post,
                getString(R.string.post_caption_1),
                getString(R.string.tenth_place_name),
                getString(R.string.post_location_beal_gardens)
        ));
        userPosts.add(new Post(
                R.drawable.online_class,
                getString(R.string.post_caption_2),
                getString(R.string.first_place_name),
                getString(R.string.post_location_online)
        ));
        userPosts.add(new Post(
                R.drawable.beaumont_tower,
                getString(R.string.post_caption_3),
                getString(R.string.fifth_place_name),
                getString(R.string.post_location_beaumont)
        ));
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCameraXActivity();
            } else {
                boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA);
                if (!showRationale) {
                    showPermissionDeniedDialog();
                } else {
                    Toast.makeText(this, R.string.camera_permission_denied, Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocationAndPublish();
            } else {
                completePendingPost(getString(R.string.post_location_permission_denied));
            }
        }
    }

    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.camera_permission_needed_title)
                .setMessage(R.string.camera_permission_needed_message)
                .setPositiveButton(R.string.open_settings, (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }

    private Bitmap processCapturedImage(Uri photoUri) {
        try {
            InputStream input = getContentResolver().openInputStream(photoUri);
            if (input == null) {
                return null;
            }
            Bitmap original = BitmapFactory.decodeStream(input);
            input.close();

            input = getContentResolver().openInputStream(photoUri);
            if (input == null) {
                return original;
            }
            ExifInterface exif = new ExifInterface(input);
            input.close();
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            Matrix matrix = new Matrix();
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                matrix.postRotate(90);
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                matrix.postRotate(180);
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                matrix.postRotate(270);
            }

            Bitmap rotated = Bitmap.createBitmap(original, 0, 0, original.getWidth(), original.getHeight(), matrix, true);

            int maxDim = 800;
            float scale = Math.min((float) maxDim / rotated.getWidth(), (float) maxDim / rotated.getHeight());
            int newWidth = Math.round(rotated.getWidth() * scale);
            int newHeight = Math.round(rotated.getHeight() * scale);
            return Bitmap.createScaledBitmap(rotated, newWidth, newHeight, true);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void fetchLocationAndPublish() {
        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellationTokenSource.getToken())
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        completePendingPost(formatLocation(location));
                    } else {
                        fetchLastKnownLocation();
                    }
                })
                .addOnFailureListener(e -> fetchLastKnownLocation());
    }

    private void fetchLastKnownLocation() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(lastLocation -> {
                    if (lastLocation != null) {
                        completePendingPost(formatLocation(lastLocation));
                    } else {
                        completePendingPost(getString(R.string.post_location_unavailable));
                    }
                })
                .addOnFailureListener(e -> completePendingPost(getString(R.string.post_location_unavailable)));
    }

    private String formatLocation(Location location) {
        return getString(
                R.string.post_location_format,
                location.getLatitude(),
                location.getLongitude()
        );
    }

    private void completePendingPost(String locationText) {
        if (pendingImageUri == null) {
            return;
        }

        addPostToFeed(pendingImageUri, pendingCaption != null ? pendingCaption : "", locationText);
        Toast.makeText(this, R.string.post_success_message, Toast.LENGTH_SHORT).show();
        hidePostPreview();
        pendingCaption = null;
    }
}
