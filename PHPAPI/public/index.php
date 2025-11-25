<?php
// ---- logging: project-local log file ----
ini_set('display_errors', 0);
ini_set('log_errors', 1);
ini_set('error_log', __DIR__ . '/../_logs/app.log');
error_reporting(E_ALL);

set_error_handler(function($s,$m,$f,$l){
  error_log("PHP[$s] $m @ $f:$l");
});
set_exception_handler(function($e){
  error_log("UNCAUGHT ".get_class($e).": ".$e->getMessage()." @ ".$e->getFile().":".$e->getLine());
  if (strpos($_SERVER['REQUEST_URI'] ?? '', '/api/') !== false) {
    header('Content-Type: application/json', true, 500);
    echo json_encode(['ok'=>false,'error'=>'internal']);
  }
  exit;
});

// ---- requires (absolute paths, match your tree) ----
require_once __DIR__ . '/../src/Config.php';
require_once __DIR__ . '/../src/Database.php';
require_once __DIR__ . '/../src/Security.php';

require_once __DIR__ . '/../controllers/UsersController.php';
require_once __DIR__ . '/../controllers/PostsController.php';
require_once __DIR__ . '/../controllers/LocationsController.php';
require_once __DIR__ . '/../controllers/FollowersController.php';
require_once __DIR__ . '/../controllers/CommentsController.php';

use App\Database;

// ---- common headers (API style) ----
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');
if (($_SERVER['REQUEST_METHOD'] ?? '') === 'OPTIONS') exit(0);

// ---- DB handle ----
$pdo = (new Database())->pdo();

// ---- base path trimming for DECS (/cse476/<group>) ----
$path   = parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH) ?: '/';
$base   = '/cse476/group6'; // <-- adjust if your group path changes
if (strpos($path, $base) === 0) $path = substr($path, strlen($base));
$method = $_SERVER['REQUEST_METHOD'] ?? 'GET';

// ---- routing ----
try {
  switch (true) {
    // ===== Users =====
    case $path === '/api/users/create' && $method === 'POST':
      (new UsersController($pdo))->createUser();  break;

    case $path === '/api/users/login' && $method === 'POST':
      (new UsersController($pdo))->login();       break;

    case $path === '/api/users/update' && $method === 'POST':
      (new UsersController($pdo))->updateUser();  break;

    case $path === '/api/users/delete' && $method === 'DELETE':
      (new UsersController($pdo))->deleteUser();  break;

    case $path === '/api/users/all' && $method === 'GET':
      (new UsersController($pdo))->getAllUsers(); break;

    case $path === '/api/users/get' && $method === 'GET':
      (new UsersController($pdo))->getUser();     break;

    // ===== Posts =====
    case $path === '/api/posts/create' && $method === 'POST':
      (new PostsController($pdo))->createPost();  break;

     case $path === '/api/posts/upload' && $method === 'POST':
        (new PostsController($pdo))->uploadPostImage();  break;

    case $path === '/api/posts/delete' && $method === 'DELETE':
      (new PostsController($pdo))->deletePost();  break;

    case $path === '/api/posts/feed' && $method === 'GET':
      (new PostsController($pdo))->getPostFeed(); break;

    // ===== Locations =====
    case $path === '/api/locations/create' && $method === 'POST':
      (new LocationsController($pdo))->createLocation(); break;

    case $path === '/api/locations/delete' && $method === 'DELETE':
      (new LocationsController($pdo))->deleteLocation(); break;

    // ===== Comments =====
    case $path === '/api/comments' && $method === 'GET':
      (new CommentsController($pdo))->getCommentsByPost(); break;

    case $path === '/api/comments/create' && $method === 'POST':
      (new CommentsController($pdo))->addComment(); break;

    // ===== Followers (placeholders) =====
    // case $path === '/api/followers/follow' && $method === 'POST':
    //   (new FollowersController($pdo))->follow(); break;
    // case $path === '/api/followers' && $method === 'GET':
    //   (new FollowersController($pdo))->getFollowers(); break;
    // case $path === '/api/following' && $method === 'GET':
    //   (new FollowersController($pdo))->getFollowing(); break;

    default:
      http_response_code(404);
      echo json_encode(['ok'=>false,'error'=>'not_found','path'=>$path]);
  }
} catch (Throwable $e) {
  error_log('ROUTER FAIL: '.$e->getMessage().' @ '.$e->getFile().':'.$e->getLine());
  http_response_code(500);
  echo json_encode(['ok'=>false,'error'=>'internal']);
}