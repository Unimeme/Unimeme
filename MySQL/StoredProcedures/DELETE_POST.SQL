DELIMITER $$
 
DROP PROCEDURE IF EXISTS DELETE_POST $$
CREATE PROCEDURE DELETE_POST(IN p_user_id INT, IN p_post_id INT)
BEGIN
  DELETE FROM comments WHERE post_id = p_post_id;
  DELETE FROM posts WHERE post_id = p_post_id AND user_id = p_user_id;
  SELECT ROW_COUNT() > 0 AS IsSuccess;
END $$
 