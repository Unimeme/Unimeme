<?php
declare(strict_types=1);
 
use App\Security;
 //use pdo;

final class PostsController
{
    public function __construct(private PDO $pdo) {}
        /** Utility: authenticate and return user row or null */
    private function authUser(string $username, string $password): ?array
    {
        $stmt = $this->pdo->prepare(
            "SELECT user_id, username, password_hash, profile_url
             FROM users WHERE username = :u"
        );
        $stmt->execute([':u' => $username]);
        $row = $stmt->fetch();
        if (!$row) return null;
        if (!Security::verifyPassword($password, $row['password_hash'])) return null;
        return $row;
    }
 
    /** 5) CreatePost (POST)
     *  Body: { username, password, imageUrl, caption, locationId }
     *  Returns: { IsSuccess: bool, postId?: int }
     */
    public function createPost(): void
    {
        $body = json_decode(file_get_contents('php://input'), true);
        $u  = trim($body['username'] ?? '');
        $p  = (string)($body['password'] ?? '');
        $img= trim($body['imageUrl'] ?? '');
        $cap= (string)($body['caption'] ?? '');
        $loc= $body['locationId'] ?? null;
 
        if ($u === '' || $p === '' || $img === '') {
            http_response_code(400);
            echo json_encode(['IsSuccess' => false, 'error' => 'username, password, imageUrl required']);
            return;
        }
 
        $user = $this->authUser($u, $p);
        if (!$user) { echo json_encode(['IsSuccess' => false, 'error' => 'auth_failed']); return; }
 
        $sql = "INSERT INTO posts (user_id, location_id, image_url, caption, created_at)
                VALUES (:uid, :loc, :img, :cap, UTC_TIMESTAMP())";
        $stmt = $this->pdo->prepare($sql);
        $ok = $stmt->execute([
            ':uid' => $user['user_id'],
            ':loc' => $loc !== null ? (int)$loc : null,
            ':img' => $img,
            ':cap' => $cap
        ]);
        echo json_encode(['IsSuccess' => $ok, 'postId' => $ok ? (int)$this->pdo->lastInsertId() : null]);
    }
 
    /** 6) DeletePost (DELETE)
     *  Body (x-www-form-urlencoded or JSON): username, password, postId
     *  Only the owner can delete.
     */
    public function deletePost(): void
    {
        // Accept either JSON body or form
        $raw = file_get_contents('php://input');
        $body = json_decode($raw, true);
        if (!is_array($body)) { parse_str($raw, $body); }
 
        $u = trim($body['username'] ?? '');
        $p = (string)($body['password'] ?? '');
        $postId = (int)($body['postId'] ?? 0);
 
        if ($u === '' || $p === '' || $postId <= 0) {
            http_response_code(400);
            echo json_encode(['IsSuccess' => false, 'error' => 'bad_request']);
            return;
        }
 
        $user = $this->authUser($u, $p);
        if (!$user) { echo json_encode(['IsSuccess' => false, 'error' => 'auth_failed']); return; }
 
        // Verify ownership
        $own = $this->pdo->prepare("SELECT 1 FROM posts WHERE post_id=:pid AND user_id=:uid");
        $own->execute([':pid' => $postId, ':uid' => $user['user_id']]);
        if (!$own->fetchColumn()) {
            echo json_encode(['IsSuccess' => false, 'error' => 'not_owner']); return;
        }
 
        // Optional: delete comments first (FK ON DELETE CASCADE also works)
        $this->pdo->beginTransaction();
        try {
            $this->pdo->prepare("DELETE FROM comments WHERE post_id=:pid")->execute([':pid' => $postId]);
            $ok = $this->pdo->prepare("DELETE FROM posts WHERE post_id=:pid")->execute([':pid' => $postId]);
            $this->pdo->commit();
            echo json_encode(['IsSuccess' => $ok]);
        } catch (\Throwable $e) {
            $this->pdo->rollBack();
            http_response_code(500);
            echo json_encode(['IsSuccess' => false, 'error' => 'delete_failed']);
        }
    }
 
    /** 7) GetPostFeed (GET)
     *  Query: ?username=&password=&limit=20&afterId=
     *  Returns posts from users the requester follows (plus their own), with comments.
     */
    public function getPostFeed(): void
    {
        $u = trim($_GET['username'] ?? '');
        $p = (string)($_GET['password'] ?? '');
        $limit = max(1, min(50, (int)($_GET['limit'] ?? 20)));
        $afterId = (int)($_GET['afterId'] ?? 0); // for pagination (older-than this ID)
 
        $user = $this->authUser($u, $p);
        if (!$user) { echo json_encode(['error' => 'auth_failed']); return; }
 
        // Build list of authors: followed + self
        $authors = $this->pdo->prepare(
            "SELECT followed_id AS author_id FROM followers WHERE follower_id = :uid
             UNION SELECT :uid"
        );
        $authors->execute([':uid' => $user['user_id']]);
        $authorIds = array_map(fn($r) => (int)$r['author_id'], $authors->fetchAll());
        if (empty($authorIds)) { echo json_encode(['posts' => []]); return; }
 
        // Prepare placeholders
        $ph = implode(',', array_fill(0, count($authorIds), '?'));
 
        $sql = "SELECT p.post_id, p.user_id, p.location_id, p.image_url, p.caption, p.created_at,
                       u.username, u.profile_pic_url,
                       l.name AS location_name, l.latitude, l.longitude
                FROM posts p
                JOIN users u ON u.user_id = p.user_id
                LEFT JOIN locations l ON l.location_id = p.location_id
                WHERE p.user_id IN ($ph) " .
                ($afterId > 0 ? "AND p.post_id < ? " : "") .
                "ORDER BY p.post_id DESC
                 LIMIT ?";
 
        $params = $authorIds;
        if ($afterId > 0) $params[] = $afterId;
        $params[] = $limit;
 
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute($params);
        $posts = $stmt->fetchAll();
 
        if (!$posts) { echo json_encode(['posts' => []]); return; }
 
        // Fetch comments for those posts
        $postIds = array_map(fn($r) => (int)$r['post_id'], $posts);
        $ph2 = implode(',', array_fill(0, count($postIds), '?'));
        $cstmt = $this->pdo->prepare(
            "SELECT c.comment_id, c.post_id, c.user_id, c.content, c.created_at, u.username
             FROM comments c
             JOIN users u ON u.user_id = c.user_id
             WHERE c.post_id IN ($ph2)
             ORDER BY c.created_at ASC"
        );
        $cstmt->execute($postIds);
        $comments = $cstmt->fetchAll();
 
        // Group comments by post_id
        $byPost = [];
        foreach ($comments as $c) {
            $pid = (int)$c['post_id'];
            $byPost[$pid][] = [
                'comment_id' => (int)$c['comment_id'],
                'post_id'    => $pid,
                'user_id'    => (int)$c['user_id'],
                'username'   => $c['username'],
                'content'    => $c['content'],
                'created_at' => $c['created_at'],
            ];
        }
 
        // Shape final payload
        $out = [];
        foreach ($posts as $p) {
            $pid = (int)$p['post_id'];
            $out[] = [
                'post_id'    => $pid,
                'created_at' => $p['created_at'],
                'image_url'  => $p['image_url'],
                'caption'    => $p['caption'],
                'author'     => [
                    'user_id'  => (int)$p['user_id'],
                    'username' => $p['username'],
                    'pic'      => $p['profile_pic_url']
                ],
                'location'   => $p['location_id'] ? [
                    'location_id' => (int)$p['location_id'],
                    'name'        => $p['location_name'],
                    'latitude'    => $p['latitude'],
                    'longitude'   => $p['longitude']
                ] : null,
                'comments'   => $byPost[$pid] ?? []
            ];
        }
 
        echo json_encode(['posts' => $out, 'nextAfterId' => min($postIds)]);
    }
}
