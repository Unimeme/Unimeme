package com.example.cse476assignment2;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Simple page that lets the user choose media from their device library and send it back
 * through the existing post preview flow.
 */
public class MediaPickerActivity extends AppCompatActivity {

    private static final String STATE_SELECTED_URI = "selected_uri";

    private ImageView selectedImagePreview;
    private Button btnContinueUpload;

    private Uri selectedUri;

    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<Intent> postPreviewLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_picker);

        selectedImagePreview = findViewById(R.id.selectedImagePreview);
        Button btnChooseFromLibrary = findViewById(R.id.btnChooseFromLibrary);
        btnContinueUpload = findViewById(R.id.btnContinueUpload);
        Button btnCancelUpload = findViewById(R.id.btnCancelUpload);

        if (savedInstanceState != null) {
            String savedUri = savedInstanceState.getString(STATE_SELECTED_URI);
            if (savedUri != null) {
                selectedUri = Uri.parse(savedUri);
                selectedImagePreview.setImageURI(selectedUri);
                btnContinueUpload.setEnabled(true);
            }
        }

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedUri = uri;
                        selectedImagePreview.setImageURI(uri);
                        btnContinueUpload.setEnabled(true);
                    }
                }
        );

        postPreviewLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            setResult(Activity.RESULT_OK, data);
                        }
                        finish();
                    }
                }
        );

        btnChooseFromLibrary.setOnClickListener(v -> galleryLauncher.launch("image/*"));

        btnContinueUpload.setOnClickListener(v -> {
            if (selectedUri == null) {
                Toast.makeText(this, R.string.no_media_selected, Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, PostPreviewActivity.class);
            intent.putExtra("photoUri", selectedUri.toString());
            postPreviewLauncher.launch(intent);
        });

        btnCancelUpload.setOnClickListener(v -> finish());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (selectedUri != null) {
            outState.putString(STATE_SELECTED_URI, selectedUri.toString());
        }
    }
}
