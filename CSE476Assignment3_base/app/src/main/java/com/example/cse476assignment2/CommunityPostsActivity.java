package com.example.cse476assignment2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CommunityPostsActivity extends AppCompatActivity implements PostAdapter.OnPostInteractionListener {

    private final List<Post> userPosts = new ArrayList<>();
    private PostAdapter postAdapter;
    private SortOption currentSortOption = SortOption.MOST_RECENT;
    private ActivityResultLauncher<Intent> cameraXLauncher;
    private ActivityResultLauncher<Intent> commentsLauncher;
    private ActivityResultLauncher<Intent> postPreviewLauncher;

    private enum SortOption {
        MOST_RECENT,
        MOST_LIKED
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_posts);

        ImageButton backButton = findViewById(R.id.backButton);
        Button addPostButton = findViewById(R.id.btnAddPost);
        RecyclerView postsRecyclerView = findViewById(R.id.recyclerPosts);
        Spinner sortSpinner = findViewById(R.id.sortSpinner);

        List<Post> loadedPosts = DataManager.loadPosts(this);
        if (loadedPosts == null || loadedPosts.isEmpty()) {
            ensureDefaultPosts();
            DataManager.savePosts(this, userPosts);
        } else {
            userPosts.addAll(loadedPosts);
        }

        postsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        postAdapter = new PostAdapter(userPosts, this);
        postsRecyclerView.setAdapter(postAdapter);

        setupSortSpinner(sortSpinner);
        sortPostsAndRefresh();
        registerActivityResultLaunchers();

        backButton.setOnClickListener(v -> finish());
        addPostButton.setOnClickListener(v -> showImageSourceChooser());
    }

    @Override
    protected void onStop() {
        super.onStop();
        DataManager.savePosts(this, userPosts);
    }

    private void registerActivityResultLaunchers() {
        cameraXLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        String photoPath = result.getData().getStringExtra("photoUri");
                        String location = result.getData().getStringExtra("location");
                        if (photoPath != null) {
                            Intent previewIntent = new Intent(this, PostPreviewActivity.class);
                            previewIntent.putExtra("photoUri", photoPath);
                            previewIntent.putExtra("location", location);
                            postPreviewLauncher.launch(previewIntent);
                        }
                    }
                }
        );

        postPreviewLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        String photoUri = result.getData().getStringExtra("photoUri");
                        String caption = result.getData().getStringExtra("caption");
                        String location = result.getData().getStringExtra("location");
                        ArrayList<String> hashtags = result.getData().getStringArrayListExtra("hashtags");

                        if (photoUri != null) {
                            addPostToFeed(Uri.parse(photoUri), caption, location, hashtags);
                        }
                    }
                }
        );

        commentsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Post updatedPost = (Post) result.getData().getSerializableExtra(CommentsActivity.EXTRA_POST);
                        int position = result.getData().getIntExtra("POST_POSITION", -1);

                        if (updatedPost != null && position != -1 && position < userPosts.size()) {
                            userPosts.set(position, updatedPost);
                            postAdapter.notifyItemChanged(position);
                        }
                    }
                });
    }

    @Override
    public void onCommentClick(Post post, int position) {
        Intent intent = new Intent(this, CommentsActivity.class);
        intent.putExtra(CommentsActivity.EXTRA_POST, post);
        intent.putExtra("POST_POSITION", position);
        commentsLauncher.launch(intent);
    }

    @Override
    public void onLikeClick(int position) {
        // This is handled in the adapter, but the method must be implemented.
    }

    @Override
    public void onHashtagClick(String hashtag) {
        Intent intent = new Intent(this, HashtagPostsActivity.class);
        intent.putExtra(HashtagPostsActivity.EXTRA_HASHTAG, hashtag);
        intent.putExtra(HashtagPostsActivity.EXTRA_ALL_POSTS, (Serializable) userPosts);
        startActivity(intent);
    }

    // NEW: Implementation for the delete button listener
    @Override
    public void onDeleteClick(int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Post")
                .setMessage("Are you sure you want to delete this post? This cannot be undone.")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Remove the post from the list
                    userPosts.remove(position);
                    // Notify the adapter that an item was removed
                    postAdapter.notifyItemRemoved(position);
                    // Save the updated list to persist the deletion
                    DataManager.savePosts(CommunityPostsActivity.this, userPosts);
                    // Show confirmation message
                    Toast.makeText(this, "Post deleted successfully.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null) // Do nothing if "No" is pressed
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void setupSortSpinner(Spinner sortSpinner) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.post_sort_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(adapter);
        sortSpinner.setSelection(currentSortOption.ordinal());
        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentSortOption = position == 0 ? SortOption.MOST_RECENT : SortOption.MOST_LIKED;
                sortPostsAndRefresh();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void addPostToFeed(Uri imageUri, String caption, String location, List<String> hashtags) {
        Post newPost = new Post(imageUri, caption, getString(R.string.you_as_user), location);
        if (hashtags != null) {
            newPost.setHashtags(hashtags);
        }
        userPosts.add(0, newPost);
        sortPostsAndRefresh();
    }

    private void sortPostsAndRefresh() {
        if (currentSortOption == SortOption.MOST_RECENT) {
            Collections.sort(userPosts, (p1, p2) -> Long.compare(p2.getCreatedAt(), p1.getCreatedAt()));
        } else {
            Collections.sort(userPosts, (p1, p2) -> Integer.compare(p2.getLikeCount(), p1.getLikeCount()));
        }
        postAdapter.notifyDataSetChanged();
    }

    private void ensureDefaultPosts() {
        if (!userPosts.isEmpty()) return;
        long now = System.currentTimeMillis();

        Post squirrelPost = new Post(R.drawable.squirrel_post, "Look at this little guy!", "Sparty", "W. J. Beal Botanical Garden", 42, now - TimeUnit.HOURS.toMillis(18));
        ArrayList<String> squirrelTags = new ArrayList<>();
        squirrelTags.add("campuslife"); squirrelTags.add("squirrels"); squirrelTags.add("cute");
        squirrelPost.setHashtags(squirrelTags);
        userPosts.add(squirrelPost);

        Post studyPost = new Post(R.drawable.online_class, "Late night study session.", "Zeke", "Online", 120, now - TimeUnit.HOURS.toMillis(6));
        ArrayList<String> studyTags = new ArrayList<>();
        studyTags.add("studygrind"); studyTags.add("finals"); studyTags.add("latenight");
        studyPost.setHashtags(studyTags);
        userPosts.add(studyPost);

        Post towerPost = new Post(R.drawable.beaumont_tower, "Campus is beautiful today.", "Jen", "Beaumont Tower", 75, now - TimeUnit.DAYS.toMillis(1));
        ArrayList<String> towerTags = new ArrayList<>();
        towerTags.add("gogreen"); towerTags.add("msu"); towerTags.add("spartans");
        towerPost.setHashtags(towerTags);
        userPosts.add(towerPost);
    }

    private void showImageSourceChooser() {
        Intent intent = new Intent(this, CameraXActivity.class);
        intent.putExtra("USERNAME", getLoggedInUser());
        cameraXLauncher.launch(intent);
    }

    private String getLoggedInUser() {
        SharedPreferences loginPrefs = getSharedPreferences("LOGIN_PREFS", MODE_PRIVATE);
        return loginPrefs.getString("USERNAME", "default_user");
    }
}