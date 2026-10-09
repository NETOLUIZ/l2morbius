<?php
require 'config.php';
$activePage = 'login';

$errors = [];

if (isLoggedIn()) {
    header('Location: doar.php');
    exit;
}

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $token = $_POST['csrf_token'] ?? '';
    if (!validateCsrfToken($token)) {
        $errors[] = 'Token de formulário expirado ou inválido. Atualize a página e tente novamente.';
    }

    $login = trim($_POST['login'] ?? '');
    $password = $_POST['password'] ?? '';

    if (empty($login) || empty($password)) {
        $errors[] = 'Informe login e senha da sua conta do jogo.';
    }

    if (!$errors) {
        $stmt = $mysqli->prepare('SELECT login, password FROM accounts WHERE login = ?');
        if ($stmt) {
            $stmt->bind_param('s', $login);
            $stmt->execute();
            $stmt->store_result();
            if ($stmt->num_rows === 1) {
                $dbLogin = '';
                $dbPass = '';
                $stmt->bind_result($dbLogin, $dbPass);
                $stmt->fetch();

                $expectedHash = base64_encode(sha1($password, true));
                if (hash_equals($dbPass, $expectedHash)) {
                    session_regenerate_id(true);
                    $_SESSION['auth_login'] = $dbLogin;
                    $stmt->close();
                    header('Location: doar.php');
                    exit;
                } else {
                    $errors[] = 'Login ou senha incorretos.';
                }
            } else {
                $errors[] = 'Login ou senha incorretos.';
            }
            $stmt->close();
        } else {
            $errors[] = 'Erro interno ao consultar contas. Tente novamente.';
        }
    }
}

$csrfToken = generateCsrfToken();
require 'header.php';
?>

<section class="section">
  <div class="section-head">
    <h2>Acessar Minha Conta</h2>
    <p>Entre com os dados da sua conta de jogo para apoiar a manutenção do <?php echo h($serverName); ?>.</p>
  </div>

  <div class="form-card">
    <?php foreach ($errors as $error): ?>
      <div class="alert alert-error" role="alert">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 9v4M12 17h.01M10.3 3.9L2.6 18a1.5 1.5 0 0 0 1.3 2.2h16.2a1.5 1.5 0 0 0 1.3-2.2L13.7 3.9a1.5 1.5 0 0 0-2.6 0z"/></svg>
        <span><?php echo h($error); ?></span>
      </div>
    <?php endforeach; ?>

    <form method="post" action="login.php" novalidate>
      <input type="hidden" name="csrf_token" value="<?php echo h($csrfToken); ?>">
      
      <div class="field">
        <label for="login">Login da Conta</label>
        <input type="text" id="login" name="login" maxlength="14" autocomplete="username" value="<?php echo isset($login) ? h($login) : ''; ?>" required>
      </div>

      <div class="field">
        <label for="password">Senha</label>
        <input type="password" id="password" name="password" maxlength="16" autocomplete="current-password" required>
      </div>

      <button type="submit" class="btn btn-primary btn-block">Entrar</button>
    </form>

    <div style="margin-top: 1.5rem; text-align: center; font-size: 0.9rem; color: #a0aec0;">
      Ainda não tem conta no servidor? <a href="register.php" style="color: #ecc94b; text-decoration: underline;">Cadastre-se aqui</a>.
    </div>
  </div>
</section>

<?php require 'footer.php'; ?>
