DELIMITER $$

DROP PROCEDURE IF EXISTS UPDATE_USER $$
CREATE PROCEDURE UPDATE_USER(
  IN p_username       VARCHAR(255),
  IN p_new_username   VARCHAR(255),
  IN p_password_hash  VARCHAR(255),
  IN p_bio            TEXT,
  IN p_profile_url    VARCHAR(255)
)
proc: BEGIN
  DECLARE v_target VARCHAR(255);

  SET v_target = NULLIF(TRIM(p_new_username), '');

  START TRANSACTION;

  IF NOT EXISTS (SELECT 1 FROM users WHERE username = p_username FOR UPDATE) THEN
    ROLLBACK;
    SELECT 0 AS ok, 'not_found' AS error, 0 AS updated, NULL AS new_username;
    LEAVE proc;
  END IF;

  IF v_target IS NOT NULL
     AND v_target <> p_username
     AND EXISTS (SELECT 1 FROM users WHERE username = v_target FOR UPDATE) THEN
    ROLLBACK;
    SELECT 0 AS ok, 'username_taken' AS error, 0 AS updated, NULL AS new_username;
    LEAVE proc;
  END IF;

  UPDATE users
     SET username      = COALESCE(v_target, username),
         password_hash = COALESCE(p_password_hash, password_hash),
         bio           = COALESCE(p_bio, bio),
         profile_url   = COALESCE(p_profile_url, profile_url)
   WHERE username = p_username;

  COMMIT;

  SELECT 1 AS ok,
         NULL AS error,
         ROW_COUNT() AS updated,
         COALESCE(v_target, p_username) AS new_username;
END $$

DELIMITER ;
