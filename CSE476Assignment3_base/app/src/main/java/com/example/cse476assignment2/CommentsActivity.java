// /app/src/main/java/com/example/cse476assignment2/CommentsActivity.java
package com.example.cse476assignment2;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;

public class CommentsActivity extends AppCompatActivity {

    public static final String EXTRA_POST = "EXTRA_POST";

    private RecyclerView commentsRecyclerView;
    private TextView tvNoComments;
    private EditText etCommentInput;
    private Button btnPostComment;
    private CommentAdapter adapter;
    private Post post; // To hold the post we're commenting on

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        // Find views
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        commentsRecyclerView = findViewById(R.id.commentsRecyclerView);
        tvNoComments = findViewById(R.id.tvNoComments);
        etCommentInput = findViewById(R.id.etCommentInput);
        btnPostComment = findViewById(R.id.btnPostComment);

        // Set up the toolbar's back button
        toolbar.setNavigationOnClickListener(v -> finish());

        // Get the Post object from the intent
        post = (Post) getIntent().getSerializableExtra(EXTRA_POST);
        if (post == null) {
            finish();
            return;
        }

        // --- NEW: Setup RecyclerView ---
        adapter = new CommentAdapter(post.getCommentObjects());
        commentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        commentsRecyclerView.setAdapter(adapter);
        // --- End of new RecyclerView setup ---

        // Handle the "Post" button click
        btnPostComment.setOnClickListener(v -> postNewComment());

        // Update visibility based on whether there are comments
        updateCommentsVisibility();
    }

    private void postNewComment() {
        String commentText = etCommentInput.getText().toString().trim();
        if (commentText.isEmpty()) {
            Toast.makeText(this, "Cannot post an empty comment", Toast.LENGTH_SHORT).show();
            return;
        }

        // For now, we'll hardcode the author as "You" and use a default profile icon.
        // In a real app, this would come from the logged-in user's profile.
        Comment newComment = new Comment("You", commentText, R.drawable.profile_icon);
        post.addCommentObject(newComment);

        // Notify the adapter that a new item has been added at the top
        adapter.notifyItemInserted(0);

        // Scroll the RecyclerView to the top to show the new comment
        commentsRecyclerView.scrollToPosition(0);

        // Clear the input field
        etCommentInput.setText("");

        // Update visibility in case this was the first comment
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