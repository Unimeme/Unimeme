// /app/src/main/java/com/example/cse476assignment2/PostPreviewActivity.java
package com.example.cse476assignment2;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.exifinterface.media.ExifInterface;
import android.provider.MediaStore;

import com.example.cse476assignment2.model.Req.CreatePostReq;
import com.example.cse476assignment2.model.Res.CreatePostRes;
import com.example.cse476assignment2.model.Res.UploadImageRes;
import com.example.cse476assignment2.net.ApiClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostPreviewActivity extends AppCompatActivity {

    private ImageView previewImage;
    private EditText captionInput;
    private Button btnContinue, btnCancel;
    private EditText hashtagInput;

    private Bitmap originalBitmap;
    private Bitmap filteredBitmap;
    private String location;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_preview);

        previewImage = findViewById(R.id.previewImage);
        captionInput = findViewById(R.id.captionInput);
        btnContinue = findViewById(R.id.btnContinue);
        btnCancel = findViewById(R.id.btnCancel);
        hashtagInput = findViewById(R.id.hashtagInput);

        Button btnGrayscale = findViewById(R.id.btnGrayscale);
        Button btnSepia = findViewById(R.id.btnSepia);
        Button btnBright = findViewById(R.id.btnBright);
        Button btnCool = findViewById(R.id.btnCool);
        Button btnReset = findViewById(R.id.btnReset);

        String photoUriStr = getIntent().getStringExtra("photoUri");
        location = getIntent().getStringExtra("location");
        if (location == null) {
            location = "";
        }

        if (photoUriStr != null) {
            Uri photoUri = Uri.parse(photoUriStr);
            originalBitmap = loadAndRotateBitmap(photoUri);
            filteredBitmap = originalBitmap;
            if (filteredBitmap != null) {
                previewImage.setImageBitmap(filteredBitmap);
            } else {
                Toast.makeText(this, "Failed to load image.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        } else {
            Toast.makeText(this, "No image provided.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnCancel.setOnClickListener(v -> finish());

        btnContinue.setOnClickListener(v -> {
            String caption = captionInput.getText().toString();

            if (filteredBitmap == null) {
                Toast.makeText(this, "No image to save.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 1) Save bitmap to gallery
            Uri savedUri = saveBitmap(filteredBitmap, "post_" + System.currentTimeMillis());
            if (savedUri == null) {
                Toast.makeText(this, "Something went wrong. Try again later!", Toast.LENGTH_SHORT).show();
                return;
            }

            // 2) Prepare hashtags
            ArrayList<String> hashtags = new ArrayList<>();
            String rawHashtags = hashtagInput.getText().toString().trim();
            if (!rawHashtags.isEmpty()) {
                String[] tags = rawHashtags.split("\\s+");
                for (String tag : tags) {
                    if (hashtags.size() >= 3) {
                        break;
                    }
                    if (tag.startsWith("#") && tag.length() > 1) {
                        hashtags.add(tag.substring(1));
                    }
                }
            }

            // 3) Send result back to previous activity (so your UI still works offline)
            Intent resultIntent = new Intent();
            resultIntent.putExtra("photoUri", savedUri.toString());
            resultIntent.putExtra("caption", caption);
            resultIntent.putExtra("hashtags", hashtags);
            resultIntent.putExtra("location", location);
            setResult(RESULT_OK, resultIntent);

            // 4) upload to server in background
            uploadToServerAndCreatePost(filteredBitmap, caption);

            finish();
        });

        btnGrayscale.setOnClickListener(v -> applyFilter("grayscale"));
        btnSepia.setOnClickListener(v -> applyFilter("sepia"));
        btnBright.setOnClickListener(v -> applyFilter("bright"));
        btnCool.setOnClickListener(v -> applyFilter("cool"));
        btnReset.setOnClickListener(v -> {
            if (originalBitmap != null) {
                filteredBitmap = originalBitmap;
                previewImage.setImageBitmap(originalBitmap);
            }
        });
    }

    private Uri saveBitmap(Bitmap bitmap, String displayName) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, displayName + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MyApp");

        Uri uri;
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                uri = getContentResolver().insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values);
            } else {
                uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            }

            if (uri != null) {
                try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                    if (out != null) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out);
                    } else {
                        throw new IOException("Failed to get output stream.");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return uri;
    }


    private Bitmap loadAndRotateBitmap(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            InputStream input = getContentResolver().openInputStream(uri);
            if (input == null) return bitmap;

            ExifInterface exif = new ExifInterface(input);
            input.close();
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90: matrix.postRotate(90); break;
                case ExifInterface.ORIENTATION_ROTATE_180: matrix.postRotate(180); break;
                case ExifInterface.ORIENTATION_ROTATE_270: matrix.postRotate(270); break;
            }
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void applyFilter(String filterType) {
        if (originalBitmap == null) return;
        Bitmap newBitmap = Bitmap.createBitmap(originalBitmap.getWidth(), originalBitmap.getHeight(), originalBitmap.getConfig());
        ColorMatrix colorMatrix = new ColorMatrix();
        switch (filterType) {
            case "grayscale": colorMatrix.setSaturation(0); break;
            case "sepia": colorMatrix.setScale(1f, 0.95f, 0.82f, 1f); break;
            case "bright":
                colorMatrix.set(new float[]{ 1.2f,0,0,0,30, 0,1.2f,0,0,30, 0,0,1.2f,0,30, 0,0,0,1,0 });
                break;
            case "cool":
                colorMatrix.set(new float[]{ 0.9f,0,0,0,0, 0,1,0,0,0, 0,0,1.2f,0,0, 0,0,0,1,0 });
                break;
        }
        android.graphics.Canvas canvas = new android.graphics.Canvas(newBitmap);
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        canvas.drawBitmap(originalBitmap, 0, 0, paint);
        filteredBitmap = newBitmap;
        previewImage.setImageBitmap(filteredBitmap);
    }

    private void uploadToServerAndCreatePost(Bitmap bitmap, String caption) {

        // Get username/password from SharedPreferences
        SharedPreferences loginPrefs = getSharedPreferences("LOGIN_PREFS", MODE_PRIVATE);
        String username = loginPrefs.getString("USERNAME", null);
        String password = loginPrefs.getString("PASSWORD", null);  // make sure you save this at login

        if (username == null || password == null) {
            Toast.makeText(this, "Not logged in. Cannot upload post.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1) Save bitmap to a temp file in cache dir
        File cacheFile = new File(getCacheDir(), "upload_post_" + System.currentTimeMillis() + ".jpg");
        try (FileOutputStream fos = new FileOutputStream(cacheFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to prepare image file.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2) Build multipart request for upload
        RequestBody usernameBody = RequestBody.create(
                okhttp3.MultipartBody.FORM, username);

        RequestBody passwordBody = RequestBody.create(
                okhttp3.MultipartBody.FORM, password);

        RequestBody imgBody = RequestBody.create(
                okhttp3.MediaType.parse("image/jpeg"), cacheFile);

        MultipartBody.Part imagePart = MultipartBody.Part.createFormData(
                "image", cacheFile.getName(), imgBody);

        // 3) Call uploadPostImage
        ApiClient.get().uploadPostImage(usernameBody, passwordBody, imagePart)
                .enqueue(new Callback<UploadImageRes>() {
                    @Override
                    public void onResponse(Call<UploadImageRes> call,
                                           Response<UploadImageRes> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            Toast.makeText(PostPreviewActivity.this,
                                    "Image upload failed: " + response.code(),
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        UploadImageRes res = response.body();
                        if (!res.IsSuccess || res.imageUrl == null) {
                            Toast.makeText(PostPreviewActivity.this,
                                    "Image upload failed: " + res.error,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // 4) If upload succeeded, create the post with the returned imageUrl
                        createPostOnServer(username, password, res.imageUrl, caption);
                    }

                    @Override
                    public void onFailure(Call<UploadImageRes> call, Throwable t) {
                        Toast.makeText(PostPreviewActivity.this,
                                "Image upload error: " + t.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void createPostOnServer(String username,
                                    String password,
                                    String imageUrl,
                                    String caption) {

        // For now, we send null for locationId (or set an actual id if you have one)
        Integer locationId = null;

        CreatePostReq body = new CreatePostReq(
                username,
                password,
                imageUrl,
                caption,
                locationId
        );

        ApiClient.get().createPost(body).enqueue(new Callback<CreatePostRes>() {
            @Override
            public void onResponse(Call<CreatePostRes> call, Response<CreatePostRes> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(PostPreviewActivity.this,
                            "Create post failed: " + response.code(),
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                CreatePostRes res = response.body();
                if (!res.IsSuccess) {
                    Toast.makeText(PostPreviewActivity.this,
                            "Create post failed: " + res.error,
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(PostPreviewActivity.this,
                        "Post uploaded!",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<CreatePostRes> call, Throwable t) {
                Toast.makeText(PostPreviewActivity.this,
                        "Create post error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }


}