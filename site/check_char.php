<?php
require_once __DIR__ . '/config.php';
header('Content-Type: text/plain; charset=utf-8');

$res = mysqli_query($mysqli, "SELECT charId, char_name, account_name, level, exp, sp, classid, accesslevel, online FROM characters WHERE char_name='LUiz'");
$row = mysqli_fetch_assoc($res);
print_r($row);
