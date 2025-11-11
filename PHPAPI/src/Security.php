<?php
declare(strict_types=1);
 
namespace App;
 
final class Security {
    public static function hashPassword(string $plain): string {
        return password_hash($plain, PASSWORD_BCRYPT);
    }
 
    public static function verifyPassword(string $plain, string $hash): bool {
        return password_verify($plain, $hash);
    }
}
 