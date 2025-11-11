DELIMITER $$

DROP PROCEDURE IF EXISTS INSERT_FOLLOWER $$
CREATE PROCEDURE INSERT_FOLLOWER(
  IN p_follower_id INT,
  IN p_followed_id INT
)
BEGIN
  IF p_follower_id = p_followed_id THEN
    SELECT FALSE AS IsSuccess, 'CANNOT_FOLLOW_SELF' AS message;
  ELSE
    INSERT IGNORE INTO followers (follower_id, followed_id, created_at)
    VALUES (p_follower_id, p_followed_id, UTC_TIMESTAMP());
 
    -- INSERT IGNORE: ROW_COUNT() = 1 (inserted), 0 (already existed)
    SELECT (ROW_COUNT() >= 0) AS IsSuccess,
           IF(ROW_COUNT()=0, 'ALREADY_FOLLOWING', 'OK') AS message;
  END IF;
END $$