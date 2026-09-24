<?php
$secret = getenv('UPLOAD_SECRET');
$given = $_SERVER['HTTP_X_UPLOAD_SECRET'] ?? '';
if (!$secret || !hash_equals($secret, $given)) {
    http_response_code(403);
    exit('forbidden');
}

$allowed = ['logo.jpg', 'top01.jpg', 'top02.jpg', 'pic01.jpg', 'pic02.jpg', 'pic03.gif', 'pic04.gif'];
$name = $_GET['name'] ?? '';
if (!in_array($name, $allowed, true)) {
    http_response_code(400);
    exit('invalid name');
}

$data = file_get_contents('php://input');
if (!$data || @getimagesizefromstring($data) === false) {
    http_response_code(400);
    exit('invalid image data');
}

file_put_contents('/var/www/html/images/' . $name, $data);
echo 'ok';
