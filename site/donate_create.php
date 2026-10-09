<?php
require 'config.php';
requireAuth();

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    header('Location: doar.php');
    exit;
}

$token = $_POST['csrf_token'] ?? '';
if (!validateCsrfToken($token)) {
    header('Location: doar.php?err=' . urlencode('Sessão ou token de formulário inválido. Tente novamente.'));
    exit;
}

$login = getLoggedInUser();
$rawAmount = str_replace(',', '.', trim($_POST['amount'] ?? ''));
if (!is_numeric($rawAmount)) {
    header('Location: doar.php?err=' . urlencode('Informe um valor numérico válido.'));
    exit;
}

$amount = round((float)$rawAmount, 2);
if ($amount < $minDonationAmount || $amount > $maxDonationAmount) {
    header('Location: doar.php?err=' . urlencode('O valor deve estar entre R$ ' . number_format($minDonationAmount, 2, ',', '.') . ' e R$ ' . number_format($maxDonationAmount, 2, ',', '.') . '.'));
    exit;
}

$externalReference = bin2hex(random_bytes(32));

// Inserir registro inicial como pending
$stmt = $mysqli->prepare('INSERT INTO donations (login, external_reference, amount, currency, status) VALUES (?, ?, ?, "BRL", "pending")');
if (!$stmt) {
    header('Location: doar.php?err=' . urlencode('Erro interno ao iniciar doação. Tente novamente.'));
    exit;
}
$stmt->bind_param('ssd', $login, $externalReference, $amount);
if (!$stmt->execute()) {
    $stmt->close();
    header('Location: doar.php?err=' . urlencode('Falha ao registrar doação no banco.'));
    exit;
}
$stmt->close();

// Chamada a API do Mercado Pago
if (empty($mpAccessToken)) {
    // Ambiente sem token configurado (modo de teste ou pendente de configuracao da chave)
    header('Location: donate_status.php?ref=' . urlencode($externalReference) . '&notice=no_token');
    exit;
}

$mpPayload = [
    'transaction_amount' => $amount,
    'description' => 'Apoio voluntario ' . $serverName . ' (' . $login . ')',
    'payment_method_id' => 'pix',
    'payer' => [
        'email' => strtolower($login) . '@korentech.l2',
        'first_name' => $login
    ],
    'external_reference' => $externalReference
];

if (!empty($mpWebhookUrl)) {
    $mpPayload['notification_url'] = $mpWebhookUrl;
}

$ch = curl_init('https://api.mercadopago.com/v1/payments');
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_POST => true,
    CURLOPT_POSTFIELDS => json_encode($mpPayload),
    CURLOPT_HTTPHEADER => [
        'Authorization: Bearer ' . $mpAccessToken,
        'Content-Type: application/json',
        'X-Idempotency-Key: ' . $externalReference
    ],
    CURLOPT_TIMEOUT => 20
]);

$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
$curlErr = curl_error($ch);
curl_close($ch);

if ($response && ($httpCode === 200 || $httpCode === 201)) {
    $data = json_decode($response, true);
    $paymentId = (string)($data['id'] ?? '');
    $qrCode = $data['point_of_interaction']['transaction_data']['qr_code'] ?? null;
    $qrCodeBase64 = $data['point_of_interaction']['transaction_data']['qr_code_base64'] ?? null;
    $status = $data['status'] ?? 'pending';

    $upStmt = $mysqli->prepare('UPDATE donations SET payment_id = ?, pix_qr_code = ?, pix_qr_code_base64 = ?, status = ? WHERE external_reference = ?');
    if ($upStmt) {
        $upStmt->bind_param('sssss', $paymentId, $qrCode, $qrCodeBase64, $status, $externalReference);
        $upStmt->execute();
        $upStmt->close();
    }

    header('Location: donate_status.php?ref=' . urlencode($externalReference));
    exit;
} else {
    // Registra falha de comunicacao com a API
    error_log("Mercado Pago Pix Error HTTP $httpCode: $response $curlErr");
    header('Location: doar.php?err=' . urlencode('Não foi possível gerar o Pix com o Mercado Pago no momento. Tente novamente mais tarde.'));
    exit;
}
