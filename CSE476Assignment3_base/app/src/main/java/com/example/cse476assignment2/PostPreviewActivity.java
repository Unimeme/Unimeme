// /app/src/main/java/com/example/cse476assignment2/PostPreviewActivity.java
package com.example.cse476assignment2;

import android.content.ContentValues;
import android.content.Intent;
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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class PostPreviewActivity extends AppCompatActivity {

    private ImageView previewImage;
    private EditText captionInput;
    private Button btnContinue, btnCancel;

    private EditText hashtagInput;

    private Bitmap originalBitmap;
    private Bitmap filteredBitmap;

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
        if (photoUriStr != null) {
            Uri photoUri = Uri.parse(photoUriStr);

            originalBitmap = loadAndRotateBitmap(photoUri);
            filteredBitmap = originalBitmap;
            if (filteredBitmap != null) {
                previewImage.setImageBitmap(filteredBitmap);
            }
            else {
                Toast.makeText(this, "Failed to load image.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }
        else {
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

            Uri savedUri = saveBitmap(filteredBitmap, "post_" + System.currentTimeMillis());
            if (savedUri == null) {
                Toast.makeText(this, "Failed to save filtered image.", Toast.LENGTH_SHORT).show();
                return;
            }

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

            Intent resultIntent = new Intent();
            resultIntent.putExtra("photoUri", savedUri.toString());
            resultIntent.putExtra("caption", caption);
            resultIntent.putExtra("hashtags", hashtags);
            setResult(RESULT_OK, resultIntent);
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
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            uri = getContentResolver().insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values);
        } else {
            uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        }

        if (uri != null) {
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return uri;
    }


    private Bitmap loadAndRotateBitmap(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            InputStream input = getContentResolver().openInputStream(uri);
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
}