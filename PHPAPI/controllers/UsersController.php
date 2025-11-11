<?php
declare(strict_types=1);
 
use App\Security;
use PDO;
 
final class UsersController {
    public function __construct(private PDO $pdo) {}
 
    //  CreateUser (POST)
    public function createUser(): void {
        $body = json_decode(file_get_contents('php://input'), true);
        $u = trim($body['UserName'] ?? '');
        $e = trim($body['Email'] ?? '');
        $p = trim($body['Password'] ?? '');
        $b = trim($body['Bio'] ?? '');
        $pic = trim($body['PicURL'] ?? '');
        //Encrypt Plain Text Passwrod to Hash value
        if ($u === '' || $e === '' || $p === '') {
            http_response_code(400);
            echo json_encode(['IsSuccess' => false, 'error' => 'Missing required fields']);
            return;
        }
 
        //Insert new user record with hash to MySQL
        $hash = Security::hashPassword($p);
        $sql = "INSERT INTO users (username, email, password_hash, bio, profile_pic_url, created_at)
                VALUES (:u, :e, :p, :b, :pic, NOW())";
        $stmt = $this->pdo->prepare($sql);
        $ok = $stmt->execute([
            ':u' => $u, ':e' => $e, ':p' => $hash, ':b' => $b, ':pic' => $pic
        ]);
 
        echo json_encode(['IsSuccess' => $ok]);
    }
 
    //  UpdateUser (POST)
    public function updateUser(): void {
        $body = json_decode(file_get_contents('php://input'), true);
        $u = trim($body['UserName'] ?? '');
        $e = trim($body['Email'] ?? '');
        $p = trim($body['Password'] ?? '');
        $b = trim($body['Bio'] ?? '');
        $pic = trim($body['PicURL'] ?? '');
 
        if ($u === '') {
            http_response_code(400);
            echo json_encode(['IsSuccess' => false]);
            return;
        }
 
        $fields = [];
        $params = [':u' => $u];
        if ($e !== '') { $fields[] = 'email = :e'; $params[':e'] = $e; }
        if ($p !== '') { $fields[] = 'password_hash = :p'; $params[':p'] = Security::hashPassword($p); }
        if ($b !== '') { $fields[] = 'bio = :b'; $params[':b'] = $b; }
        if ($pic !== '') { $fields[] = 'profile_pic_url = :pic'; $params[':pic'] = $pic; }
 
        if (empty($fields)) {
            echo json_encode(['IsSuccess' => false, 'error' => 'Nothing to update']);
            return;
        }
 
        $sql = "UPDATE users SET " . implode(', ', $fields) . " WHERE username = :u";
        $ok = $this->pdo->prepare($sql)->execute($params);
        echo json_encode(['IsSuccess' => $ok]);
    }
 
    //  DeleteUser (DELETE)
    public function deleteUser(): void {
        parse_str(file_get_contents('php://input'), $body);
        $u = trim($body['username'] ?? '');
        $p = trim($body['password'] ?? '');
        if ($u === '' || $p === '') { echo json_encode(['IsSuccess' => false]); return; }
 
        $stmt = $this->pdo->prepare("SELECT password_hash FROM users WHERE username=:u");
        $stmt->execute([':u'=>$u]);
        $row = $stmt->fetch();
        if (!$row || !Security::verifyPassword($p, $row['password_hash'])) {
            echo json_encode(['IsSuccess' => false]); return;
        }
 
        $ok = $this->pdo->prepare("DELETE FROM users WHERE username=:u")->execute([':u'=>$u]);
        echo json_encode(['IsSuccess'=>$ok]);
    }
 
    //  GetAllUsers (GET)
    public function getAllUsers(): void {
        $rows = $this->pdo->query("SELECT user_id, username, email, bio, profile_pic_url FROM users")->fetchAll();
        echo json_encode(['Users'=>$rows]);
    }
 
    // 4b GetUser (GET)
    public function getUser(): void {
        $username = $_GET['username'] ?? '';
        $stmt = $this->pdo->prepare(
            "SELECT user_id, username, email, bio, profile_pic_url, created_at
             FROM users WHERE username=:u"
        );
        $stmt->execute([':u'=>$username]);
        $user = $stmt->fetch();
        echo json_encode($user ?: []);
    }
}
 