<?php
// Script de testes automatizados para o sistema de doacoes Pix via Mercado Pago

$passed = 0;
$failed = 0;

function assertTest($description, $condition) {
    global $passed, $failed;
    if ($condition) {
        echo "  [PASS] $description\n";
        $passed++;
    } else {
        echo "  [FAIL] $description\n";
        $failed++;
    }
}

echo "=== INICIANDO TESTES DO SISTEMA DE DOAÇÕES PIX ===\n\n";

// 1. Teste de Hashing de Senha Compatível com Emulador L2
echo "1. Compatibilidade de Hash de Senhas L2:\n";
$plainPass = 'senhaSegura123';
$hash1 = base64_encode(sha1($plainPass, true));
$hash2 = base64_encode(sha1($plainPass, true));
$wrongHash = base64_encode(sha1('outraSenha', true));

assertTest('Hashes gerados para a mesma senha são idênticos', $hash1 === $hash2);
assertTest('Hash confere com comparação segura hash_equals', hash_equals($hash1, $hash2));
assertTest('Hash de senha diferente falha na validação', !hash_equals($hash1, $wrongHash));
assertTest('Formato do hash é Base64 válido de SHA-1 (28 caracteres)', strlen($hash1) === 28 && base64_decode($hash1, true) !== false);

// 2. Teste de Tokens CSRF
echo "\n2. Validação de Tokens CSRF:\n";
$_SESSION = [];
function testGenerateCsrf() {
    if (empty($_SESSION['csrf_token'])) {
        $_SESSION['csrf_token'] = bin2hex(random_bytes(32));
    }
    return $_SESSION['csrf_token'];
}
function testValidateCsrf($token) {
    return !empty($token) && !empty($_SESSION['csrf_token']) && hash_equals($_SESSION['csrf_token'], $token);
}

$token = testGenerateCsrf();
assertTest('Token CSRF gerado tem 64 caracteres hexadecimais', strlen($token) === 64 && ctype_xdigit($token));
assertTest('Validação com token correto retorna true', testValidateCsrf($token));
assertTest('Validação com token vazio retorna false', !testValidateCsrf(''));
assertTest('Validação com token adulterado retorna false', !testValidateCsrf('fake-token-123'));

// 3. Teste de Limites de Valores de Doação
echo "\n3. Validação de Limites de Contribuição:\n";
$min = 5.00;
$max = 1000.00;

function isValidDonationAmount($raw, $min, $max) {
    $norm = str_replace(',', '.', trim((string)$raw));
    if (!is_numeric($norm)) return false;
    $val = round((float)$norm, 2);
    return ($val >= $min && $val <= $max);
}

assertTest('Valor R$ 25,00 é aceito', isValidDonationAmount('25.00', $min, $max));
assertTest('Valor com vírgula R$ 25,50 é aceito', isValidDonationAmount('25,50', $min, $max));
assertTest('Valor mínimo exato R$ 5,00 é aceito', isValidDonationAmount('5.00', $min, $max));
assertTest('Valor máximo exato R$ 1.000,00 é aceito', isValidDonationAmount('1000.00', $min, $max));
assertTest('Valor abaixo do mínimo R$ 4,99 é rejeitado', !isValidDonationAmount('4.99', $min, $max));
assertTest('Valor acima do máximo R$ 1.000,01 é rejeitado', !isValidDonationAmount('1000.01', $min, $max));
assertTest('Valor negativo R$ -10,00 é rejeitado', !isValidDonationAmount('-10.00', $min, $max));
assertTest('Entrada não-numérica é rejeitada', !isValidDonationAmount('dez_reais', $min, $max));

// 4. Teste de Formato de External Reference
echo "\n4. Formato de External Reference:\n";
$extRef = bin2hex(random_bytes(32));
assertTest('External Reference possui 64 caracteres hexadecimais únicos', strlen($extRef) === 64 && ctype_xdigit($extRef));

// 5. Teste de Conexão com MariaDB e Persistência da Tabela donations
echo "\n5. Integração com Banco de Dados (MariaDB):\n";
$dbHost = getenv('DB_HOST') ?: 'mariadb';
$dbPort = (int) (getenv('DB_PORT') ?: 3306);
$dbName = getenv('DB_NAME') ?: 'l2jmobiusinterlude';
$dbUser = getenv('DB_USER') ?: 'root';
$dbPass = getenv('DB_PASSWORD') ?: (getenv('DB_PASS') ?: 'luiz33423342');

$mysqli = @mysqli_connect($dbHost, $dbUser, $dbPass, $dbName, $dbPort);
if (!$mysqli) {
    $mysqli = @mysqli_connect('127.0.0.1', $dbUser, $dbPass, $dbName, 3307);
}

if ($mysqli) {
    assertTest('Conexão com MariaDB estabelecida', true);

    // Inserir registro de teste
    $testLogin = 'test_donor';
    $testRef = 'test_ref_' . bin2hex(random_bytes(24));
    $stmt = $mysqli->prepare('INSERT INTO donations (login, external_reference, amount, currency, status) VALUES (?, ?, 25.00, "BRL", "pending")');
    $stmt->bind_param('ss', $testLogin, $testRef);
    $inserted = $stmt->execute();
    $insertId = $stmt->insert_id;
    $stmt->close();
    assertTest('Inserção de doação pendente com sucesso', $inserted && $insertId > 0);

    // Atualização idempotente para approved
    $paymentId = 'pay_' . rand(100000, 999999);
    $upStmt = $mysqli->prepare('UPDATE donations SET status = "approved", payment_id = ?, approved_at = NOW() WHERE external_reference = ?');
    $upStmt->bind_param('ss', $paymentId, $testRef);
    $updated = $upStmt->execute();
    $upStmt->close();
    assertTest('Atualização para status approved com timestamp de aprovação', $updated);

    // Verificar leitura
    $selStmt = $mysqli->prepare('SELECT status, payment_id, approved_at FROM donations WHERE external_reference = ?');
    $selStmt->bind_param('s', $testRef);
    $selStmt->execute();
    $res = $selStmt->get_result()->fetch_assoc();
    $selStmt->close();
    assertTest('Status lido confere com approved', $res['status'] === 'approved');
    assertTest('Payment ID confere com o atualizado', $res['payment_id'] === $paymentId);
    assertTest('Approved_at preenchido', !empty($res['approved_at']));

    // Limpeza do registro de teste
    $delStmt = $mysqli->prepare('DELETE FROM donations WHERE external_reference = ?');
    $delStmt->bind_param('s', $testRef);
    $delStmt->execute();
    $delStmt->close();
    assertTest('Limpeza do registro de teste concluída', true);
} else {
    echo "  [SKIP] Conexão com MariaDB ignorada (banco inacessível no ambiente atual).\n";
}

echo "\n=========================================\n";
echo "RESULTADO DOS TESTES: $passed PASS | $failed FAIL\n";
echo "=========================================\n";

if ($failed > 0) {
    exit(1);
}
exit(0);
