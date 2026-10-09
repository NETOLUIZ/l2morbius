<?php
require 'config.php';

header('Content-Type: application/json; charset=utf-8');

// Captura de payment_id da requisicao (via JSON POST ou Query Params)
$paymentId = null;

$rawInput = file_get_contents('php://input');
if (!empty($rawInput)) {
    $payload = json_decode($rawInput, true);
    if (isset($payload['data']['id'])) {
        $paymentId = (string)$payload['data']['id'];
    } elseif (isset($payload['id']) && (!isset($payload['type']) || $payload['type'] === 'payment')) {
        $paymentId = (string)$payload['id'];
    }
}

if (!$paymentId) {
    if (isset($_GET['data_id'])) {
        $paymentId = (string)$_GET['data_id'];
    } elseif (isset($_GET['id'])) {
        $paymentId = (string)$_GET['id'];
    }
}

if (!$paymentId) {
    http_response_code(200);
    echo json_encode(['status' => 'ignored', 'reason' => 'missing_payment_id']);
    exit;
}

if (empty($mpAccessToken)) {
    http_response_code(200);
    echo json_encode(['status' => 'ignored', 'reason' => 'missing_access_token']);
    exit;
}

// Consulta server-to-server obrigatoria na API do Mercado Pago
$ch = curl_init("https://api.mercadopago.com/v1/payments/" . urlencode($paymentId));
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_HTTPHEADER => [
        'Authorization: Bearer ' . $mpAccessToken,
        'Content-Type: application/json'
    ],
    CURLOPT_TIMEOUT => 20
]);

$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
$curlErr = curl_error($ch);
curl_close($ch);

if (!$response || $httpCode !== 200) {
    error_log("Webhook MP API error for payment $paymentId: HTTP $httpCode - $response - $curlErr");
    // Retorna 500 para o Mercado Pago tentar novamente mais tarde
    http_response_code(500);
    echo json_encode(['status' => 'error', 'reason' => 'mp_api_failed']);
    exit;
}

$mpData = json_decode($response, true);
$mpStatus = $mpData['status'] ?? null;
$mpCurrency = $mpData['currency_id'] ?? null;
$mpAmount = (float)($mpData['transaction_amount'] ?? 0);
$externalReference = $mpData['external_reference'] ?? null;

if (!$externalReference || $mpCurrency !== 'BRL') {
    http_response_code(200);
    echo json_encode(['status' => 'ignored', 'reason' => 'invalid_currency_or_reference']);
    exit;
}

// Processamento transacional e idempotente
$mysqli->begin_transaction();
try {
    $stmt = $mysqli->prepare('SELECT id, status, amount FROM donations WHERE external_reference = ? FOR UPDATE');
    if (!$stmt) {
        throw new Exception('Prepare failed');
    }

    $stmt->bind_param('s', $externalReference);
    $stmt->execute();
    $result = $stmt->get_result();
    $donation = $result ? $result->fetch_assoc() : null;
    $stmt->close();

    if (!$donation) {
        $mysqli->rollback();
        http_response_code(200);
        echo json_encode(['status' => 'ignored', 'reason' => 'donation_not_found']);
        exit;
    }

    // Se ja foi aprovada anteriormente, nao altera nada (idempotencia)
    if ($donation['status'] === 'approved') {
        $mysqli->commit();
        http_response_code(200);
        echo json_encode(['status' => 'already_approved']);
        exit;
    }

    // Valida se o valor pago bate com o registrado
    $expectedAmount = (float)$donation['amount'];
    if (abs($expectedAmount - $mpAmount) > 0.01) {
        error_log("Webhook amount mismatch for ref $externalReference: expected $expectedAmount, got $mpAmount");
        $mysqli->rollback();
        http_response_code(200);
        echo json_encode(['status' => 'ignored', 'reason' => 'amount_mismatch']);
        exit;
    }

    if ($mpStatus === 'approved') {
        $upStmt = $mysqli->prepare('UPDATE donations SET status = "approved", payment_id = ?, approved_at = NOW() WHERE id = ?');
        if ($upStmt) {
            $donationId = (int)$donation['id'];
            $upStmt->bind_param('si', $paymentId, $donationId);
            $upStmt->execute();
            $upStmt->close();
        }
    } elseif (in_array($mpStatus, ['cancelled', 'rejected', 'refunded', 'charged_back'], true)) {
        $upStmt = $mysqli->prepare('UPDATE donations SET status = ?, payment_id = ? WHERE id = ?');
        if ($upStmt) {
            $donationId = (int)$donation['id'];
            $upStmt->bind_param('ssi', $mpStatus, $paymentId, $donationId);
            $upStmt->execute();
            $upStmt->close();
        }
    }

    $mysqli->commit();
    http_response_code(200);
    echo json_encode(['status' => 'success', 'donation_status' => $mpStatus]);
    exit;

} catch (Exception $e) {
    $mysqli->rollback();
    error_log("Webhook database exception: " . $e->getMessage());
    http_response_code(500);
    echo json_encode(['status' => 'error', 'message' => $e->getMessage()]);
    exit;
}
