<?php
// declare(strict_types=1);

// use App\Security;
// use PDO;

final class UsersController {
    /** @var PDO */
    private \PDO $pdo;

    public function __construct(PDO $pdo) {
        $this->pdo = $pdo;
    }

    // 1) CreateUser (POST)
    // Body: { "UserName": string, "Password": string, "Bio"?: string, "PicURL"?: string }
    public function createUser(): void {
        $b   = json_decode(file_get_contents('php://input'), true) ?? [];
        $u   = trim((string)($b['UserName'] ?? ''));
        $p   = (string)($b['Password'] ?? '');
        $bio = (string)($b['Bio'] ?? '');
        $pic = (string)($b['PicURL'] ?? ''); // maps to profile_url

        if ($u === '' || $p === '') {
            http_response_code(400);
            echo json_encode(['IsSuccess' => false, 'error' => 'Missing required fields']);
            return;
        }

        $hash = \App\Security::hashPassword($p);
        $sql  = "INSERT INTO users (username, password_hash, bio, profile_url, created_at)
                 VALUES (:u, :ph, :bio, :pic, UTC_TIMESTAMP())";
        $stmt = $this->pdo->prepare($sql);
        $ok   = $stmt->execute([
            ':u'   => $u,
            ':ph'  => $hash,
            ':bio' => $bio !== '' ? $bio : null,
            ':pic' => $pic !== '' ? $pic : null
        ]);

        echo json_encode([
            'IsSuccess' => $ok,
            'userId'    => $ok ? (int)$this->pdo->lastInsertId() : null
        ]);
    }

    // 2) UpdateUser (POST) — partial update
    // Body: { "UserName": string, "Password"?: string, "Bio"?: string, "PicURL"?: string }
    public function updateUser(): void {
        $b   = json_decode(file_get_contents('php://input'), true) ?? [];
        $u   = trim((string)($b['UserName'] ?? ''));
        $p   = (string)($b['Password'] ?? '');
        $bio = (string)($b['Bio'] ?? '');
        $pic = (string)($b['PicURL'] ?? '');

        if ($u === '') {
            http_response_code(400);
            echo json_encode(['IsSuccess' => false, 'error' => 'UserName required']);
            return;
        }

        $fields = [];
        $params = [':u' => $u];

        if ($p !== '') {
            $fields[]       = 'password_hash = :ph';
            $params[':ph']  = \App\Security::hashPassword($p);
        }
        if ($bio !== '') {
            $fields[]       = 'bio = :bio';
            $params[':bio'] = $bio;
        }
        if ($pic !== '') {
            $fields[]        = 'profile_url = :pic';
            $params[':pic']  = $pic;
        }

        if (!$fields) {
            echo json_encode(['IsSuccess' => false, 'error' => 'Nothing to update']);
            return;
        }

        $sql = "UPDATE users SET ".implode(', ', $fields)." WHERE username = :u";
        $ok  = $this->pdo->prepare($sql)->execute($params);

        echo json_encode(['IsSuccess' => $ok]);
    }

    // 3) DeleteUser (DELETE) — body: username,password
    public function deleteUser(): void {
        $raw = file_get_contents('php://input');
        $b   = json_decode($raw, true);
        if (!is_array($b)) { parse_str($raw, $b); }

        $u = trim((string)($b['username'] ?? ''));
        $p = (string)($b['password'] ?? '');

        if ($u === '' || $p === '') {
            http_response_code(400);
            echo json_encode(['IsSuccess' => false, 'error' => 'bad_request']);
            return;
        }

        $stmt = $this->pdo->prepare("SELECT password_hash FROM users WHERE username = :u");
        $stmt->execute([':u' => $u]);
        $row = $stmt->fetch();

        if (!$row || !\App\Security::verifyPassword($p, (string)$row['password_hash'])) {
            echo json_encode(['IsSuccess' => false, 'error' => 'auth_failed']);
            return;
        }

        $ok = $this->pdo->prepare("DELETE FROM users WHERE username = :u")->execute([':u' => $u]);
        echo json_encode(['IsSuccess' => $ok]);
    }

    // 4a) GetAllUsers (GET)
    public function getAllUsers(): void {
        $rows = $this->pdo
            ->query("SELECT user_id, username, bio, profile_url, created_at FROM users ORDER BY user_id")
            ->fetchAll();
        echo json_encode(['Users' => $rows]);
    }

    // 4b) GetUser (GET ?username=)
    public function getUser(): void {
        $u = trim((string)($_GET['username'] ?? ''));
        $stmt = $this->pdo->prepare(
            "SELECT user_id, username, bio, profile_url, created_at
             FROM users WHERE username = :u"
        );
        $stmt->execute([':u' => $u]);
        echo json_encode($stmt->fetch() ?: []);
    }
}