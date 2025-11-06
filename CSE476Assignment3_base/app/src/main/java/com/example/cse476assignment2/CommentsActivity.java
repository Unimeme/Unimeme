package com.example.cse476assignment2;

import android.content.Intent;
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
    private Post post;
    private boolean hasChanges = false; // NEW: Track if comments were added

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        commentsRecyclerView = findViewById(R.id.commentsRecyclerView);
        tvNoComments = findViewById(R.id.tvNoComments);
        etCommentInput = findViewById(R.id.etCommentInput);
        btnPostComment = findViewById(R.id.btnPostComment);

        toolbar.setNavigationOnClickListener(v -> finish());

        post = (Post) getIntent().getSerializableExtra(EXTRA_POST);
        if (post == null) {
            finish();
            return;
        }

        adapter = new CommentAdapter(post.getCommentObjects());
        commentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        commentsRecyclerView.setAdapter(adapter);

        btnPostComment.setOnClickListener(v -> postNewComment());

        updateCommentsVisibility();
    }

    private void postNewComment() {
        String commentText = etCommentInput.getText().toString().trim();
        if (commentText.isEmpty()) {
            Toast.makeText(this, "Cannot post an empty comment", Toast.LENGTH_SHORT).show();
            return;
        }

        Comment newComment = new Comment("You", commentText, R.drawable.profile_icon);
        post.addCommentObject(newComment);
        hasChanges = true; // NEW: Mark that data has changed

        adapter.notifyItemInserted(0);
        commentsRecyclerView.scrollToPosition(0);
        etCommentInput.setText("");
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

    // --- NEW: Override finish() to return the result ---
    @Override
    public void finish() {
        if (hasChanges) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra(EXTRA_POST, post);
            // Also include the original position passed to this activity
            resultIntent.putExtra("POST_POSITION", getIntent().getIntExtra("POST_POSITION", -1));
            setResult(RESULT_OK, resultIntent);
        } else {
            setResult(RESULT_CANCELED);
        }
        super.finish();
    }
}