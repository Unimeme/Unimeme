// /app/src/main/java/com/example/cse476assignment2/CommentsActivity.java

package com.example.cse476assignment2;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;

public class CommentsActivity extends AppCompatActivity {

    public static final String EXTRA_POST = "EXTRA_POST";

    private RecyclerView commentsRecyclerView;
    private TextView tvNoComments;
    private Post post; // To hold the post we're commenting on

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        commentsRecyclerView = findViewById(R.id.commentsRecyclerView);
        tvNoComments = findViewById(R.id.tvNoComments);

        // Set up the toolbar's back button to close the activity
        toolbar.setNavigationOnClickListener(v -> finish());

        // Get the Post object from the intent that started this activity
        post = (Post) getIntent().getSerializableExtra(EXTRA_POST);
        if (post == null) {
            // If for some reason the post isn't passed, close the activity
            // to prevent a crash.
            finish();
            return;
        }

        // Initially, just check if there are comments and update the view
        updateCommentsVisibility();
    }

    private void updateCommentsVisibility() {
        if (post.getCommentObjects().isEmpty()) {
            commentsRecyclerView.setVisibility(View.GONE);
            tvNoComments.setVisibility(View.VISIBLE);
        } else {
            commentsRecyclerView.setVisibility(View.VISIBLE);
            tvNoComments.setVisibility(View.GONE);
        }
    }
}