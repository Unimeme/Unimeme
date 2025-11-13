<?php
//declare(strict_types=1);

// use App\Security;
// use PDO;

final class PostsController {
    private \PDO $pdo;

    public function __construct(PDO $pdo) {
        $this->pdo = $pdo;
    }

    private function authUser(string $username, string $password): ?array {
        $s = $this->pdo->prepare(
            "SELECT user_id, username, password_hash FROM users WHERE username = :u"
        );
        $s->execute([':u' => $username]);
        $r = $s->fetch();

        return ($r && \App\Security::verifyPassword($password, (string)$r['password_hash']))
            ? $r
            : null;
    }

    /** ----------------------------
     *  5) CreatePost (POST)
     *  Body (JSON): { username, password, imageUrl, caption?, locationId? }
     *  Res : { IsSuccess:bool, postId:int|null, error?:string }
     *  ---------------------------- */
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
        $loc = $b['locationId'] ?? null;  // null 허용

        if ($u === '' || $p === '' || $img === '') {
            http_response_code(400);
            echo json_encode([
                'IsSuccess' => false,
                'postId'    => null,
                'error'     => 'missing_fields'
            ]);
            return;
        }

        $user = $this->authUser($u, $p);
        if (!$user) {
            http_response_code(401);
            echo json_encode([
                'IsSuccess' => false,
                'postId'    => null,
                'error'     => 'auth_failed'
            ]);
            return;
        }

        try {
            $sql = "INSERT INTO posts (user_id, location_id, image_url, caption, created_at)
                    VALUES (:uid, :loc, :img, :cap, UTC_TIMESTAMP())";
            $stmt = $this->pdo->prepare($sql);

            $stmt->bindValue(':uid', (int)$user['user_id'], \PDO::PARAM_INT);

            if ($loc === null || $loc === '') {
                // location_id 컬럼이 NULL 허용이어야 함
                $stmt->bindValue(':loc', null, \PDO::PARAM_NULL);
            } else {
                $stmt->bindValue(':loc', (int)$loc, \PDO::PARAM_INT);
            }

            $stmt->bindValue(':img', $img, \PDO::PARAM_STR);
            $stmt->bindValue(':cap', $cap, \PDO::PARAM_STR);

            $stmt->execute();

            $postId = (int)$this->pdo->lastInsertId();

            echo json_encode([
                'IsSuccess' => true,
                'postId'    => $postId,
                'error'     => null
            ]);
        } catch (\Throwable $e) {
            error_log('PostsController::createPost FAIL: '.$e->getMessage());
            http_response_code(500);
            echo json_encode([
                'IsSuccess' => false,
                'postId'    => null,
                'error'     => 'internal'
            ]);
        }
    }

    /** ----------------------------
     *  5-1) uploadPostImage (multipart/form-data)
     *  POST /api/posts/upload
     *  Fields: username, password, image(file)
     *  ---------------------------- */
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

        $uploadDirFs   = __DIR__ . '/../uploads/';    // filesystem path
        $uploadBaseUrl = '/cse476/group6/uploads/';   // URL prefix

        if (!is_dir($uploadDirFs)) {
            @mkdir($uploadDirFs, 0775, true);
        }

        $origName = $_FILES['image']['name'];
        $ext      = strtolower(pathinfo($origName, PATHINFO_EXTENSION));
        $allowed  = ['jpg','jpeg','png','gif','webp'];

        if (!in_array($ext, $allowed, true)) {
            http_response_code(400);
            echo json_encode(['IsSuccess'=>false,'error'=>'invalid_file_type']);
            return;
        }

        $newName      = bin2hex(random_bytes(16)).'.'.$ext;
        $targetFsPath = $uploadDirFs.$newName;

        if (!move_uploaded_file($_FILES['image']['tmp_name'], $targetFsPath)) {
            http_response_code(500);
            echo json_encode(['IsSuccess'=>false,'error'=>'upload_failed']);
            return;
        }

        $imageUrl = $uploadBaseUrl.$newName;

        echo json_encode([
            'IsSuccess' => true,
            'imageUrl'  => $imageUrl
        ]);
    }

    // 6) DeletePost (DELETE)
    public function deletePost(): void {
        $raw = file_get_contents('php://input');
        $b   = json_decode($raw,true);
        if (!is_array($b)) parse_str($raw,$b);

        $u   = trim((string)($b['username']??''));
        $p   = (string)($b['password']??'');
        $pid = (int)($b['postId']??0);

        if ($u==='' || $p==='' || $pid<=0) {
            http_response_code(400);
            echo json_encode(['IsSuccess'=>false,'error'=>'bad_request']);
            return;
        }

        $user=$this->authUser($u,$p);
        if(!$user){
            echo json_encode(['IsSuccess'=>false,'error'=>'auth_failed']);
            return;
        }

        $own=$this->pdo->prepare("SELECT 1 FROM posts WHERE post_id=:pid AND user_id=:uid");
        $own->execute([':pid'=>$pid, ':uid'=>(int)$user['user_id']]);
        if(!$own->fetchColumn()){
            echo json_encode(['IsSuccess'=>false,'error'=>'not_owner']);
            return;
        }

        $this->pdo->beginTransaction();
        try {
            $this->pdo->prepare("DELETE FROM comments WHERE post_id=:pid")
                      ->execute([':pid'=>$pid]);
            $ok=$this->pdo->prepare("DELETE FROM posts WHERE post_id=:pid")
                          ->execute([':pid'=>$pid]);
            $this->pdo->commit();
            echo json_encode(['IsSuccess'=>$ok]);
        } catch (\Throwable $e) {
            $this->pdo->rollBack();
            error_log('PostsController::deletePost FAIL: '.$e->getMessage());
            http_response_code(500);
            echo json_encode(['IsSuccess'=>false,'error'=>'delete_failed']);
        }
    }

    // 7) GetPostFeed (GET)
    public function getPostFeed(): void {
        $u=trim((string)($_GET['username']??''));
        $p=(string)($_GET['password']??'');
        $limit=max(1,min(50,(int)($_GET['limit']??20)));
        $afterId=(int)($_GET['afterId']??0);

        $user=$this->authUser($u,$p);
        if(!$user){
            echo json_encode(['error'=>'auth_failed']);
            return;
        }

        $authors=$this->pdo->prepare(
            "SELECT followed_id AS author_id FROM followers WHERE follower_id=:uid
             UNION
             SELECT :uid"
        );
        $authors->execute([':uid'=>(int)$user['user_id']]);
        $ids=array_map(fn($r)=>(int)$r['author_id'],$authors->fetchAll());
        if(!$ids){
            echo json_encode(['posts'=>[]]);
            return;
        }

        $ph=implode(',', array_fill(0,count($ids),'?'));
        $sql="SELECT p.post_id,p.user_id,p.location_id,p.image_url,p.caption,p.created_at,
                     u.username,u.profile_url,
                     l.name AS location_name,l.latitude,l.longitude
              FROM posts p
              JOIN users u ON u.user_id=p.user_id
              LEFT JOIN locations l ON l.location_id=p.location_id
              WHERE p.user_id IN ($ph) ".($afterId>0?"AND p.post_id < ? ":"")."
              ORDER BY p.post_id DESC LIMIT ?";
        $params=$ids;
        if($afterId>0) $params[]=$afterId;
        $params[]=$limit;

        $stmt=$this->pdo->prepare($sql);
        $stmt->execute($params);
        $posts=$stmt->fetchAll();
        if(!$posts){
            echo json_encode(['posts'=>[]]);
            return;
        }

        $pids=array_map(fn($r)=>(int)$r['post_id'],$posts);
        $ph2=implode(',', array_fill(0,count($pids),'?'));
        $c=$this->pdo->prepare(
            "SELECT c.comment_id,c.post_id,c.user_id,c.content,c.created_at,u.username
               FROM comments c
               JOIN users u ON u.user_id=c.user_id
              WHERE c.post_id IN ($ph2)
              ORDER BY c.created_at ASC"
        );
        $c->execute($pids);
        $cmts=$c->fetchAll();
        $by=[];
        foreach($cmts as $x){
            $by[(int)$x['post_id']][]=[
                'comment_id'=>(int)$x['comment_id'],
                'post_id'   =>(int)$x['post_id'],
                'user_id'   =>(int)$x['user_id'],
                'username'  =>$x['username'],
                'content'   =>$x['content'],
                'created_at'=>$x['created_at']
            ];
        }

        $out=[];
        foreach($posts as $p0){
            $pid=(int)$p0['post_id'];
            $out[]=[
                'post_id'   =>$pid,
                'created_at'=>$p0['created_at'],
                'image_url' =>$p0['image_url'],
                'caption'   =>$p0['caption'],
                'author'    =>[
                    'user_id' =>(int)$p0['user_id'],
                    'username'=>$p0['username'],
                    'pic'     =>$p0['profile_url'],
                ],
                'location'  =>$p0['location_id'] ? [
                    'location_id'=>(int)$p0['location_id'],
                    'name'       =>$p0['location_name'],
                    'latitude'   =>$p0['latitude'],
                    'longitude'  =>$p0['longitude'],
                ] : null,
                'comments'  =>$by[$pid] ?? []
            ];
        }

        echo json_encode([
            'posts'      =>$out,
            'nextAfterId'=>min($pids)
        ]);
    }
}
