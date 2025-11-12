<?php
declare(strict_types=1);

namespace App;

use PDO;
use PDOException;

final class Database
{
    private PDO $pdo;

    public function __construct()
    {
        $cfg = $this->readMysqlFromIniWithFallback();

        $dsn = sprintf(
            'mysql:host=%s;port=%d;dbname=%s;charset=%s',
            $cfg['host'],
            (int)$cfg['port'],
            $cfg['database'],
            $cfg['charset']
        );

        $options = [
            PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_EMULATE_PREPARES   => false,
            PDO::ATTR_TIMEOUT            => 5,
        ];
        if (!empty($cfg['ssl_ca'])) {
            $options[\PDO::MYSQL_ATTR_SSL_CA] = $cfg['ssl_ca'];
        }

        try {
            $this->pdo = new PDO($dsn, $cfg['username'], $cfg['password'], $options);
        } catch (PDOException $e) {
            error_log('DB_CONNECTION_FAILED: '.$e->getMessage());
            http_response_code(500);
            header('Content-Type: application/json');
            echo json_encode(['ok'=>false,'error'=>'db_connect_failed']);
            exit;
        }
    }

    public function pdo(): PDO { return $this->pdo; }

    private function readMysqlFromIniWithFallback(): array
    {
        $envHost = getenv('DB_HOST');
        if ($envHost) {
            return [
                'host'     => $envHost,
                'port'     => (int)(getenv('DB_PORT') ?: 3306),
                'database' => (string)(getenv('DB_NAME') ?: ''),
                'username' => (string)(getenv('DB_USER') ?: ''),
                'password' => (string)(getenv('DB_PASS') ?: ''),
                'charset'  => (string)(getenv('DB_CHARSET') ?: 'utf8mb4'),
                'ssl_ca'   => getenv('DB_SSL_CA') ?: null,
            ];
        }

        $iniPath = \php_ini_loaded_file();
        if ($iniPath !== false) {
            $ini = \parse_ini_file($iniPath, true, \INI_SCANNER_TYPED) ?: [];
            $s   = $ini['mysql'] ?? [];
            if (!empty($s['host']) && !empty($s['database']) && !empty($s['username']) && isset($s['password'])) {
                return [
                    'host'     => (string)$s['host'],
                    'port'     => (int)   ($s['port'] ?? 3306),
                    'database' => (string)$s['database'],
                    'username' => (string)$s['username'],
                    'password' => (string)$s['password'],
                    'charset'  => (string)($s['charset'] ?? 'utf8mb4'),
                    'ssl_ca'   => isset($s['ssl_ca']) ? (string)$s['ssl_ca'] : null,
                ];
            }
        }

        return [
            'host'     => 'mysql-user.egr.msu.edu',
            'port'     => 3306,
            'database' => 'cse476fs25group6',
            'username' => 'cse476fs25group6',
            'password' => 'iegh7luziH@o',
            'charset'  => 'utf8mb4',
            'ssl_ca'   => null,
        ];
    }
}
