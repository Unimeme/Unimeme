// /app/src/main/java/com/example/cse476assignment2/CommentsActivity.java
package com.example.cse476assignment2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cse476assignment2.model.CommentDto;
import com.example.cse476assignment2.model.Req.AddCommentReq;
import com.example.cse476assignment2.model.Req.DeleteCommentReq;
import com.example.cse476assignment2.model.Res.AddCommentRes;
import com.example.cse476assignment2.model.Res.DeleteCommentRes;
import com.example.cse476assignment2.model.Res.GetCommentsRes;
import com.example.cse476assignment2.net.ApiClient;
import com.google.android.material.appbar.MaterialToolbar;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// UPDATED: Implement the new, more general listener
public class CommentsActivity extends AppCompatActivity implements CommentAdapter.OnCommentInteractionListener {

    public static final String EXTRA_POST = "EXTRA_POST";

    private RecyclerView commentsRecyclerView;
    private TextView tvNoComments;
    private EditText etCommentInput;
    private Button btnPostComment;
    private CommentAdapter adapter;
    private Post post;
    private boolean hasChanges = false;

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

        // UPDATED: Pass the current user ("You") and the new listener to the adapter
        adapter = new CommentAdapter(post.getCommentObjects(), "You", this);
        commentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        commentsRecyclerView.setAdapter(adapter);

        loadCommentsFromServer();
        btnPostComment.setOnClickListener(v -> postNewComment());

        updateCommentsVisibility();
    }

    private void postNewComment() {
        String commentText = etCommentInput.getText().toString().trim();
        if (commentText.isEmpty()) {
            Toast.makeText(this, "Cannot post an empty comment", Toast.LENGTH_SHORT).show();
            return;
        }
        SharedPreferences prefs = getSharedPreferences("LOGIN_PREFS", MODE_PRIVATE);
        String username = prefs.getString("USERNAME", null);
        String password = prefs.getString("PASSWORD", null);


        if (username == null || password == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        AddCommentReq req = new AddCommentReq(
                username,
                password,
                post.getPostId(),
                commentText
        );

        ApiClient.get().addComment(req).enqueue(new retrofit2.Callback<AddCommentRes>() {
            @Override
            public void onResponse(retrofit2.Call<AddCommentRes> call,
                                   retrofit2.Response<AddCommentRes> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(CommentsActivity.this, "Server error", Toast.LENGTH_SHORT).show();
                    return;
                }

                AddCommentRes body = response.body();
                if (!body.IsSuccess) {
                    Toast.makeText(CommentsActivity.this, "Failed: " + body.error, Toast.LENGTH_SHORT).show();
                    return;
                }

                Comment newComment = new Comment("You", commentText, R.drawable.profile_icon);
                newComment.setCommentId(body.commentId);
                post.addCommentObject(newComment);
                hasChanges = true;

                adapter.notifyItemInserted(0);
                commentsRecyclerView.scrollToPosition(0);
                etCommentInput.setText("");
                updateCommentsVisibility();
            }

            @Override
            public void onFailure(retrofit2.Call<AddCommentRes> call, Throwable t) {
                Toast.makeText(CommentsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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

    @Override
    public void onLikeClicked() {
        hasChanges = true; // Mark that data has changed for saving
    }

    // --- NEW: Implementation of the onDeleteClicked method ---
    @Override
    public void onDeleteClicked(int position) {

        SharedPreferences prefs = getSharedPreferences("LOGIN_PREFS", MODE_PRIVATE);
        String username = prefs.getString("USERNAME", null);
        String password = prefs.getString("PASSWORD", null);

        if (username == null || password == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        Comment c = post.getCommentObjects().get(position);
        int commentId = c.getCommentId();

        DeleteCommentReq req = new DeleteCommentReq(username, password, commentId);

        ApiClient.get().deleteComment(req).enqueue(new Callback<DeleteCommentRes>() {
            @Override
            public void onResponse(Call<DeleteCommentRes> call, Response<DeleteCommentRes> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(CommentsActivity.this, "Server error", Toast.LENGTH_SHORT).show();
                    return;
                }

                DeleteCommentRes res = response.body();
                if (!res.IsSuccess) {
                    Toast.makeText(CommentsActivity.this, "Delete failed: " + res.error, Toast.LENGTH_SHORT).show();
                    return;
                }

                post.getCommentObjects().remove(position);
                adapter.notifyItemRemoved(position);
                hasChanges = true;
                updateCommentsVisibility();
            }

            @Override
            public void onFailure(Call<DeleteCommentRes> call, Throwable t) {
                Toast.makeText(CommentsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public void finish() {
        if (hasChanges) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra(EXTRA_POST, post);
            resultIntent.putExtra("POST_POSITION", getIntent().getIntExtra("POST_POSITION", -1));
            setResult(RESULT_OK, resultIntent);
        } else {
            setResult(RESULT_CANCELED);
        }
        super.finish();
    }

    private void loadCommentsFromServer() {

        SharedPreferences prefs = getSharedPreferences("LOGIN_PREFS", MODE_PRIVATE);
        String username = prefs.getString("USERNAME", null);
        String password = prefs.getString("PASSWORD", null);

        if (username == null || password == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiClient.get().getComments(username, password, post.getPostId())
                .enqueue(new retrofit2.Callback<GetCommentsRes>() {
                    @Override
                    public void onResponse(retrofit2.Call<GetCommentsRes> call,
                                           retrofit2.Response<GetCommentsRes> response) {

                        if (!response.isSuccessful() || response.body() == null) {
                            Toast.makeText(CommentsActivity.this, "Failed to load comments", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        GetCommentsRes res = response.body();
                        if (res.Comments == null) return;

                        post.getCommentObjects().clear();

                        for (CommentDto dto : res.Comments) {
                            String cAuthor = dto.username != null ? dto.username : "";
                            if (cAuthor.equals(username)) cAuthor = "You";

                            String content = dto.content != null ? dto.content : "";
                            long timeMs = parseUtc(dto.createdAt);

                            Comment nc = new Comment(cAuthor, content, R.drawable.profile_icon, timeMs);
                            nc.setCommentId(dto.commentId);
                            post.addCommentObject(nc);
                        }

                        adapter.notifyDataSetChanged();
                        updateCommentsVisibility();
                    }

                    @Override
                    public void onFailure(retrofit2.Call<GetCommentsRes> call, Throwable t) {
                        Toast.makeText(CommentsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private long parseUtc(String s) {
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime ldt = LocalDateTime.parse(s, fmt);
            return ldt.toInstant(ZoneOffset.UTC).toEpochMilli();
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }


}