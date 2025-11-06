package com.example.cse476assignment2;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CommunityPostsActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION = 100;

    private static final String PREFS_NAME = "COMMUNITY_POSTS_PREFS";
    private static final String KEY_USER_POSTS = "USER_POSTS";

    private static final List<Post> userPosts = new ArrayList<>();
    private static boolean hasLoadedPosts = false;

    private ActivityResultLauncher<Intent> cameraXLauncher;
    private ActivityResultLauncher<String> galleryLauncher;

    private LinearLayout postPreviewContainer;
    private ImageView imagePreview;
    private EditText captionInput;
    private Button postButton;
    private RecyclerView postsRecyclerView;
    private PostAdapter postAdapter;
    private Uri pendingImageUri;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_community_posts);

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

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        if (!hasLoadedPosts) {
            userPosts.clear();
            ensureDefaultPosts();
            loadUserPostsFromStorage();
            hasLoadedPosts = true;
        }

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

        String caption = captionInput.getText().toString();
        addPostToFeed(pendingImageUri, caption);
        Toast.makeText(this, R.string.post_success_message, Toast.LENGTH_SHORT).show();
        hidePostPreview();
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
    }

    private void addPostToFeed(Uri imageUri, String caption) {
        Post newPost = new Post(imageUri, caption, getString(R.string.you_as_user));
        userPosts.add(0, newPost);
        postAdapter.notifyItemInserted(0);
        postsRecyclerView.scrollToPosition(0);
        saveUserPostsToStorage();
    }

    private void ensureDefaultPosts() {
        if (!userPosts.isEmpty()) {
            boolean hasDefault = false;
            for (Post post : userPosts) {
                if (post.getImageResId() != null) {
                    hasDefault = true;
                    break;
                }
            }
            if (hasDefault) {
                return;
            }
        }

        userPosts.add(new Post(R.drawable.squirrel_post, getString(R.string.post_caption_1), getString(R.string.tenth_place_name)));
        userPosts.add(new Post(R.drawable.online_class, getString(R.string.post_caption_2), getString(R.string.first_place_name)));
        userPosts.add(new Post(R.drawable.beaumont_tower, getString(R.string.post_caption_3), getString(R.string.fifth_place_name)));
    }

    private void saveUserPostsToStorage() {
        JSONArray jsonArray = new JSONArray();
        for (Post post : userPosts) {
            if (post.getImageUri() == null) {
                continue;
            }

            JSONObject postObject = new JSONObject();
            try {
                postObject.put("imageUri", post.getImageUri().toString());
                postObject.put("caption", post.getCaption());
                postObject.put("author", post.getAuthor());

                JSONArray commentsArray = new JSONArray();
                for (String comment : post.getComments()) {
                    commentsArray.put(comment);
                }
                postObject.put("comments", commentsArray);
            } catch (JSONException e) {
                e.printStackTrace();
                continue;
            }

            jsonArray.put(postObject);
        }

        sharedPreferences.edit().putString(KEY_USER_POSTS, jsonArray.toString()).apply();
    }

    private void loadUserPostsFromStorage() {
        String storedPosts = sharedPreferences.getString(KEY_USER_POSTS, null);
        if (storedPosts == null || storedPosts.isEmpty()) {
            return;
        }

        try {
            JSONArray jsonArray = new JSONArray(storedPosts);
            List<Post> loadedPosts = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject postObject = jsonArray.getJSONObject(i);
                String imageUriString = postObject.optString("imageUri", null);
                if (imageUriString == null) {
                    continue;
                }

                Uri imageUri = Uri.parse(imageUriString);
                String caption = postObject.optString("caption", "");
                String author = postObject.optString("author", getString(R.string.you_as_user));

                Post post = new Post(imageUri, caption, author);

                JSONArray commentsArray = postObject.optJSONArray("comments");
                if (commentsArray != null) {
                    for (int j = 0; j < commentsArray.length(); j++) {
                        post.getComments().add(commentsArray.optString(j));
                    }
                }

                loadedPosts.add(post);
            }

            for (int i = loadedPosts.size() - 1; i >= 0; i--) {
                userPosts.add(0, loadedPosts.get(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveUserPostsToStorage();
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSION) {
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
}
