DELIMITER $$
 

DROP PROCEDURE IF EXISTS UPDATE_USER $$
CREATE PROCEDURE UPDATE_USER(
  IN p_username      VARCHAR(255),
  IN p_password_hash VARCHAR(255),
  IN p_bio           TEXT,
  IN p_profile_url   VARCHAR(255)
)
BEGIN
  UPDATE users
     SET password_hash = COALESCE(p_password_hash, password_hash),
         bio           = COALESCE(p_bio, bio),
         profile_url   = COALESCE(p_profile_url, profile_url)
   WHERE username = p_username;
 
  SELECT ROW_COUNT() > 0 AS IsSuccess;
END $$