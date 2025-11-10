DELIMITER $$
 
DROP PROCEDURE IF EXISTS GET_ALL_USERS $$
CREATE PROCEDURE GET_ALL_USERS()
BEGIN
  SELECT user_id, username, bio, profile_url, created_at
  FROM users
  ORDER BY username;
END $$
 
DROP PROCEDURE IF EXISTS GET_USER $$
CREATE PROCEDURE GET_USER(IN p_username VARCHAR(255))
BEGIN
  SELECT user_id, username, bio, profile_url, created_at
  FROM users
  WHERE username = p_username;
END $$