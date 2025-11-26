DELIMITER $$

DROP PROCEDURE IF EXISTS GET_MESSAGES_BETWEEN_USERS $$
CREATE PROCEDURE GET_MESSAGES_BETWEEN_USERS(
    IN p_user1 INT,
    IN p_user2 INT
)
BEGIN
    SELECT
        id,
        sender_id,
        receiver_id,
        content,
        created_at
    FROM messages
    WHERE (sender_id = p_user1 AND receiver_id = p_user2)
       OR (sender_id = p_user2 AND receiver_id = p_user1)
    ORDER BY created_at ASC;
END $$

DELIMITER ;
