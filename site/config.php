<?php
$dbHost = getenv('DB_HOST') ?: 'mariadb';
$dbPort = (int) (getenv('DB_PORT') ?: 3306);
$dbName = getenv('DB_NAME') ?: 'l2jmobiusinterlude';
$dbUser = getenv('DB_USER') ?: 'root';
$dbPass = getenv('DB_PASS') ?: 'luiz33423342';

$mysqli = @mysqli_connect($dbHost, $dbUser, $dbPass, $dbName, $dbPort);
if (!$mysqli) {
    // Fallback if connecting from outside docker on host port 3307
    $mysqli = @mysqli_connect('127.0.0.1', $dbUser, $dbPass, $dbName, 3307);
    if (!$mysqli) {
        die('Erro ao conectar ao banco de dados: ' . mysqli_connect_error());
    }
}

$serverName = 'L2 Korentech';
$serverIp = getenv('SERVER_IP') ?: '2.24.108.110';
$loginPort = getenv('LOGIN_PORT') ?: 2106;
$gamePort = getenv('GAME_PORT') ?: 7777;

function isServerOnline($host, $port) {
    $conn = @fsockopen($host, $port, $errno, $errstr, 1.5);
    if ($conn) {
        fclose($conn);
        return true;
    }
    return false;
}

function h($value) {
    return htmlspecialchars($value, ENT_QUOTES, 'UTF-8');
}

function getOnlinePlayerCount($mysqli) {
    $result = @mysqli_query($mysqli, "SELECT COUNT(*) AS total FROM characters WHERE online = 1");
    if (!$result) {
        return 0;
    }
    $row = mysqli_fetch_assoc($result);
    return (int) ($row['total'] ?? 0);
}
