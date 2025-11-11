DELIMITER $$

DROP PROCEDURE IF EXISTS INSERT_COMMENT $$
CREATE PROCEDURE INSERT_COMMENT(
  IN p_commenter_id INT,
  IN p_post_id      INT,
  IN p_content      TEXT
)
BEGIN
  INSERT INTO comments (post_id, commenter_id, content, created_at)
  VALUES (p_post_id, p_commenter_id, p_content, UTC_TIMESTAMP());
 
  SELECT TRUE AS IsSuccess, LAST_INSERT_ID() AS comment_id;
END $$
 
DELIMITER ;