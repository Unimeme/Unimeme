<?php
//declare(strict_types=1);

// use App\Security;
// use PDO;

final class FollowersController {
    private \PDO $pdo;

    public function __construct(PDO $pdo) {
        $this->pdo = $pdo;
    }

    private function authUser(string $username, string $password): bool {
        $s=$this->pdo->prepare("SELECT password_hash FROM users WHERE username=:u");
        $s->execute([':u'=>$username]); $r=$s->fetch();
        return $r ? \App\Security::verifyPassword($password,(string)$r['password_hash']) : false;
    }

    // 8) CREATE_LOCATION (POST)
    public function createLocation(): void {
        $b=json_decode(file_get_contents('php://input'), true) ?? [];
        $u=trim((string)($b['username']??'')); $p=(string)($b['password']??'');
        $name=trim((string)($b['name']??'')); $lat=$b['latitude']??null; $lon=$b['longitude']??null;

        if($u===''||$p===''||$name===''){ http_response_code(400); echo json_encode(['IsSuccess'=>false,'error'=>'missing_fields']); return; }
        if(!$this->authUser($u,$p)){ echo json_encode(['IsSuccess'=>false,'error'=>'auth_failed']); return; }

        $ok=$this->pdo->prepare(
            "INSERT INTO locations (name, latitude, longitude) VALUES (:n,:lat,:lon)"
        )->execute([':n'=>$name, ':lat'=>$lat, ':lon'=>$lon]);

        echo json_encode(['IsSuccess'=>$ok, 'locationId'=>$ok?(int)$this->pdo->lastInsertId():null]);
    }

    // 9) DELETE_LOCATION (DELETE)
    public function deleteLocation(): void {
        $raw=file_get_contents('php://input'); $b=json_decode($raw,true); if(!is_array($b)) parse_str($raw,$b);
        $u=trim((string)($b['username']??'')); $p=(string)($b['password']??''); $locId=(int)($b['LocationId']??0);

        if($u===''||$p===''||$locId<=0){ http_response_code(400); echo json_encode(['IsSuccess'=>false,'error'=>'bad_request']); return; }
        if(!$this->authUser($u,$p)){ echo json_encode(['IsSuccess'=>false,'error'=>'auth_failed']); return; }

        // optional: ensure no posts reference this location, or rely on FK
        $ok=$this->pdo->prepare("DELETE FROM locations WHERE location_id=:id")->execute([':id'=>$locId]);
        echo json_encode(['IsSuccess'=>$ok]);
    }
}
