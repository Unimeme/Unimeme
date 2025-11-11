<?php
declare(strict_types=1);
 
namespace App;
 
final class Config
{
    /** Returns the [mysql] block from php.ini as an array (with sane defaults). */
    public static function mysql(): array
    {
        static $cached = null;
        if ($cached !== null) return $cached;
 
        $iniPath = \php_ini_loaded_file();
        if ($iniPath === false) {
            throw new \RuntimeException('php.ini not found; cannot load MySQL settings.');
        }
 
        $ini = \parse_ini_file($iniPath, true, \INI_SCANNER_TYPED);
        $s   = $ini['mysql'] ?? [];
 
        $cached = [
            'host'     => $s['host']     ?? '127.0.0.1',
            'port'     => (int)($s['port'] ?? 3306),
            'database' => $s['database'] ?? '',
            'username' => $s['username'] ?? '',
            'password' => $s['password'] ?? '',
            'charset'  => $s['charset']  ?? 'utf8mb4',
            'ssl_ca'   => $s['ssl_ca']   ?? null,
        ];
        return $cached;
    }
}