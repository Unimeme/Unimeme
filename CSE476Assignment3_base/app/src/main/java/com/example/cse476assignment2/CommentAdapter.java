// /app/src/main/java/com/example/cse476assignment2/CommentAdapter.java
package com.example.cse476assignment2;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private final List<Comment> comments;

    public CommentAdapter(List<Comment> comments) {
        this.comments = comments;
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
        holder.bind(comment);
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

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            authorImageView = itemView.findViewById(R.id.commentAuthorImage);
            authorTextView = itemView.findViewById(R.id.commentAuthor);
            timestampTextView = itemView.findViewById(R.id.commentTimestamp);
            commentTextView = itemView.findViewById(R.id.commentText);
        }

        public void bind(Comment comment) {
            authorTextView.setText(comment.getAuthor());
            commentTextView.setText(comment.getText());
            authorImageView.setImageResource(comment.getAuthorProfileImageResId());

            // Format the timestamp into a relative string (e.g., "5 minutes ago")
            CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                    comment.getTimestamp(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
            );
            timestampTextView.setText(relativeTime);
        }
    }
}