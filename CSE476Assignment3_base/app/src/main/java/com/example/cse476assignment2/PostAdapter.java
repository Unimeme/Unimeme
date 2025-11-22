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

import com.bumptech.glide.Glide;

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    public interface OnPostInteractionListener {
        void onCommentClick(Post post, int position);
        void onLikeClick(int position);
        void onHashtagClick(String hashtag);
        void onDeleteClick(int position);
    }

    private final List<Post> posts;
    private final OnPostInteractionListener interactionListener;

    // ✅ 서버 base URL (image_url이 "/cse476/...jpg" 처럼 올 때 붙여줌)
    private static final String BASE_URL = "https://www.egr.msu.edu";

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

    class PostViewHolder extends RecyclerView.ViewHolder {
        final TextView authorView, locationView, captionView, likesView, hashtagsView;
        final ImageView imageView;
        final ImageButton btnComment, btnLikePost, btnDeletePost;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            authorView = itemView.findViewById(R.id.postAuthor);
            locationView = itemView.findViewById(R.id.postLocation);
            imageView = itemView.findViewById(R.id.postImage);
            captionView = itemView.findViewById(R.id.postCaption);
            likesView = itemView.findViewById(R.id.postLikes);
            hashtagsView = itemView.findViewById(R.id.postHashtags);
            btnComment = itemView.findViewById(R.id.btnComment);
            btnLikePost = itemView.findViewById(R.id.btnLikePost);
            btnDeletePost = itemView.findViewById(R.id.btnDeletePost);
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
                Uri uri = post.getImageUri();
                String url = (uri != null) ? uri.toString() : "";

                if (url.startsWith("/")) {
                    url = BASE_URL + url;
                }

                Glide.with(itemView.getContext())
                        .load(url)
                        .into(imageView);
            }

            updateLikes(post);

            // Delete button
            if (listener != null) {
                if (post.getAuthor().equals(itemView.getContext().getString(R.string.you_as_user))) {
                    btnDeletePost.setVisibility(View.VISIBLE);
                    btnDeletePost.setOnClickListener(v -> listener.onDeleteClick(position));
                } else {
                    btnDeletePost.setVisibility(View.GONE);
                }
            } else {
                btnDeletePost.setVisibility(View.GONE);
            }

            btnLikePost.setOnClickListener(v -> {
                if (listener == null) return;
                post.toggleLike();
                updateLikes(post);
                listener.onLikeClick(position);
            });

            btnComment.setOnClickListener(v -> {
                if (listener != null) listener.onCommentClick(post, position);
            });

            // hashtags
            if (post.getHashtags() != null && !post.getHashtags().isEmpty()) {
                StringBuilder hashtagBuilder = new StringBuilder();
                for (String tag : post.getHashtags()) {
                    hashtagBuilder.append("#").append(tag).append(" ");
                }

                SpannableString spannableString =
                        new SpannableString(hashtagBuilder.toString().trim());

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
                            ds.setUnderlineText(false);
                        }
                    };
                    int endIndex = startIndex + currentTag.length();
                    spannableString.setSpan(
                            clickableSpan,
                            startIndex,
                            endIndex,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                    startIndex = endIndex + 1;
                }

                hashtagsView.setText(spannableString);
                hashtagsView.setMovementMethod(LinkMovementMethod.getInstance());
                hashtagsView.setVisibility(View.VISIBLE);
            } else {
                hashtagsView.setVisibility(View.GONE);
            }
        }

        private void updateLikes(Post post) {
            likesView.setText(itemView.getContext()
                    .getString(R.string.likes_format, post.getLikeCount()));

            if (post.isLikedByCurrentUser()) {
                btnLikePost.setImageResource(android.R.drawable.btn_star_big_on);
            } else {
                btnLikePost.setImageResource(android.R.drawable.btn_star_big_off);
            }
        }
    }
}
