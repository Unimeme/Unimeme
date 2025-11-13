DELIMITER $$

DROP PROCEDURE IF EXISTS UPDATE_USER $$
CREATE PROCEDURE UPDATE_USER(
  IN p_username     VARCHAR(255),
  IN p_bio          TEXT,
  IN p_new_username VARCHAR(255)
)
proc:BEGIN
  DECLARE v_conflict INT DEFAULT 0;

  IF p_new_username IS NOT NULL AND p_new_username <> '' AND p_new_username <> p_username THEN
    SELECT COUNT(*) INTO v_conflict
      FROM users
     WHERE username = p_new_username;

    IF v_conflict > 0 THEN
      SELECT 0 AS IsSuccess, 'username_taken' AS Error;
      LEAVE proc;
    END IF;
  END IF;

  UPDATE users
     SET bio      = COALESCE(p_bio, bio),
         username = COALESCE(NULLIF(p_new_username, ''), username)
   WHERE username = p_username;

  SELECT (ROW_COUNT() > 0) AS IsSuccess, NULL AS Error;
END $$
DELIMITER ;
