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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cse476assignment2.model.Res.GetPostFeedRes;
import com.example.cse476assignment2.model.PostDto;
import com.example.cse476assignment2.net.ApiClient;
import com.example.cse476assignment2.net.PostMapper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CommunityPostsActivity extends AppCompatActivity implements PostAdapter.OnPostInteractionListener {

    private final List<Post> userPosts = new ArrayList<>();
    private PostAdapter postAdapter;
    private SortOption currentSortOption = SortOption.MOST_RECENT;

    private ActivityResultLauncher<Intent> cameraXLauncher;
    private ActivityResultLauncher<Intent> commentsLauncher;
    private ActivityResultLauncher<Intent> postPreviewLauncher;

    // paging
    private boolean isLoading = false;
    private Integer nextAfterId = 0;
    private final int PAGE_SIZE = 20;

    private RecyclerView postsRecyclerView;

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

        loadFeed(true);

        postsRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                if (lm == null) return;

                int lastVisible = lm.findLastVisibleItemPosition();
                if (lastVisible >= userPosts.size() - 5) {
                    loadFeed(false);
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        DataManager.savePosts(this, userPosts);
    }

    private final java.util.HashSet<Integer> seenPostIds = new java.util.HashSet<>();

    private void loadFeed(boolean firstPage) {
        if (isLoading) return;

        if (!firstPage && nextAfterId == 0) return;

        isLoading = true;

        SharedPreferences loginPrefs = getSharedPreferences("LOGIN_PREFS", MODE_PRIVATE);
        String username = loginPrefs.getString("USERNAME", null);
        String password = loginPrefs.getString("PASSWORD", null);

        if (username == null || password == null) {
            Toast.makeText(this, "Not logged in.", Toast.LENGTH_SHORT).show();
            isLoading = false;
            return;
        }

        int after = firstPage ? 0 : nextAfterId;

        ApiClient.get().getPostFeed(username, password, PAGE_SIZE, after)
                .enqueue(new retrofit2.Callback<GetPostFeedRes>() {
                    @Override
                    public void onResponse(retrofit2.Call<GetPostFeedRes> call,
                                           retrofit2.Response<GetPostFeedRes> response) {
                        isLoading = false;

                        if (!response.isSuccessful() || response.body() == null) {
                            Toast.makeText(CommunityPostsActivity.this,
                                    "Feed load failed: " + response.code(),
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        GetPostFeedRes res = response.body();
                        if (res.posts == null) res.posts = new ArrayList<>();

                        if (firstPage) {
                            userPosts.clear();
                            seenPostIds.clear();
                        }

                        for (PostDto dto : res.posts) {
                            int pid = dto.postId;
                            if (pid <= 0) continue;

                            if (seenPostIds.contains(pid)) continue;
                            seenPostIds.add(pid);

                            userPosts.add(PostMapper.fromDto(dto, username));
                        }

                        nextAfterId = res.nextAfterId; // int

                        if (res.posts.isEmpty()) nextAfterId = 0;

                        sortPostsAndRefresh();
                    }

                    @Override
                    public void onFailure(retrofit2.Call<GetPostFeedRes> call, Throwable t) {
                        isLoading = false;
                        Toast.makeText(CommunityPostsActivity.this,
                                "Feed error: " + t.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
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
    public void onLikeClick(int position) {}

    @Override
    public void onHashtagClick(String hashtag) {
        Intent intent = new Intent(this, HashtagPostsActivity.class);
        intent.putExtra(HashtagPostsActivity.EXTRA_HASHTAG, hashtag);
        intent.putExtra(HashtagPostsActivity.EXTRA_ALL_POSTS, (Serializable) userPosts);
        startActivity(intent);
    }

    @Override
    public void onDeleteClick(int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Post")
                .setMessage("Are you sure you want to delete this post? This cannot be undone.")
                .setPositiveButton("Yes", (dialog, which) -> {
                    userPosts.remove(position);
                    postAdapter.notifyItemRemoved(position);
                    DataManager.savePosts(CommunityPostsActivity.this, userPosts);
                    Toast.makeText(this, "Post deleted successfully.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void setupSortSpinner(Spinner sortSpinner) {
        ArrayAdapter<CharSequence> adapter =
                ArrayAdapter.createFromResource(this,
                        R.array.post_sort_options,
                        android.R.layout.simple_spinner_item);
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
        if (hashtags != null) newPost.setHashtags(hashtags);
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

        Post squirrelPost = new Post(R.drawable.squirrel_post, "Look at this little guy!", "Sparty",
                "W. J. Beal Botanical Garden", 42, now - TimeUnit.HOURS.toMillis(18));
        ArrayList<String> squirrelTags = new ArrayList<>();
        squirrelTags.add("campuslife"); squirrelTags.add("squirrels"); squirrelTags.add("cute");
        squirrelPost.setHashtags(squirrelTags);
        userPosts.add(squirrelPost);

        Post studyPost = new Post(R.drawable.online_class, "Late night study session.", "Zeke",
                "Online", 120, now - TimeUnit.HOURS.toMillis(6));
        ArrayList<String> studyTags = new ArrayList<>();
        studyTags.add("studygrind"); studyTags.add("finals"); studyTags.add("latenight");
        studyPost.setHashtags(studyTags);
        userPosts.add(studyPost);

        Post towerPost = new Post(R.drawable.beaumont_tower, "Campus is beautiful today.", "Jen",
                "Beaumont Tower", 75, now - TimeUnit.DAYS.toMillis(1));
        ArrayList<String> towerTags = new ArrayList<>();
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
