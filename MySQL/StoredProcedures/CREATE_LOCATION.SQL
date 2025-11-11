DELIMITER $$

DROP PROCEDURE IF EXISTS CREATE_LOCATION $$
CREATE PROCEDURE CREATE_LOCATION(
  IN p_name      VARCHAR(100),
  IN p_latitude  DECIMAL(10,8),
  IN p_longitude DECIMAL(11,8)
)
BEGIN
  INSERT INTO locations (name, latitude, longitude)
  VALUES (p_name, p_latitude, p_longitude);
 
  SELECT TRUE AS IsSuccess, LAST_INSERT_ID() AS location_id;
END $$

 