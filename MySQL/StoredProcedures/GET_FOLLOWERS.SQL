DELIMITER $$

DROP PROCEDURE IF EXISTS GET_FOLLOWERS $$
CREATE PROCEDURE GET_FOLLOWERS(IN p_user_id_of_leader INT)
BEGIN
  SELECT u.user_id,
         u.username,
         u.profile_pic_url   -- change to profile_url if your column is named that
  FROM followers f
  JOIN users u ON u.user_id = f.follower_id
  WHERE f.followed_id = p_user_id_of_leader
  ORDER BY u.username;
END $$
 