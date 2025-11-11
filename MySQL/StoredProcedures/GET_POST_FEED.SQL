DELIMITER $$

DROP PROCEDURE IF EXISTS GET_POST_FEED $$
CREATE PROCEDURE GET_POST_FEED(
  IN p_user_id INT,
  IN p_limit   INT,
  IN p_afterId INT
)
BEGIN
  -- bound limit
  SET p_limit = IFNULL(p_limit, 20);
  IF p_limit < 1 THEN SET p_limit = 1; END IF;
  IF p_limit > 50 THEN SET p_limit = 50; END IF;
 
  -- temp table of selected post_ids
  CREATE TEMPORARY TABLE IF NOT EXISTS tmp_feed_posts (
    post_id INT PRIMARY KEY
  ) ENGINE=Memory;
 
  TRUNCATE TABLE tmp_feed_posts;
 
  INSERT INTO tmp_feed_posts (post_id)
  SELECT p.post_id
  FROM posts p
  WHERE p.user_id IN (
      SELECT followed_id FROM followers WHERE follower_id = p_user_id
      UNION SELECT p_user_id
  )
  AND (p_afterId = 0 OR p.post_id < p_afterId)
  ORDER BY p.post_id DESC
  LIMIT p_limit;
 
  /* 1) Posts result set: core post fields + author's username and optional location info */
  SELECT p.post_id,
         p.user_id,
         u.username,
         p.location_id,
         l.name       AS location_name,
         p.image_url,
         p.caption,
         p.created_at
  FROM posts p
  JOIN tmp_feed_posts t ON t.post_id = p.post_id
  JOIN users u          ON u.user_id = p.user_id
  LEFT JOIN locations l ON l.location_id = p.location_id
  ORDER BY p.post_id DESC;
 
  /* 2) Comments result set for those posts */
  SELECT c.comment_id,
         c.post_id,
         c.user_id,
         u.username,
         c.content,
         c.created_at
  FROM comments c
  JOIN users u ON u.user_id = c.user_id
  WHERE c.post_id IN (SELECT post_id FROM tmp_feed_posts)
  ORDER BY c.post_id, c.created_at;
 
  DROP TEMPORARY TABLE IF EXISTS tmp_feed_posts;
END $$
 
DELIMITER ;
