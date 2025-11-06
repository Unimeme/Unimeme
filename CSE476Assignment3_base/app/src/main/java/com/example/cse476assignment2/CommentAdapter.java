// /app/src/main/java/com/example/cse476assignment2/CommentAdapter.java
package com.example.cse476assignment2;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    // UPDATED: More general listener for all interactions
    public interface OnCommentInteractionListener {
        void onLikeClicked();
        void onDeleteClicked(int position); // Pass position for deletion
    }

    private final List<Comment> comments;
    private final String currentUser;
    private final OnCommentInteractionListener interactionListener;

    public CommentAdapter(List<Comment> comments, String currentUser, OnCommentInteractionListener listener) {
        this.comments = comments;
        this.currentUser = currentUser;
        this.interactionListener = listener;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.comment_item, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);
        holder.bind(comment, currentUser, interactionListener);
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        private final ImageView authorImageView;
        private final TextView authorTextView;
        private final TextView timestampTextView;
        private final TextView commentTextView;
        private final ImageButton btnLikeComment;
        private final TextView tvCommentLikeCount;
        private final ImageButton btnDeleteComment; // NEW

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            authorImageView = itemView.findViewById(R.id.commentAuthorImage);
            authorTextView = itemView.findViewById(R.id.commentAuthor);
            timestampTextView = itemView.findViewById(R.id.commentTimestamp);
            commentTextView = itemView.findViewById(R.id.commentText);
            btnLikeComment = itemView.findViewById(R.id.btnLikeComment);
            tvCommentLikeCount = itemView.findViewById(R.id.tvCommentLikeCount);
            btnDeleteComment = itemView.findViewById(R.id.btnDeleteComment); // NEW
        }

        public void bind(Comment comment, String currentUser, OnCommentInteractionListener listener) {
            authorTextView.setText(comment.getAuthor());
            commentTextView.setText(comment.getText());
            authorImageView.setImageResource(comment.getAuthorProfileImageResId());

            CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                    comment.getTimestamp(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
            timestampTextView.setText(relativeTime);

            updateLikeButtonState(comment);
            tvCommentLikeCount.setText(String.valueOf(comment.getLikeCount()));

            btnLikeComment.setOnClickListener(v -> {
                comment.toggleLike();
                updateLikeButtonState(comment);
                tvCommentLikeCount.setText(String.valueOf(comment.getLikeCount()));
                if (listener != null) {
                    listener.onLikeClicked();
                }
            });

            // --- NEW: Delete button logic ---
            if (comment.getAuthor().equals(currentUser)) {
                btnDeleteComment.setVisibility(View.VISIBLE);
                btnDeleteComment.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDeleteClicked(getAdapterPosition());
                    }
                });
            } else {
                btnDeleteComment.setVisibility(View.GONE);
            }
        }

        private void updateLikeButtonState(Comment comment) {
            if (comment.isLikedByCurrentUser()) {
                btnLikeComment.setImageResource(android.R.drawable.btn_star_big_on);
            } else {
                btnLikeComment.setImageResource(android.R.drawable.btn_star_big_off);
            }
        }
    }
}