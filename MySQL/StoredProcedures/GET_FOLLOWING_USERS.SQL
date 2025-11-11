DELIMITER $$

DROP PROCEDURE IF EXISTS GET_FOLLOWING_USERS $$
CREATE PROCEDURE GET_FOLLOWING_USERS(IN p_user_id_of_follower INT)
BEGIN
  SELECT u.user_id,
         u.username,
         u.profile_pic_url   -- change to profile_url if needed
  FROM followers f
  JOIN users u ON u.user_id = f.followed_id
  WHERE f.follower_id = p_user_id_of_follower
  ORDER BY u.username;
END $$
 
DELIMITER ;