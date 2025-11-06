// /app/src/main/java/com/example/cse476assignment2/PostAdapter.java
package com.example.cse476assignment2;

import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    // UPDATED: More general listener interface
    public interface OnPostInteractionListener {
        void onCommentClick(Post post, int position);
        void onLikeClick(int position); // NEW: For handling post likes
    }

    private final List<Post> posts;
    private final OnPostInteractionListener interactionListener;

    public PostAdapter(List<Post> posts, OnPostInteractionListener listener) {
        this.posts = posts;
        this.interactionListener = listener;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.post_item, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = posts.get(position);
        holder.bind(post, position, interactionListener);
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        final TextView authorView;
        final TextView locationView;
        final ImageView imageView;
        final TextView captionView;
        final TextView likesView;
        final ImageButton btnComment;
        final ImageButton btnLikePost; // NEW

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            authorView = itemView.findViewById(R.id.postAuthor);
            locationView = itemView.findViewById(R.id.postLocation);
            imageView = itemView.findViewById(R.id.postImage);
            captionView = itemView.findViewById(R.id.postCaption);
            likesView = itemView.findViewById(R.id.postLikes);
            btnComment = itemView.findViewById(R.id.btnComment);
            btnLikePost = itemView.findViewById(R.id.btnLikePost); // NEW
        }

        public void bind(Post post, int position, OnPostInteractionListener listener) {
            authorView.setText(post.getAuthor());
            locationView.setText(post.getLocation());
            locationView.setVisibility(TextUtils.isEmpty(post.getLocation()) ? View.GONE : View.VISIBLE);
            captionView.setText(post.getCaption());
            captionView.setVisibility(TextUtils.isEmpty(post.getCaption()) ? View.GONE : View.VISIBLE);

            if (post.getImageResId() != null) {
                imageView.setImageResource(post.getImageResId());
            } else {
                imageView.setImageURI(post.getImageUri());
            }

            // --- NEW: Post Like Logic ---
            updateLikes(post);

            btnLikePost.setOnClickListener(v -> {
                post.toggleLike();
                updateLikes(post);
                if (listener != null) {
                    listener.onLikeClick(position);
                }
            });
            // --- End Post Like Logic ---

            btnComment.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCommentClick(post, position);
                }
            });
        }

        private void updateLikes(Post post) {
            likesView.setText(itemView.getContext().getString(R.string.likes_format, post.getLikeCount()));
            if (post.isLikedByCurrentUser()) {
                btnLikePost.setImageResource(android.R.drawable.btn_star_big_on);
            } else {
                btnLikePost.setImageResource(android.R.drawable.btn_star_big_off);
            }
        }
    }
}