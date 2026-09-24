<?php require 'config.php'; $activePage = 'register'; ?>
<?php
$errors = [];
$success = false;

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $login = trim($_POST['login'] ?? '');
    $password = $_POST['password'] ?? '';
    $password2 = $_POST['password2'] ?? '';

    if (!preg_match('/^[a-zA-Z0-9_]{4,14}$/', $login)) {
        $errors[] = 'Login deve ter de 4 a 14 caracteres (letras, números ou _).';
    }
    if (strlen($password) < 4 || strlen($password) > 16) {
        $errors[] = 'Senha deve ter de 4 a 16 caracteres.';
    }
    if ($password !== $password2) {
        $errors[] = 'As senhas não conferem.';
    }

    if (!$errors) {
        $stmt = $mysqli->prepare('SELECT login FROM accounts WHERE login = ?');
        $stmt->bind_param('s', $login);
        $stmt->execute();
        $stmt->store_result();
        if ($stmt->num_rows > 0) {
            $errors[] = 'Essa conta já existe.';
        }
        $stmt->close();
    }

    if (!$errors) {
        $hash = base64_encode(sha1($password, true));
        $stmt = $mysqli->prepare('INSERT INTO accounts (login, password, accessLevel) VALUES (?, ?, 0)');
        $stmt->bind_param('ss', $login, $hash);
        if ($stmt->execute()) {
            $success = true;
        } else {
            $errors[] = 'Falha ao criar a conta. Tente novamente.';
        }
        $stmt->close();
    }
}
?>
<?php require 'header.php'; ?>

<section class="section">
  <div class="section-head">
    <h2>Registrar Conta</h2>
    <p>Crie seu login para entrar no <?php echo h($serverName); ?>.</p>
  </div>

  <div class="form-card">
    <?php if ($success): ?>
      <div class="alert alert-success" role="status">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="10"/><path d="M8 12l3 3 5-6"/></svg>
        <span>Conta criada com sucesso! Você já pode abrir o cliente e entrar no jogo.</span>
      </div>
      <a class="btn btn-primary btn-block" href="index.php">Voltar ao início</a>
    <?php else: ?>
      <?php foreach ($errors as $error): ?>
        <div class="alert alert-error" role="alert">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 9v4M12 17h.01M10.3 3.9L2.6 18a1.5 1.5 0 0 0 1.3 2.2h16.2a1.5 1.5 0 0 0 1.3-2.2L13.7 3.9a1.5 1.5 0 0 0-2.6 0z"/></svg>
          <span><?php echo h($error); ?></span>
        </div>
      <?php endforeach; ?>

      <form method="post" action="register.php" novalidate>
        <div class="field">
          <label for="login">Login</label>
          <input type="text" id="login" name="login" maxlength="14" autocomplete="username" required>
          <p class="field-hint">4 a 14 caracteres: letras, números ou _</p>
        </div>
        <div class="field">
          <label for="password">Senha</label>
          <input type="password" id="password" name="password" maxlength="16" autocomplete="new-password" required>
        </div>
        <div class="field">
          <label for="password2">Repetir Senha</label>
          <input type="password" id="password2" name="password2" maxlength="16" autocomplete="new-password" required>
        </div>
        <button type="submit" class="btn btn-primary btn-block">Registrar</button>
      </form>
    <?php endif; ?>
  </div>
</section>
<?php require 'footer.php'; ?>
