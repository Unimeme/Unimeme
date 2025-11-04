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

public class PostPreviewActivity extends AppCompatActivity {

    private ImageView previewImage;
    private EditText captionInput;
    private Button btnContinue, btnCancel;

    private Bitmap originalBitmap; // The untouched image from camera
    private Bitmap filteredBitmap; // holds the currently filtered bitmap

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_preview);

        previewImage = findViewById(R.id.previewImage);
        captionInput = findViewById(R.id.captionInput);
        btnContinue = findViewById(R.id.btnContinue);
        btnCancel = findViewById(R.id.btnCancel);

        Button btnGrayscale = findViewById(R.id.btnGrayscale);
        Button btnSepia = findViewById(R.id.btnSepia);
        Button btnBright = findViewById(R.id.btnBright);
        Button btnCool = findViewById(R.id.btnCool);
        Button btnReset = findViewById(R.id.btnReset);

        String photoUriStr = getIntent().getStringExtra("photoUri");
        if (photoUriStr != null) {
            Uri photoUri = Uri.parse(photoUriStr);

            originalBitmap = loadAndRotateBitmap(photoUri); // store original image
            filteredBitmap = originalBitmap;
            // initially filtered = original
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

            // Replace old insertImage call with new saveBitmap method
            Uri savedUri = saveBitmap(filteredBitmap, "post_" + System.currentTimeMillis());
            if (savedUri == null) {
                Toast.makeText(this, "Failed to save filtered image.", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent resultIntent = new Intent();
            resultIntent.putExtra("photoUri", savedUri.toString());
            resultIntent.putExtra("caption", caption);
            setResult(RESULT_OK, resultIntent);
            finish();
        });



        // Filter buttons
        btnGrayscale.setOnClickListener(v -> applyFilter("grayscale"));
        btnSepia.setOnClickListener(v -> applyFilter("sepia"));
        btnBright.setOnClickListener(v -> applyFilter("bright"));
        btnCool.setOnClickListener(v -> applyFilter("cool"));
        btnReset.setOnClickListener(v -> {
            if (originalBitmap != null) {
                filteredBitmap = originalBitmap;          // reset filtered bitmap
                previewImage.setImageBitmap(originalBitmap); // show original
            }
        });
    }

    private Uri saveBitmap(Bitmap bitmap, String displayName) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, displayName + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MyApp"); // optional folder

//        Uri uri = getContentResolver().insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values);
        Uri uri;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            // Android 10+ (API 29+)
            uri = getContentResolver().insert(
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                    values
            );
        } else {
            // Older Android versions (before API 29)
            uri = getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
            );
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
            // Load bitmap
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);

            // Read EXIF orientation
            InputStream input = getContentResolver().openInputStream(uri);
            ExifInterface exif = new ExifInterface(input);
            input.close();
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            // Rotate if needed
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
        if (originalBitmap == null) return; // start from original

        Bitmap newBitmap = Bitmap.createBitmap(originalBitmap.getWidth(), originalBitmap.getHeight(), originalBitmap.getConfig());

        ColorMatrix colorMatrix = new ColorMatrix();

        switch (filterType) {
            case "grayscale": colorMatrix.setSaturation(0); break;
            case "sepia": colorMatrix.setScale(1f, 0.95f, 0.82f, 1f); break;
            case "bright":
                colorMatrix.set(new float[]{
                        1.2f,0,0,0,30,
                        0,1.2f,0,0,30,
                        0,0,1.2f,0,30,
                        0,0,0,1,0
                });
                break;
            case "cool":
                colorMatrix.set(new float[]{
                        0.9f,0,0,0,0,
                        0,1,0,0,0,
                        0,0,1.2f,0,0,
                        0,0,0,1,0
                });
                break;
        }

        android.graphics.Canvas canvas = new android.graphics.Canvas(newBitmap);
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        canvas.drawBitmap(originalBitmap, 0, 0, paint); // <-- use originalBitmap here

        filteredBitmap = newBitmap;
        previewImage.setImageBitmap(filteredBitmap);
    }

}
