// /app/src/main/java/com/example/cse476assignment2/net/PostMapper.java
package com.example.cse476assignment2.net;

import android.net.Uri;

import com.example.cse476assignment2.Comment;
import com.example.cse476assignment2.Post;
import com.example.cse476assignment2.R;
import com.example.cse476assignment2.model.CommentDto;
import com.example.cse476assignment2.model.PostDto;
import com.example.cse476assignment2.model.Res.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class PostMapper {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static Post fromDto(PostDto dto, String currentUsername) {

        Uri uri = dto.imageUrl != null ? Uri.parse(dto.imageUrl) : null;

        String authorName = (dto.author != null && dto.author.username != null)
                ? dto.author.username : "";

        if (authorName.equals(currentUsername)) authorName = "You";

        String locName = "";
        if (dto.location != null && dto.location.name != null) {
            locName = dto.location.name;
        }

        long createdAtMs = parseUtc(dto.createdAt);

        Post post = new Post(
                uri,
                dto.caption != null ? dto.caption : "",
                authorName,
                locName,
                0,
                createdAtMs
        );
        post.setPostId(dto.postId);

        if (dto.comments != null) {
            for (CommentDto c : dto.comments) {
                String cAuthor = c.username != null ? c.username : "";
                if (cAuthor.equals(currentUsername)) cAuthor = "You";

                String cText = c.content != null ? c.content : "";

                long cTimeMs = parseUtc(c.createdAt);

                Comment newComment = new Comment(
                        cAuthor,
                        cText,
                        R.drawable.profile_icon,
                        cTimeMs
                );
                newComment.setCommentId(c.commentId);
                post.addCommentObject(newComment);
            }
        }

        return post;
    }

    private static long parseUtc(String s) {
        if (s == null) return System.currentTimeMillis();
        try {
            LocalDateTime ldt = LocalDateTime.parse(s, FMT);
            Instant ins = ldt.toInstant(ZoneOffset.UTC);
            return ins.toEpochMilli();
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }


}

