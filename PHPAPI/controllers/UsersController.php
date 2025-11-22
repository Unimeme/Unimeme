<?php
declare(strict_types=1);

final class UsersController
{
    private PDO $pdo;

    public function __construct(PDO $pdo){
        $this->pdo = $pdo;
    }

    /**
     * Read JSON body first; if not JSON, fallback to $_POST.
     * Normalize field names from multiple clients.
     */
    private function readBody(): array {
        $raw  = file_get_contents('php://input') ?: '';
        $data = json_decode($raw, true);
        if (!is_array($data)) $data = $_POST;

        $profileUrl = $data['profile_url']
            ?? $data['profileUrl']
            ?? $data['picUrl']
            ?? $data['PicURL']
            ?? null;

        return [
            'username'     => $data['username']     ?? $data['UserName']    ?? null,
            'new_username' => $data['new_username'] ?? $data['newUsername'] ?? null,
            'password'     => $data['password']     ?? $data['Password']    ?? null,
            'bio'          => $data['bio']          ?? $data['Bio']         ?? null,
            'profile_url'  => $profileUrl,
        ];
    }

    /** ----------------------------
     *  POST /api/users/create
     *  Body: {username, password, bio?, profile_url?}
     *  Res:  {ok:true, id:<newId>}
     *        or {ok:false, error:'missing:...'|'username_taken'|'internal'}
     *  ---------------------------- */
    public function createUser(): void
    {
        $in = $this->readBody();

        foreach (['username','password'] as $k) {
            if (empty($in[$k])) {
                http_response_code(400);
                echo json_encode(['ok'=>false,'error'=>"missing:$k"]);
                return;
            }
        }

        try {
            $stmt = $this->pdo->prepare(
                "INSERT INTO users (username, password_hash, bio, profile_url, created_at)
                 VALUES (?,?,?,?,NOW())"
            );

            $stmt->execute([
                $in['username'],
                password_hash($in['password'], PASSWORD_DEFAULT),
                $in['bio'],
                $in['profile_url'],
            ]);

            echo json_encode([
                'ok' => true,
                'id' => $this->pdo->lastInsertId()
            ]);

        } catch (Throwable $e) {
            error_log('UsersController::createUser FAIL: '.$e->getMessage());

            // Duplicate username (UNIQUE constraint)
            $sqlState = method_exists($e, 'getCode') ? (string)$e->getCode() : '';
            if ($sqlState === '23000') {
                http_response_code(409);
                echo json_encode(['ok'=>false,'error'=>'username_taken']);
                return;
            }

            http_response_code(500);
            echo json_encode(['ok'=>false,'error'=>'internal']);
        }
    }

    /** ----------------------------
     *  POST /api/users/login
     *  Body: {username, password}
     *  Res:  {ok:true, user:{user_id,username,bio,profile_url,created_at}}
     *        or {ok:false,error:'invalid_credentials'|'missing:...'|'internal'}
     *  ---------------------------- */
    public function login(): void
    {
        $in = $this->readBody();

        foreach (['username','password'] as $k) {
            if (empty($in[$k])) {
                http_response_code(400);
                echo json_encode(['ok'=>false,'error'=>"missing:$k"]);
                return;
            }
        }

        try {
            $stmt = $this->pdo->prepare(
                "SELECT user_id, username, password_hash, bio, profile_url, created_at
                   FROM users
                  WHERE username = ?"
            );
            $stmt->execute([$in['username']]);
            $row = $stmt->fetch();

            if (!$row || !password_verify($in['password'], (string)$row['password_hash'])) {
                http_response_code(401);
                echo json_encode(['ok'=>false,'error'=>'invalid_credentials']);
                return;
            }

            $user = [
                'user_id'     => (int)$row['user_id'],
                'username'    => (string)$row['username'],
                'bio'         => $row['bio'],
                'profile_url' => $row['profile_url'],
                'created_at'  => $row['created_at'],
            ];

            echo json_encode(['ok'=>true, 'user'=>$user]);

        } catch (Throwable $e) {
            error_log('UsersController::login FAIL: '.$e->getMessage());
            http_response_code(500);
            echo json_encode(['ok'=>false,'error'=>'internal']);
        }
    }

    /** ----------------------------
     *  POST /api/users/update
     *  Body: {username, new_username?, bio?}
     *  Res:  {ok:true, updated:<count>}
     *        or {ok:false,error:'missing:username'|'username_taken'|'internal'}
     *  ---------------------------- */
    public function updateUser(): void
    {
        $in = $this->readBody();

        if (empty($in['username'])) {
            http_response_code(400);
            echo json_encode(['ok'=>false,'error'=>'missing:username']);
            return;
        }

        if ($in['bio'] === null && $in['new_username'] === null) {
            http_response_code(400);
            echo json_encode(['ok'=>false,'error'=>'nothing_to_update']);
            return;
        }

        // Check new username duplication if it changes
        if (!empty($in['new_username']) && $in['new_username'] !== $in['username']) {
            try {
                $check = $this->pdo->prepare(
                    "SELECT COUNT(*) AS cnt FROM users WHERE username = ?"
                );
                $check->execute([$in['new_username']]);
                $row = $check->fetch();

                if ($row && (int)$row['cnt'] > 0) {
                    http_response_code(409);
                    echo json_encode(['ok'=>false,'error'=>'username_taken']);
                    return;
                }
            } catch (Throwable $e) {
                error_log('UsersController::updateUser username check FAIL: '.$e->getMessage());
                http_response_code(500);
                echo json_encode(['ok'=>false,'error'=>'internal']);
                return;
            }
        }

        // Build dynamic UPDATE fields (bio + username only)
        $fields = [];
        $params = [];

        if ($in['bio'] !== null) {
            $fields[] = 'bio = ?';
            $params[] = $in['bio'];
        }

        if (!empty($in['new_username'])) {
            $fields[] = 'username = ?';
            $params[] = $in['new_username'];
        }

        $params[] = $in['username']; // WHERE username = ?

        if (empty($fields)) {
            http_response_code(400);
            echo json_encode(['ok'=>false,'error'=>'nothing_to_update']);
            return;
        }

        $sql = 'UPDATE users SET ' . implode(', ', $fields) . ' WHERE username = ?';

        try {
            $stmt = $this->pdo->prepare($sql);
            $stmt->execute($params);

            echo json_encode([
                'ok'      => true,
                'updated' => $stmt->rowCount(),
            ]);
        } catch (Throwable $e) {
            error_log('UsersController::updateUser FAIL: '.$e->getMessage());
            http_response_code(500);
            echo json_encode(['ok'=>false,'error'=>'internal']);
        }
    }

    /** ----------------------------
     *  POST /api/users/delete
     *  Body: {username}
     *  Res:  {ok:true, deleted:<count>}
     *        or {ok:false,error:'missing:username'|'internal'}
     *  ---------------------------- */
    public function deleteUser(): void
    {
        $in = $this->readBody();

        if (empty($in['username'])) {
            http_response_code(400);
            echo json_encode(['ok'=>false,'error'=>'missing:username']);
            return;
        }

        try {
            $stmt = $this->pdo->prepare("DELETE FROM users WHERE username = ?");
            $stmt->execute([$in['username']]);

            echo json_encode([
                'ok'      => true,
                'deleted' => $stmt->rowCount()
            ]);

        } catch (Throwable $e) {
            error_log('UsersController::deleteUser FAIL: '.$e->getMessage());
            http_response_code(500);
            echo json_encode(['ok'=>false,'error'=>'internal']);
        }
    }

    /** ----------------------------
     *  GET /api/users/all
     *  Res: {ok:true, users:[...]}
     *  ---------------------------- */
    public function getAllUsers(): void
    {
        try {
            $rows = $this->pdo
                ->query("SELECT user_id,username,bio,profile_url,created_at FROM users ORDER BY user_id DESC")
                ->fetchAll();

            echo json_encode(['ok'=>true,'users'=>$rows]);

        } catch (Throwable $e) {
            error_log('UsersController::getAllUsers FAIL: '.$e->getMessage());
            http_response_code(500);
            echo json_encode(['ok'=>false,'error'=>'internal']);
        }
    }

    /** ----------------------------
     *  GET /api/users/get?username=...
     *  Res: {ok:true, user:{...}}
     *        or {ok:false,error:'missing:username'|'not_found'|'internal'}
     *  ---------------------------- */
    public function getUser(): void
    {
        $username = $_GET['username'] ?? null;

        if (!$username) {
            http_response_code(400);
            echo json_encode(['ok'=>false,'error'=>'missing:username']);
            return;
        }

        try {
            $stmt = $this->pdo->prepare(
                "SELECT user_id,username,bio,profile_url,created_at
                   FROM users WHERE username = ?"
            );
            $stmt->execute([$username]);
            $row = $stmt->fetch() ?: null;

            if (!$row) {
                http_response_code(404);
                echo json_encode(['ok'=>false,'error'=>'not_found']);
                return;
            }

            echo json_encode(['ok'=>true,'user'=>$row]);

        } catch (Throwable $e) {
            error_log('UsersController::getUser FAIL: '.$e->getMessage());
            http_response_code(500);
            echo json_encode(['ok'=>false,'error'=>'internal']);
        }
    }
}
