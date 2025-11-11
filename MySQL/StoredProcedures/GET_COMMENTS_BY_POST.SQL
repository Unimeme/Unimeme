DELIMITER $$

DROP PROCEDURE IF EXISTS GET_COMMENTS_BY_POST $$
CREATE PROCEDURE GET_COMMENTS_BY_POST(IN p_post_id INT)
BEGIN
  SELECT
      c.comment_id,
      c.post_id,
      c.commenter_id,
      u.username,
      c.content,
      c.created_at
  FROM comments c
  JOIN users u ON u.user_id = c.commenter_id
  WHERE c.post_id = p_post_id
  ORDER BY c.created_at ASC;
END $$