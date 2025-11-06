// /app/src/main/java/com/example/cse476assignment2/CommunityPostsActivity.java
package com.example.cse476assignment2;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
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
    private RecyclerView postsRecyclerView;
    private SortOption currentSortOption = SortOption.MOST_RECENT;
    private ActivityResultLauncher<Intent> cameraXLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
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
        postsRecyclerView = findViewById(R.id.recyclerPosts);
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
                        if (photoPath != null) {
                            Intent previewIntent = new Intent(this, PostPreviewActivity.class);
                            previewIntent.putExtra("photoUri", photoPath);
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
                        ArrayList<String> hashtags = result.getData().getStringArrayListExtra("hashtags");

                        if (photoUri != null) {
                            addPostToFeed(Uri.parse(photoUri), caption, "", hashtags);
                        }
                    }
                }
        );

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        Intent previewIntent = new Intent(this, PostPreviewActivity.class);
                        previewIntent.putExtra("photoUri", uri.toString());
                        postPreviewLauncher.launch(previewIntent);
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
        // Persistence is handled by onStop()
    }

    // --- NEW: Handle hashtag clicks ---
    @Override
    public void onHashtagClick(String hashtag) {
        Intent intent = new Intent(this, HashtagPostsActivity.class);
        intent.putExtra(HashtagPostsActivity.EXTRA_HASHTAG, hashtag);
        // Pass the entire list of posts so the next activity can filter it
        intent.putExtra(HashtagPostsActivity.EXTRA_ALL_POSTS, (Serializable) userPosts);
        startActivity(intent);
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
        userPosts.add(new Post(R.drawable.squirrel_post, "Look at this little guy!", "Sparty", "W. J. Beal Botanical Garden", 42, now - TimeUnit.HOURS.toMillis(18)));
        userPosts.add(new Post(R.drawable.online_class, "Late night study session.", "Zeke", "Online", 120, now - TimeUnit.HOURS.toMillis(6)));
        userPosts.add(new Post(R.drawable.beaumont_tower, "Campus is beautiful today.", "Jen", "Beaumont Tower", 75, now - TimeUnit.DAYS.toMillis(1)));
    }

    private void showImageSourceChooser() {
        Intent intent = new Intent(this, CameraXActivity.class);
        cameraXLauncher.launch(intent);
    }
}