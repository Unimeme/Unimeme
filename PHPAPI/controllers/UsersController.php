<?php
declare(strict_types=1);

final class UsersController
{
    private PDO $pdo;
    public function __construct(PDO $pdo){ $this->pdo = $pdo; }

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
            'username'    => $data['username']    ?? $data['UserName'] ?? null,
            'password'    => $data['password']    ?? $data['Password'] ?? null,
            'bio'         => $data['bio']         ?? $data['Bio']      ?? null,
            'profile_url' => $profileUrl,
        ];
    }

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
                "INSERT INTO users (username,password_hash,bio,profile_url,created_at)
                 VALUES (?,?,?,?,NOW())"
            );
            $stmt->execute([
                $in['username'],
                password_hash($in['password'], PASSWORD_DEFAULT),
                $in['bio'],
                $in['profile_url'],
            ]);
            echo json_encode(['ok'=>true,'id'=>$this->pdo->lastInsertId()]);
        } catch (Throwable $e) {
            error_log('UsersController::createUser FAIL: '.$e->getMessage());
            http_response_code(500);
            echo json_encode(['ok'=>false,'error'=>'internal']);
        }
    }

    public function updateUser(): void
    {
        $in = $this->readBody();
        if (empty($in['username'])) {
            http_response_code(400);
            echo json_encode(['ok'=>false,'error'=>'missing:username']);
            return;
        }

        try {
            $stmt = $this->pdo->prepare(
                "UPDATE users
                   SET bio = COALESCE(?, bio),
                       profile_url = COALESCE(?, profile_url)
                 WHERE username = ?"
            );
            $stmt->execute([
                $in['bio'],
                $in['profile_url'],
                $in['username'],
            ]);
            echo json_encode(['ok'=>true,'updated'=>$stmt->rowCount()]);
        } catch (Throwable $e) {
            error_log('UsersController::updateUser FAIL: '.$e->getMessage());
            http_response_code(500);
            echo json_encode(['ok'=>false,'error'=>'internal']);
        }
    }

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
            echo json_encode(['ok'=>true,'deleted'=>$stmt->rowCount()]);
        } catch (Throwable $e) {
            error_log('UsersController::deleteUser FAIL: '.$e->getMessage());
            http_response_code(500);
            echo json_encode(['ok'=>false,'error'=>'internal']);
        }
    }

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
            if (!$row) { http_response_code(404); echo json_encode(['ok'=>false,'error'=>'not_found']); return; }
            echo json_encode(['ok'=>true,'user'=>$row]);
        } catch (Throwable $e) {
            error_log('UsersController::getUser FAIL: '.$e->getMessage());
            http_response_code(500);
            echo json_encode(['ok'=>false,'error'=>'internal']);
        }
    }
}
