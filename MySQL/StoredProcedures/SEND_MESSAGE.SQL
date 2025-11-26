DELIMITER $$

DROP PROCEDURE IF EXISTS SEND_MESSAGE $$
CREATE PROCEDURE SEND_MESSAGE(
    IN p_sender_id INT,
    IN p_receiver_id INT,
    IN p_content TEXT
)
BEGIN
    INSERT INTO messages (sender_id, receiver_id, content, created_at)
    VALUES (p_sender_id, p_receiver_id, p_content, UTC_TIMESTAMP());
END $$

DELIMITER ;
