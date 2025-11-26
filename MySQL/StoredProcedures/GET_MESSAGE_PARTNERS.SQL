DELIMITER $$

DROP PROCEDURE IF EXISTS GET_MESSAGE_PARTNERS $$
CREATE PROCEDURE GET_MESSAGE_PARTNERS(
    IN p_user_id INT
)
BEGIN
    SELECT DISTINCT
        IF(sender_id = p_user_id, receiver_id, sender_id) AS partner_id
    FROM messages
    WHERE sender_id = p_user_id
       OR receiver_id = p_user_id;
END $$

DELIMITER ;