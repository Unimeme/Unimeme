package com.example.cse476assignment2;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ColorMatrixColorFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.InputStream;
import android.graphics.Matrix;
import androidx.exifinterface.media.ExifInterface;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;



import android.content.Context;
import android.net.Uri;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class CommunityPostsActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_PERMISSION = 100;

    // 1. STATIC LIST to store user-made posts
    private static final List<Post> userPosts = new ArrayList<>();

    private ImageButton buttonToAccountActivity;
    private Button btnAddPost;
    private ImageView imagePreview;
    private TextView captionView;
    private LinearLayout postContainer; // Your existing container for the feed

    private ActivityResultLauncher<Intent> cameraXLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_community_posts);

        buttonToAccountActivity = findViewById(R.id.backButton);
        btnAddPost = findViewById(R.id.btnAddPost);
        imagePreview = findViewById(R.id.imagePreview);
        captionView = findViewById(R.id.captionView);
        postContainer = findViewById(R.id.postContainer); // Your existing feed container

        // 2. RE-ADD SAVED USER POSTS on activity creation
        restoreUserPosts();

        // Back button
        buttonToAccountActivity.setOnClickListener(v -> finish());

        // Add post button
//        btnAddPost.setOnClickListener(v -> {
//            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
//                    != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(this,
//                        new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSION);
//            } else {
//                openCameraXActivity();
//            }
//        });

        // Check if device has any camera
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            Toast.makeText(this, "Camera not available on this device", Toast.LENGTH_LONG).show();
            btnAddPost.setEnabled(false); // disable the button
            return; // stop further setup that requires camera
        }

        btnAddPost.setOnClickListener(v -> {
            // Always show your custom dialog
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Camera Permission Required")
                    .setMessage("This app needs access to your camera to take posts. Please allow it.")
                    .setPositiveButton("OK", (dialog, which) -> requestCameraPermission())
                    .setNegativeButton("Cancel", null)
                    .show();
        });


        cameraXLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            String photoPath = data.getStringExtra("photoUri");
                            String caption = data.getStringExtra("caption");

                            // Show overlay UI
                            LinearLayout overlay = findViewById(R.id.postPreviewContainer);
                            ImageView imagePreview = findViewById(R.id.imagePreview);
                            EditText etCaption = findViewById(R.id.etCaption);
                            Button btnPost = findViewById(R.id.btnPost);

                            if (photoPath != null) {
                                Uri photoUri = Uri.parse(photoPath);
                                Bitmap processedImage = processCapturedImage(photoUri);
                                if (processedImage != null) {
                                    imagePreview.setImageBitmap(processedImage);
                                }

                                imagePreview.setVisibility(View.VISIBLE);
                                etCaption.setText(caption != null ? caption : "");
                                etCaption.setVisibility(View.VISIBLE);
                                overlay.setVisibility(View.VISIBLE);
                                btnPost.setVisibility(View.VISIBLE);
                            }

                            // Post button
                            btnPost.setOnClickListener(v -> {
                                String finalCaption = etCaption.getText().toString();
                                addPostToFeed(photoPath, finalCaption);

                                Toast.makeText(this, "Posted!", Toast.LENGTH_SHORT).show();

                                overlay.setVisibility(View.GONE);
                                imagePreview.setVisibility(View.GONE);
                                etCaption.setVisibility(View.GONE);
                                btnPost.setVisibility(View.GONE);
                            });
                        }
                    }
                }
        );




        // Adjust for system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void openCameraXActivity() {
//        Intent intent = new Intent(this, CameraXActivity.class);
//        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        Intent intent = new Intent(this, CameraXActivity.class);
        cameraXLauncher.launch(intent);
    }

//    private boolean checkAndRequestCameraPermission() {
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
//                != PackageManager.PERMISSION_GRANTED) {
//
//            // Request permission if not granted
//            ActivityCompat.requestPermissions(this,
//                    new String[]{Manifest.permission.CAMERA},
//                    REQUEST_PERMISSION);
//            return false;
//        }
//        return true;
//    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // ✅ Permission granted
                openCameraXActivity();
            } else {
                // ❌ Permission denied — check if permanently denied
                boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                        this, Manifest.permission.CAMERA);

                if (!showRationale) {
                    // User checked “Don’t ask again” — show settings dialog
                    Toast.makeText(this, "Camera permission permanently denied. Open settings to enable.", Toast.LENGTH_LONG).show();
                    showPermissionDeniedDialog();
                } else {
                    Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK && data != null) {
            String photoPath = data.getStringExtra("photoUri");

            String caption = data.getStringExtra("caption");

            // Find overlay views
            LinearLayout overlay = findViewById(R.id.postPreviewContainer);
            ImageView imagePreview = findViewById(R.id.imagePreview);
            EditText etCaption = findViewById(R.id.etCaption);
            Button btnPost = findViewById(R.id.btnPost);

            if (photoPath != null) {
                // Show captured photo
                Uri photoUri = Uri.parse(photoPath); // Convert String to Uri
                Bitmap processedImage = processCapturedImage(photoUri);
                if (processedImage != null) {
                    imagePreview.setImageBitmap(processedImage);
                }

                imagePreview.setVisibility(View.VISIBLE);

                // Set caption if available
                etCaption.setText(caption != null ? caption : "");
                etCaption.setVisibility(View.VISIBLE);

                // Show overlay
                overlay.setVisibility(View.VISIBLE);

                // Show Post button
                btnPost.setVisibility(View.VISIBLE);
            }

            // Handle Post button click
            btnPost.setOnClickListener(v -> {
                String finalCaption = etCaption.getText().toString();

                // 3. ADD THE POST (saves to static list AND adds to UI)
                addPostToFeed(photoPath, finalCaption);

                Toast.makeText(this, "Posted!", Toast.LENGTH_SHORT).show();

                // Hide overlay after posting
                overlay.setVisibility(View.GONE);
                imagePreview.setVisibility(View.GONE);
                etCaption.setVisibility(View.GONE);
                btnPost.setVisibility(View.GONE);
            });
        }
    }

    private void addPostToFeed(String photoPath, String caption) {

        // **STEP 1: SAVE THE POST to the static list**
        Post newPost = new Post(photoPath, caption);
        userPosts.add(0, newPost); // Add to index 0 (top)

        // **STEP 2: RENDER THE POST to the UI**
        // Inflate the post card using MaterialCardView (matching your existing posts)
        View postCard = getLayoutInflater().inflate(R.layout.post_item, postContainer, false);

        // Find views in the post card
        ImageView postImage = postCard.findViewById(R.id.postImage);
        TextView postCaption = postCard.findViewById(R.id.postCaption);

        // Set the image and caption
        postImage.setImageURI(android.net.Uri.parse(photoPath));
        postCaption.setText(caption);

        // Show caption only if it's not empty
        if (caption == null || caption.trim().isEmpty()) {
            postCaption.setVisibility(View.GONE);
        } else {
            postCaption.setVisibility(View.VISIBLE);
        }

        try {
            postImage.setImageURI(android.net.Uri.parse(photoPath));
        } catch (Exception e) {
            Toast.makeText(this, "Failed to display photo.", Toast.LENGTH_SHORT).show();
            return; // skip adding post to UI
        }


        // Add the post to the top of the feed (most recent first)
        // This is the only place we add views, so previous placeholder posts
        // that are defined in XML will be pushed down.
        postContainer.addView(postCard, 0);
    }

    /**
     * Re-inflates all saved user posts from the static list and adds them to the UI.
     * This is called in onCreate() to restore the posts.
     */
    private void restoreUserPosts() {
        // Iterate through the static list and call the UI rendering logic.
        // We iterate backward because the list is sorted 'most recent first'
        // and we want to insert them at index 0 (the top) of the UI in that order.
        // If we iterate forward and insert at 0, the first element (oldest)
        // would end up at the very top.

        for (int i = userPosts.size() - 1; i >= 0; i--) {
            Post post = userPosts.get(i);

            View postCard = getLayoutInflater().inflate(R.layout.post_item, postContainer, false);

            ImageView postImage = postCard.findViewById(R.id.postImage);
            TextView postCaption = postCard.findViewById(R.id.postCaption);

            postImage.setImageURI(android.net.Uri.parse(post.getPhotoPath()));
            postCaption.setText(post.getCaption());

            if (post.getCaption() == null || post.getCaption().trim().isEmpty()) {
                postCaption.setVisibility(View.GONE);
            } else {
                postCaption.setVisibility(View.VISIBLE);
            }

            // Add the restored post to the top (index 0).
            postContainer.addView(postCard, 0);
        }
    }

    private void showPermissionDeniedDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Camera Permission Needed")
                .setMessage("You’ve permanently denied camera access. Please enable it in Settings to take photos.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.CAMERA},
                REQUEST_PERMISSION
        );
    }

    private Bitmap processCapturedImage(Uri photoUri) {
        try {
            // Open InputStream
            InputStream input = getContentResolver().openInputStream(photoUri);
            Bitmap original = BitmapFactory.decodeStream(input);
            input.close();

            // Read EXIF orientation
            input = getContentResolver().openInputStream(photoUri);
            ExifInterface exif = new ExifInterface(input);
            input.close();
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            // Rotate if needed
            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
            }

            Bitmap rotated = Bitmap.createBitmap(original, 0, 0, original.getWidth(), original.getHeight(), matrix, true);

            // Resize to max width/height 800px
            int maxDim = 800;
            float scale = Math.min((float) maxDim / rotated.getWidth(), (float) maxDim / rotated.getHeight());
            int newWidth = Math.round(rotated.getWidth() * scale);
            int newHeight = Math.round(rotated.getHeight() * scale);
            Bitmap resized = Bitmap.createScaledBitmap(rotated, newWidth, newHeight, true);

            return resized;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private void applyFilter(ImageView imageView, String filterType) {
        ColorMatrix colorMatrix = new ColorMatrix();

        switch (filterType) {
            case "grayscale":
                colorMatrix.setSaturation(0);
                break;
            case "sepia":
                colorMatrix.setScale(1f, 0.95f, 0.82f, 1f);
                break;
            case "bright":
                colorMatrix.set(new float[]{
                        1.2f, 0, 0, 0, 30,
                        0, 1.2f, 0, 0, 30,
                        0, 0, 1.2f, 0, 30,
                        0, 0, 0, 1, 0
                });
                break;
            case "cool":
                colorMatrix.set(new float[]{
                        0.9f, 0, 0, 0, 0,
                        0, 1, 0, 0, 0,
                        0, 0, 1.2f, 0, 0,
                        0, 0, 0, 1, 0
                });
                break;
            default:
                return; // no filter
        }

        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
        imageView.setColorFilter(filter);
    }


}