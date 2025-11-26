<?php
// PostsController.php

final class PostsController {
    private \PDO $pdo;

    public function __construct(PDO $pdo) {
        $this->pdo = $pdo;
    }

    // ==========================
    // Auth helper
    // ==========================
    private function authUser(string $username, string $password): ?array {
        $stmt = $this->pdo->prepare(
            "SELECT user_id, username, password_hash
               FROM users
              WHERE username = :u"
        );
        $stmt->execute([':u' => $username]);
        $row = $stmt->fetch(PDO::FETCH_ASSOC);

        return ($row && \App\Security::verifyPassword($password, $row['password_hash']))
            ? $row
            : null;
    }

    // =========================================================
    // 5) CreatePost (POST)
    // Body: { username, password, imageUrl, caption?, locationId? }
    // =========================================================
    public function createPost(): void {
        $raw = file_get_contents('php://input') ?: '';
        $b   = json_decode($raw, true);
        if (!is_array($b)) {
            $b = $_POST;
        }

        $u   = trim((string)($b['username']  ?? ''));
        $p   = (string)($b['password'] ?? '');
        $img = trim((string)($b['imageUrl'] ?? ''));
        $cap = (string)($b['caption']  ?? '');
        $loc = $b['locationId'] ?? null;

        if ($u === '' || $p === '' || $img === '') {
            http_response_code(400);
            echo json_encode(['IsSuccess'=>false,'postId'=>null,'error'=>'missing_fields']);
            return;
        }

        $user = $this->authUser($u, $p);
        if (!$user) {
            http_response_code(401);
            echo json_encode(['IsSuccess'=>false,'postId'=>null,'error'=>'auth_failed']);
            return;
        }

        try {
            $stmt = $this->pdo->prepare(
                "INSERT INTO posts (user_id, location_id, image_url, caption, created_at)
                 VALUES (:uid, :loc, :img, :cap, UTC_TIMESTAMP())"
            );

            $stmt->bindValue(':uid', (int)$user['user_id'], PDO::PARAM_INT);
            $stmt->bindValue(':img', $img);
            $stmt->bindValue(':cap', $cap);

            if ($loc === null || $loc === '') {
                $stmt->bindValue(':loc', null, PDO::PARAM_NULL);
            } else {
                $stmt->bindValue(':loc', (int)$loc, PDO::PARAM_INT);
            }

            $stmt->execute();
            $postId = (int)$this->pdo->lastInsertId();

            echo json_encode(['IsSuccess'=>true,'postId'=>$postId,'error'=>null]);

        } catch (\Throwable $e) {
            error_log("createPost FAIL: ".$e->getMessage());
            http_response_code(500);
            echo json_encode(['IsSuccess'=>false,'postId'=>null,'error'=>'internal']);
        }
    }

    // =========================================================
    // 5-1) Upload Post Image (multipart/form-data)
    // =========================================================
    public function uploadPostImage(): void {
        $username = trim((string)($_POST['username'] ?? ''));
        $password = (string)($_POST['password'] ?? '');

        if ($username === '' || $password === '') {
            http_response_code(400);
            echo json_encode(['IsSuccess'=>false,'error'=>'missing_credentials']);
            return;
        }

        $user = $this->authUser($username, $password);
        if (!$user) {
            http_response_code(401);
            echo json_encode(['IsSuccess'=>false,'error'=>'auth_failed']);
            return;
        }

        if (empty($_FILES['image']) || $_FILES['image']['error'] !== UPLOAD_ERR_OK) {
            http_response_code(400);
            echo json_encode(['IsSuccess'=>false,'error'=>'no_file']);
            return;
        }

        $uploadDirFs   = __DIR__ . '/../uploads/';
        $uploadBaseUrl = '/cse476/group6/uploads/';

        if (!is_dir($uploadDirFs)) {
            @mkdir($uploadDirFs, 0775, true);
        }

        $origName = $_FILES['image']['name'];
        $ext = strtolower(pathinfo($origName, PATHINFO_EXTENSION));
        $allowed = ['jpg','jpeg','png','gif','webp'];

        if (!in_array($ext, $allowed, true)) {
            http_response_code(400);
            echo json_encode(['IsSuccess'=>false,'error'=>'invalid_file_type']);
            return;
        }

        $newName = bin2hex(random_bytes(16)).'.'.$ext;
        $targetFs = $uploadDirFs.$newName;

        if (!move_uploaded_file($_FILES['image']['tmp_name'], $targetFs)) {
            http_response_code(500);
            echo json_encode(['IsSuccess'=>false,'error'=>'upload_failed']);
            return;
        }

        echo json_encode([
            'IsSuccess'=>true,
            'imageUrl'=>$uploadBaseUrl.$newName
        ]);
    }

    // =========================================================
    // 6) DeletePost (DELETE)
    // Body: { username, password, postId }
    // =========================================================
    public function deletePost(): void {
        $raw = file_get_contents('php://input');
        $b   = json_decode($raw,true);
        if (!is_array($b)) {
            parse_str($raw,$b);
        }

        $u   = trim((string)($b['username']??''));
        $p   = (string)($b['password']??'');
        $pid = (int)($b['postId']??0);

        if ($u==='' || $p==='' || $pid<=0) {
            http_response_code(400);
            echo json_encode(['IsSuccess'=>false,'error'=>'bad_request']);
            return;
        }

        $user = $this->authUser($u,$p);
        if (!$user) {
            http_response_code(401);
            echo json_encode(['IsSuccess'=>false,'error'=>'auth_failed']);
            return;
        }

        // Check ownership
        $own = $this->pdo->prepare(
            "SELECT 1 FROM posts WHERE post_id=:pid AND user_id=:uid"
        );
        $own->execute([':pid'=>$pid, ':uid'=>$user['user_id']]);

        if (!$own->fetchColumn()) {
            echo json_encode(['IsSuccess'=>false,'error'=>'not_owner']);
            return;
        }

        // Delete post + comments
        $this->pdo->beginTransaction();
        try {
            $this->pdo->prepare("DELETE FROM comments WHERE post_id=:pid")
                ->execute([':pid'=>$pid]);

            $ok = $this->pdo->prepare("DELETE FROM posts WHERE post_id=:pid")
                ->execute([':pid'=>$pid]);

            $this->pdo->commit();

            echo json_encode(['IsSuccess'=>$ok, 'error'=>null]);

        } catch (\Throwable $e) {
            $this->pdo->rollBack();
            error_log("deletePost FAIL: ".$e->getMessage());
            http_response_code(500);
            echo json_encode(['IsSuccess'=>false,'error'=>'delete_failed']);
        }
    }

    // =========================================================
    // 7) GetPostFeed (GET)
    // Query: username, password, limit?, afterId?
    // =========================================================
    public function getPostFeed(): void {
        $u = trim((string)($_GET['username'] ?? ''));
        $p = (string)($_GET['password'] ?? '');
        $limit   = max(1, min(50, (int)($_GET['limit'] ?? 20)));
        $afterId = (int)($_GET['afterId'] ?? 0);

        try {
            $user = $this->authUser($u,$p);
            if (!$user) {
                echo json_encode(['error'=>'auth_failed']);
                return;
            }

            // load posts
            $sql = "SELECT p.post_id, p.user_id, p.location_id,
                           p.image_url, p.caption, p.created_at,
                           u.username
                      FROM posts p
                      JOIN users u ON u.user_id = p.user_id
                     WHERE ($afterId = 0 OR p.post_id < $afterId)
                     ORDER BY p.post_id DESC
                     LIMIT $limit";

            $stmt  = $this->pdo->query($sql);
            $posts = $stmt->fetchAll(PDO::FETCH_ASSOC);

            if (!$posts) {
                echo json_encode(['posts'=>[], 'nextAfterId'=>0]);
                return;
            }

            // load comments
            $pids = array_map(fn($r)=>(int)$r['post_id'], $posts);
            $ph   = implode(',', array_fill(0, count($pids), '?'));

            $c = $this->pdo->prepare(
                "SELECT c.comment_id,
                        c.post_id,
                        c.commenter_user_id AS user_id,
                        u.username,
                        c.content,
                        c.created_at
                   FROM comments c
                   JOIN users u ON u.user_id = c.commenter_user_id
                  WHERE c.post_id IN ($ph)
                  ORDER BY c.created_at ASC"
            );
            $c->execute($pids);
            $cmts = $c->fetchAll(PDO::FETCH_ASSOC);

            // group by post_id
            $by = [];
            foreach ($cmts as $x) {
                $by[(int)$x['post_id']][] = [
                    'comment_id' => (int)$x['comment_id'],
                    'post_id'    => (int)$x['post_id'],
                    'user_id'    => (int)$x['user_id'],
                    'username'   => $x['username'],
                    'content'    => $x['content'],
                    'created_at' => $x['created_at']
                ];
            }

            // output
            $out = [];
            foreach ($posts as $p0) {
                $pid = (int)$p0['post_id'];
                $out[] = [
                    'post_id'    => $pid,
                    'created_at' => $p0['created_at'],
                    'image_url'  => $p0['image_url'],
                    'caption'    => $p0['caption'],
                    'author'     => [
                        'user_id'  => (int)$p0['user_id'],
                        'username' => $p0['username'],
                        'pic'      => null
                    ],
                    'location'   => null,
                    'comments'   => $by[$pid] ?? []
                ];
            }

            echo json_encode([
                'posts'       => $out,
                'nextAfterId' => min($pids)
            ]);

        } catch (\Throwable $e) {
            error_log("getPostFeed FAIL: ".$e->getMessage());
            http_response_code(500);
            echo json_encode(['ok'=>false,'error'=>'internal']);
        }
    }
}