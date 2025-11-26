<?php
final class CommentsController {
    private \PDO $pdo;

    public function __construct(PDO $pdo) {
        $this->pdo = $pdo;
    }

    private function authUserRow(string $username,string $password): ?array {
        $s=$this->pdo->prepare("SELECT user_id,password_hash FROM users WHERE username=:u");
        $s->execute([':u'=>$username]);
        $r=$s->fetch();

        return ($r && \App\Security::verifyPassword($password,(string)$r['password_hash']))
            ? $r
            : null;
    }

    // ---------------------
    // 13) GET_COMMENTS_BY_POST
    // ---------------------
    public function getCommentsByPost(): void {
        $u = trim((string)($_GET['username'] ?? ''));
        $p = (string)($_GET['password'] ?? '');
        $postId = (int)($_GET['postId'] ?? 0);

        if ($u === '' || $p === '' || $postId <= 0) {
            http_response_code(400);
            echo json_encode(['Comments' => []]);
            return;
        }

        if (!$this->authUserRow($u, $p)) {
            echo json_encode(['Comments' => []]);
            return;
        }

        $stmt = $this->pdo->prepare(
            "SELECT c.comment_id,
                    c.post_id,
                    c.commenter_user_id AS user_id,
                    u.username,
                    c.content,
                    c.created_at
               FROM comments c
               JOIN users u ON u.user_id = c.commenter_user_id
              WHERE c.post_id = :pid
              ORDER BY c.created_at ASC"
        );

        $stmt->execute([':pid' => $postId]);
        echo json_encode(['Comments' => $stmt->fetchAll()]);
    }

    // ---------------------
    // 14) INSERT_COMMENT
    // ---------------------
    public function addComment(): void {
        $b = json_decode(file_get_contents('php://input'), true) ?? [];

        $u = trim((string)($b['username'] ?? ''));
        $p = (string)($b['password'] ?? '');
        $postId = (int)($b['postId'] ?? 0);

        $content = trim((string)($b['Comment'] ?? $b['comment'] ?? ''));

        if ($u === '' || $p === '' || $postId <= 0 || $content === '') {
            http_response_code(400);
            echo json_encode(['IsSuccess' => false, 'error' => 'bad_request']);
            return;
        }

        $me = $this->authUserRow($u, $p);
        if (!$me) {
            echo json_encode(['IsSuccess' => false, 'error' => 'auth_failed']);
            return;
        }

        $commenterId = (int)$me['user_id'];

        $ok = $this->pdo->prepare(
            "INSERT INTO comments (post_id, commenter_user_id, content, created_at)
             VALUES (:pid, :uid, :c, UTC_TIMESTAMP())"
        )->execute([
            ':pid' => $postId,
            ':uid' => $commenterId,
            ':c'   => $content
        ]);

        echo json_encode([
            'IsSuccess' => $ok,
            'commentId' => $ok ? (int)$this->pdo->lastInsertId() : null
        ]);
    }

    // ---------------------
    // 15) DELETE_COMMENT
    // ---------------------
    public function deleteComment(): void {
        $b = json_decode(file_get_contents('php://input'), true) ?? [];

        $u = trim((string)($b['username'] ?? ''));
        $p = (string)($b['password'] ?? '');
        $commentId = (int)($b['commentId'] ?? 0);

        if ($u === '' || $p === '' || $commentId <= 0) {
            http_response_code(400);
            echo json_encode(['IsSuccess' => false, 'error' => 'bad_request']);
            return;
        }

        $me = $this->authUserRow($u, $p);
        if (!$me) {
            echo json_encode(['IsSuccess' => false, 'error' => 'auth_failed']);
            return;
        }

        $uid = (int)$me['user_id'];

        // Only delete own comment
        $stmt = $this->pdo->prepare(
            "DELETE FROM comments
             WHERE comment_id = :cid AND commenter_user_id = :uid"
        );

        $ok = $stmt->execute([
            ':cid' => $commentId,
            ':uid' => $uid
        ]);

        echo json_encode([
            'IsSuccess' => $ok
        ]);
    }
}