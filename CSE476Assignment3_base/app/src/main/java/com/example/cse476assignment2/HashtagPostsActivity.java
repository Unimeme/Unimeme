// /app/src/main/java/com/example/cse476assignment2/HashtagPostsActivity.java
package com.example.cse476assignment2;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class HashtagPostsActivity extends AppCompatActivity {

    public static final String EXTRA_HASHTAG = "EXTRA_HASHTAG";
    public static final String EXTRA_ALL_POSTS = "EXTRA_ALL_POSTS";

    private PostAdapter postAdapter;
    private List<Post> filteredPosts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hashtag_posts);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        RecyclerView recyclerView = findViewById(R.id.hashtagPostsRecyclerView);

        String hashtag = getIntent().getStringExtra(EXTRA_HASHTAG);
        Serializable postsSerializable = getIntent().getSerializableExtra(EXTRA_ALL_POSTS);
        List<Post> allPosts = (postsSerializable instanceof List) ? (List<Post>) postsSerializable : new ArrayList<>();

        toolbar.setTitle(hashtag);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Filter the posts to find ones containing the hashtag
        if (hashtag != null && !hashtag.isEmpty()) {
            // Remove the '#' prefix from the clicked tag for matching, as it's not stored in the Post object
            String cleanHashtag = hashtag.startsWith("#") ? hashtag.substring(1) : hashtag;
            for (Post post : allPosts) {
                if (post.getHashtags().contains(cleanHashtag)) {
                    filteredPosts.add(post);
                }
            }
        }

        // Sort by most recent
        Collections.sort(filteredPosts, (p1, p2) -> Long.compare(p2.getCreatedAt(), p1.getCreatedAt()));

        // We set the listener to null here because this screen doesn't need to handle
        // post interactions like the main feed does.
        postAdapter = new PostAdapter(filteredPosts, null);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(postAdapter);
    }
}