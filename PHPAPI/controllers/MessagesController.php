<?php
final class MessagesController {

    private \PDO $pdo;

    public function __construct(PDO $pdo) {
        $this->pdo = $pdo;
    }

    // ---------------------------
    // Auth helper
    // ---------------------------
    private function authUser(string $username, string $password): ?array {
        $sql = "SELECT user_id, username, password_hash
                  FROM users
                 WHERE username = :u";
        $st = $this->pdo->prepare($sql);
        $st->execute([':u' => $username]);
        $r = $st->fetch(\PDO::FETCH_ASSOC);

        return ($r && \App\Security::verifyPassword($password, (string)$r['password_hash']))
            ? $r
            : null;
    }

    // =======================================================
    // 1) Send message (POST)
    // =======================================================
    public function sendMessage(): void {
        $body = json_decode(file_get_contents("php://input"), true) ?? [];

        $u   = trim((string)($body['username'] ?? ''));
        $p   = (string)($body['password'] ?? '');
        $recvU = trim((string)($body['receiverUsername'] ?? ''));
        $content = trim((string)($body['content'] ?? ''));

        if ($u === '' || $p === '' || $recvU === '' || $content === '') {
            http_response_code(400);
            echo json_encode(['IsSuccess'=>false, 'error'=>'bad_request']);
            return;
        }

        $me = $this->authUser($u, $p);
        if (!$me) {
            echo json_encode(['IsSuccess'=>false, 'error'=>'auth_failed']);
            return;
        }

        // find receiver_id
        $st = $this->pdo->prepare("SELECT user_id FROM users WHERE username = :u");
        $st->execute([':u'=>$recvU]);
        $recvId = $st->fetchColumn();

        if (!$recvId) {
            echo json_encode(['IsSuccess'=>false, 'error'=>'receiver_not_found']);
            return;
        }

        $sql = "INSERT INTO messages (sender_id, receiver_id, content, created_at)
                VALUES (:sid, :rid, :c, UTC_TIMESTAMP())";
        $ok = $this->pdo->prepare($sql)->execute([
            ':sid' => (int)$me['user_id'],
            ':rid' => (int)$recvId,
            ':c'   => $content
        ]);

        echo json_encode([
            'IsSuccess' => $ok,
            'messageId' => $ok ? (int)$this->pdo->lastInsertId() : null
        ]);
    }

    // =======================================================
    // 2) Get thread between 2 users (GET)
    // =======================================================
    public function getThread(): void {
        $u = trim((string)($_GET['username'] ?? ''));
        $p = (string)($_GET['password'] ?? '');
        $partnerUsername = trim((string)($_GET['partner'] ?? ''));

        if ($u === '' || $p === '' || $partnerUsername === '') {
            http_response_code(400);
            echo json_encode(['Messages'=>[]]);
            return;
        }

        $me = $this->authUser($u, $p);
        if (!$me) {
            echo json_encode(['Messages'=>[]]);
            return;
        }

        // find partner_id
        $st = $this->pdo->prepare("SELECT user_id FROM users WHERE username = :u");
        $st->execute([':u'=>$partnerUsername]);
        $partnerId = $st->fetchColumn();

        if (!$partnerId) {
            echo json_encode(['Messages'=>[]]);
            return;
        }

        // FIX — all placeholders must have unique names
        $sql = "
            SELECT message_id, sender_id, receiver_id, content, created_at
              FROM messages
             WHERE (sender_id = :me1 AND receiver_id = :p1)
                OR (sender_id = :p2 AND receiver_id = :me2)
             ORDER BY created_at ASC
        ";

        $st2 = $this->pdo->prepare($sql);
        $st2->execute([
            ':me1' => (int)$me['user_id'],
            ':p1'  => (int)$partnerId,
            ':p2'  => (int)$partnerId,
            ':me2' => (int)$me['user_id']
        ]);

        echo json_encode(['Messages' => $st2->fetchAll(\PDO::FETCH_ASSOC)]);
    }

    // =======================================================
    // 3) Get all chat partners (GET)
    // =======================================================
    public function getPartners(): void {
        $u = trim((string)($_GET['username'] ?? ''));
        $p = (string)($_GET['password'] ?? '');

        if ($u === '' || $p === '') {
            http_response_code(400);
            echo json_encode(['Partners'=>[]]);
            return;
        }

        $me = $this->authUser($u, $p);
        if (!$me) {
            echo json_encode(['Partners'=>[]]);
            return;
        }

        $myId = (int)$me['user_id'];

        // FIX — duplicated :me placeholder removed
        $sql = "
            SELECT DISTINCT other.user_id, other.username
            FROM (
                SELECT receiver_id AS user_id FROM messages WHERE sender_id = :me1
                UNION
                SELECT sender_id AS user_id FROM messages WHERE receiver_id = :me2
            ) AS x
            JOIN users other ON other.user_id = x.user_id
            ORDER BY other.username ASC
        ";

        $st = $this->pdo->prepare($sql);
        $st->execute([
            ':me1' => $myId,
            ':me2' => $myId
        ]);

        echo json_encode(['Partners'=>$st->fetchAll(\PDO::FETCH_ASSOC)]);
    }
}
