DELIMITER $$
 

DROP PROCEDURE IF EXISTS CREATE_POST $$
CREATE PROCEDURE CREATE_POST(
  IN p_user_id     INT,
  IN p_image_url   VARCHAR(255),
  IN p_caption     TEXT,
  IN p_location_id INT
)
BEGIN
  INSERT INTO posts (user_id, location_id, image_url, caption, created_at)
  VALUES (p_user_id, p_location_id, p_image_url, p_caption, UTC_TIMESTAMP());
 
  SELECT TRUE AS IsSuccess, LAST_INSERT_ID() AS post_id;
END $$