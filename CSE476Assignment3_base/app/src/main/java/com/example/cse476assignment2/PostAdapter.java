// /app/src/main/java/com/example/cse476assignment2/PostAdapter.java
package com.example.cse476assignment2;

import android.net.Uri;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
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

    public interface OnPostInteractionListener {
        void onCommentClick(Post post, int position);
        void onLikeClick(int position);
        void onHashtagClick(String hashtag); // NEW
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
        final TextView authorView, locationView, captionView, likesView, hashtagsView; // UPDATED
        final ImageView imageView;
        final ImageButton btnComment, btnLikePost;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            authorView = itemView.findViewById(R.id.postAuthor);
            locationView = itemView.findViewById(R.id.postLocation);
            imageView = itemView.findViewById(R.id.postImage);
            captionView = itemView.findViewById(R.id.postCaption);
            likesView = itemView.findViewById(R.id.postLikes);
            hashtagsView = itemView.findViewById(R.id.postHashtags); // NEW
            btnComment = itemView.findViewById(R.id.btnComment);
            btnLikePost = itemView.findViewById(R.id.btnLikePost);
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

            updateLikes(post);
            btnLikePost.setOnClickListener(v -> {
                if (listener == null) return; // Prevent crash on hashtag screen
                post.toggleLike();
                updateLikes(post);
                listener.onLikeClick(position);
            });

            btnComment.setOnClickListener(v -> {
                if (listener != null) listener.onCommentClick(post, position);
            });

            // --- NEW: Hashtag Logic ---
            if (post.getHashtags() != null && !post.getHashtags().isEmpty()) {
                StringBuilder hashtagBuilder = new StringBuilder();
                for (String tag : post.getHashtags()) {
                    hashtagBuilder.append("#").append(tag).append(" ");
                }

                SpannableString spannableString = new SpannableString(hashtagBuilder.toString().trim());
                int startIndex = 0;
                for (String tag : post.getHashtags()) {
                    final String currentTag = "#" + tag;
                    ClickableSpan clickableSpan = new ClickableSpan() {
                        @Override
                        public void onClick(@NonNull View widget) {
                            if (listener != null) listener.onHashtagClick(currentTag);
                        }
                        @Override
                        public void updateDrawState(@NonNull TextPaint ds) {
                            super.updateDrawState(ds);
                            ds.setUnderlineText(false); // No underline
                        }
                    };
                    int endIndex = startIndex + currentTag.length();
                    spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    startIndex = endIndex + 1; // Move to the start of the next tag
                }

                hashtagsView.setText(spannableString);
                hashtagsView.setMovementMethod(LinkMovementMethod.getInstance()); // Make links clickable
                hashtagsView.setVisibility(View.VISIBLE);
            } else {
                hashtagsView.setVisibility(View.GONE);
            }
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