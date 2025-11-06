package com.example.cse476assignment2;

import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private final List<Post> posts;

    public PostAdapter(List<Post> posts) {
        this.posts = posts;
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

        holder.authorView.setText(post.getAuthor());

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
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        final TextView authorView;
        final ImageView imageView;
        final TextView captionView;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            authorView = itemView.findViewById(R.id.postAuthor);
            imageView = itemView.findViewById(R.id.postImage);
            captionView = itemView.findViewById(R.id.postCaption);
        }
    }
}
