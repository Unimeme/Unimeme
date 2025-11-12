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
        $cfg = $this->readMysqlFromIni();

        $dsn = sprintf(
            'mysql:host=%s;port=%d;dbname=%s;charset=%s',
            $cfg['host'],
            (int)$cfg['port'],
            $cfg['database'],
            $cfg['charset']
        );

        // $options = [
        //     PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
        //     PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        //     PDO::ATTR_EMULATE_PREPARES   => false,
        // ];
        $options = [
        PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES   => false,
        PDO::ATTR_TIMEOUT            => 5,
        ];



        // Optional TLS if provided
        if (!empty($cfg['ssl_ca'])) {
            $options[\PDO::MYSQL_ATTR_SSL_CA] = $cfg['ssl_ca'];
        }

        try {
            $this->pdo = new PDO($dsn, $cfg['username'], $cfg['password'], $options);
        } catch (PDOException $e) {
            throw $e;
            
            http_response_code(500);
            header('Content-Type: application/json');
            echo json_encode(['error' => 'DB_CONNECTION_FAILED']);
            // You may also log $e->getMessage() to a file
            exit;
        }
    }

    public function pdo(): PDO
    {
        return $this->pdo;
    }

    /** Load [mysql] block from the active php.ini */
    private function readMysqlFromIni(): array
    {
        $iniPath = \php_ini_loaded_file();
        if ($iniPath === false) {
            throw new \RuntimeException('Unable to locate php.ini; cannot read [mysql] settings.');
        }

        $ini = \parse_ini_file($iniPath, true, \INI_SCANNER_TYPED);
        $s   = $ini['mysql'] ?? [];

        // Minimal validation
        foreach (['host','database','username','password'] as $k) {
            if (!isset($s[$k]) || $s[$k] === '') {
                throw new \RuntimeException("Missing [mysql]::$k in php.ini");
            }
        }

        return [
            'host'     => (string)($s['host'] ?? '127.0.0.1'),
            'port'     => (int)   ($s['port'] ?? 3306),
            'database' => (string)($s['database'] ?? ''),
            'username' => (string)($s['username'] ?? ''),
            'password' => (string)($s['password'] ?? ''),
            'charset'  => (string)($s['charset']  ?? 'utf8mb4'),
            'ssl_ca'   => isset($s['ssl_ca']) ? (string)$s['ssl_ca'] : null,
        ];
    }
}