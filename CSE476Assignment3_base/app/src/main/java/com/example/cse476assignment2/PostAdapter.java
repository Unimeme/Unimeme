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

    // --- NEW: Interface for handling clicks ---
    public interface OnCommentClickListener {
        void onCommentClick(Post post, int position);
    }

    private final List<Post> posts;
    private final OnCommentClickListener commentClickListener; // NEW: Listener instance

    public PostAdapter(List<Post> posts, OnCommentClickListener listener) { // NEW: Updated constructor
        this.posts = posts;
        this.commentClickListener = listener;
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

        // --- UPDATED: Use the listener for the click event ---
        holder.btnComment.setOnClickListener(v -> {
            if (commentClickListener != null) {
                commentClickListener.onCommentClick(post, position);
            }
        });
        // --- End of updated OnClickListener setup ---

        holder.authorView.setText(post.getAuthor());

        String location = post.getLocation();
        if (TextUtils.isEmpty(location)) {
            holder.locationView.setVisibility(View.GONE);
        } else {
            holder.locationView.setText(location);
            holder.locationView.setVisibility(View.VISIBLE);
        }

        if (post.getImageResId() != null) {
            holder.imageView.setImageResource(post.getImageResId());
        } else {
            Uri imageUri = post.getImageUri();
            if (imageUri != null) {
                holder.imageView.setImageURI(imageUri);
            } else {
                holder.imageView.setImageDrawable(null);
            }
        }

        String caption = post.getCaption();
        if (TextUtils.isEmpty(caption)) {
            holder.captionView.setVisibility(View.GONE);
        } else {
            holder.captionView.setText(caption);
            holder.captionView.setVisibility(View.VISIBLE);
        }

        holder.likesView.setText(holder.itemView.getContext().getString(R.string.likes_format, post.getLikeCount()));
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

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            authorView = itemView.findViewById(R.id.postAuthor);
            locationView = itemView.findViewById(R.id.postLocation);
            imageView = itemView.findViewById(R.id.postImage);
            captionView = itemView.findViewById(R.id.postCaption);
            likesView = itemView.findViewById(R.id.postLikes);
            btnComment = itemView.findViewById(R.id.btnComment);
        }
    }
}