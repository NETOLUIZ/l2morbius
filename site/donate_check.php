<?php
require 'config.php';

header('Content-Type: application/json; charset=utf-8');

if (!isLoggedIn()) {
    http_response_code(401);
    echo json_encode(['error' => 'Não autenticado']);
    exit;
}

$ref = trim($_GET['ref'] ?? '');
if (empty($ref)) {
    http_response_code(400);
    echo json_encode(['error' => 'Referência ausente']);
    exit;
}

$login = getLoggedInUser();
$stmt = $mysqli->prepare('SELECT status, amount, currency, approved_at FROM donations WHERE external_reference = ? AND login = ? LIMIT 1');
if (!$stmt) {
    http_response_code(500);
    echo json_encode(['error' => 'Erro interno']);
    exit;
}

$stmt->bind_param('ss', $ref, $login);
$stmt->execute();
$result = $stmt->get_result();
$donation = $result ? $result->fetch_assoc() : null;
$stmt->close();

if (!$donation) {
    http_response_code(404);
    echo json_encode(['error' => 'Doação não encontrada']);
    exit;
}

echo json_encode([
    'status' => $donation['status'],
    'amount' => (float)$donation['amount'],
    'currency' => $donation['currency'],
    'approved_at' => $donation['approved_at']
]);
