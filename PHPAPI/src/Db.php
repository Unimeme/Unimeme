<?php
declare(strict_types=1);
 
namespace App;
 
use PDO;
use PDOException;
 
final class Db
{
    private PDO $pdo;
 
    public function __construct(?array $cfg = null)
    {
        $c = $cfg ?? Config::mysql();
 
        $dsn = \sprintf(
            'mysql:host=%s;port=%d;dbname=%s;charset=%s',
            $c['host'], $c['port'], $c['database'], $c['charset']
        );
 
        $options = [
            PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_EMULATE_PREPARES   => false,
        ];
        if (!empty($c['ssl_ca'])) {
            $options[\PDO::MYSQL_ATTR_SSL_CA] = $c['ssl_ca'];
        }
 
        try {
            $this->pdo = new PDO($dsn, $c['username'], $c['password'], $options);
        } catch (PDOException $e) {
            // safe minimal output; log the exception elsewhere
            http_response_code(500);
            header('Content-Type: application/json');
            echo json_encode(['error' => 'DB_CONNECTION_FAILED']);
            throw $e;
        }
    }
 
    public function pdo(): PDO { return $this->pdo; }
}