DELIMITER $$
 

DROP PROCEDURE IF EXISTS CREATE_USER $$
CREATE PROCEDURE CREATE_USER(
  IN p_username      VARCHAR(255),
  IN p_password_hash VARCHAR(255),
  IN p_bio           TEXT,
  IN p_profile_url   VARCHAR(255)
)
BEGIN
  DECLARE v_exists INT DEFAULT 0;
 
  SELECT COUNT(*) INTO v_exists FROM users WHERE username = p_username;
 
  IF v_exists > 0 THEN
    SELECT FALSE AS IsSuccess, NULL AS user_id, 'USERNAME_EXISTS' AS message;
  ELSE
    INSERT INTO users (username, password_hash, bio, profile_url, created_at)
    VALUES (p_username, p_password_hash, p_bio, p_profile_url, UTC_TIMESTAMP());
    SELECT TRUE AS IsSuccess, LAST_INSERT_ID() AS user_id, 'OK' AS message;
  END IF;
END $$