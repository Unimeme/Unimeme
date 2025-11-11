<?php
declare(strict_types=1);
 
use App\Db;
 
require_once __DIR__ . '/../src/Config.php';
require_once __DIR__ . '/../src/Db.php';
require_once __DIR__ . '/../src/Security.php';
 
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') exit(0);
 
$pdo = (new Db())->pdo();
 
$path   = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);
$method = $_SERVER['REQUEST_METHOD'];
 
switch (true) {
    case $path === '/api/posts/create' && $method === 'POST':
        require __DIR__ . '/../src/controllers/PostsController.php';
        (new PostsController($pdo))->createPost();
        break;

    case $path === '/api/users/create' && $method === 'POST':
        require __DIR__ . '/../src/controllers/UsersController.php';
        (new UsersController($pdo))->createUser();
        break;

    // add other routes (users, followers, etc.)
    default:
        http_response_code(404);
        echo json_encode(['error' => 'Not found']);
}