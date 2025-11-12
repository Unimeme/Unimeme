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
        $s=$this->pdo->prepare("SELECT user_id, username, password_hash FROM users WHERE username=:u");
        $s->execute([':u'=>$username]); $r=$s->fetch();
        return ($r && \App\Security::verifyPassword($password,(string)$r['password_hash'])) ? $r : null;
    }

    // 5) CreatePost (POST)
    public function createPost(): void {
        $b=json_decode(file_get_contents('php://input'), true) ?? [];
        $u=trim((string)($b['username']??'')); $p=(string)($b['password']??'');
        $img=trim((string)($b['imageUrl']??'')); $cap=(string)($b['caption']??'');
        $loc=$b['locationId']??null;

        if($u===''||$p===''||$img===''){ http_response_code(400); echo json_encode(['IsSuccess'=>false,'error'=>'missing_fields']); return; }
        $user=$this->authUser($u,$p); if(!$user){ echo json_encode(['IsSuccess'=>false,'error'=>'auth_failed']); return; }

        $ok=$this->pdo->prepare(
            "INSERT INTO posts (user_id, location_id, image_url, caption, created_at)
             VALUES (:uid,:loc,:img,:cap,UTC_TIMESTAMP())"
        )->execute([
            ':uid'=>(int)$user['user_id'],
            ':loc'=>($loc!==null? (int)$loc : null),
            ':img'=>$img,
            ':cap'=>$cap
        ]);
        echo json_encode(['IsSuccess'=>$ok,'postId'=>$ok?(int)$this->pdo->lastInsertId():null]);
    }

    // 6) DeletePost (DELETE)
    public function deletePost(): void {
        $raw=file_get_contents('php://input'); $b=json_decode($raw,true); if(!is_array($b)) parse_str($raw,$b);
        $u=trim((string)($b['username']??'')); $p=(string)($b['password']??''); $pid=(int)($b['postId']??0);
        if($u===''||$p===''||$pid<=0){ http_response_code(400); echo json_encode(['IsSuccess'=>false,'error'=>'bad_request']); return; }

        $user=$this->authUser($u,$p); if(!$user){ echo json_encode(['IsSuccess'=>false,'error'=>'auth_failed']); return; }

        $own=$this->pdo->prepare("SELECT 1 FROM posts WHERE post_id=:pid AND user_id=:uid");
        $own->execute([':pid'=>$pid, ':uid'=>(int)$user['user_id']]);
        if(!$own->fetchColumn()){ echo json_encode(['IsSuccess'=>false,'error'=>'not_owner']); return; }

        $this->pdo->beginTransaction();
        try{
            $this->pdo->prepare("DELETE FROM comments WHERE post_id=:pid")->execute([':pid'=>$pid]);
            $ok=$this->pdo->prepare("DELETE FROM posts WHERE post_id=:pid")->execute([':pid'=>$pid]);
            $this->pdo->commit();
            echo json_encode(['IsSuccess'=>$ok]);
        }catch(Throwable $e){ $this->pdo->rollBack(); http_response_code(500); echo json_encode(['IsSuccess'=>false,'error'=>'delete_failed']); }
    }

    // 7) GetPostFeed (GET)
    public function getPostFeed(): void {
        $u=trim((string)($_GET['username']??'')); $p=(string)($_GET['password']??'');
        $limit=max(1,min(50,(int)($_GET['limit']??20))); $afterId=(int)($_GET['afterId']??0);
        $user=$this->authUser($u,$p); if(!$user){ echo json_encode(['error'=>'auth_failed']); return; }

        $authors=$this->pdo->prepare("SELECT followed_id AS author_id FROM followers WHERE follower_id=:uid UNION SELECT :uid");
        $authors->execute([':uid'=>(int)$user['user_id']]);
        $ids=array_map(fn($r)=>(int)$r['author_id'],$authors->fetchAll());
        if(!$ids){ echo json_encode(['posts'=>[]]); return; }

        $ph=implode(',', array_fill(0,count($ids),'?'));
        $sql="SELECT p.post_id,p.user_id,p.location_id,p.image_url,p.caption,p.created_at,
                     u.username,u.profile_pic_url,l.name AS location_name,l.latitude,l.longitude
              FROM posts p
              JOIN users u ON u.user_id=p.user_id
              LEFT JOIN locations l ON l.location_id=p.location_id
              WHERE p.user_id IN ($ph) ".($afterId>0?"AND p.post_id < ? ":"")."
              ORDER BY p.post_id DESC LIMIT ?";
        $params=$ids; if($afterId>0) $params[]=$afterId; $params[]=$limit;

        $stmt=$this->pdo->prepare($sql); $stmt->execute($params); $posts=$stmt->fetchAll();
        if(!$posts){ echo json_encode(['posts'=>[]]); return; }

        $pids=array_map(fn($r)=>(int)$r['post_id'],$posts);
        $ph2=implode(',', array_fill(0,count($pids),'?'));
        $c=$this->pdo->prepare("SELECT c.comment_id,c.post_id,c.user_id,c.content,c.created_at,u.username
                                FROM comments c JOIN users u ON u.user_id=c.user_id
                                WHERE c.post_id IN ($ph2) ORDER BY c.created_at ASC");
        $c->execute($pids); $cmts=$c->fetchAll();
        $by=[]; foreach($cmts as $x){ $by[(int)$x['post_id']][]=[
            'comment_id'=>(int)$x['comment_id'],'post_id'=>(int)$x['post_id'],'user_id'=>(int)$x['user_id'],
            'username'=>$x['username'],'content'=>$x['content'],'created_at'=>$x['created_at']
        ];}

        $out=[]; foreach($posts as $p0){ $pid=(int)$p0['post_id']; $out[]=[
            'post_id'=>$pid,'created_at'=>$p0['created_at'],'image_url'=>$p0['image_url'],'caption'=>$p0['caption'],
            'author'=>['user_id'=>(int)$p0['user_id'],'username'=>$p0['username'],'pic'=>$p0['profile_pic_url']],
            'location'=>$p0['location_id']?[
                'location_id'=>(int)$p0['location_id'],'name'=>$p0['location_name'],
                'latitude'=>$p0['latitude'],'longitude'=>$p0['longitude']
            ]:null,
            'comments'=>$by[$pid]??[]
        ];}

        echo json_encode(['posts'=>$out,'nextAfterId'=>min($pids)]);
    }
}