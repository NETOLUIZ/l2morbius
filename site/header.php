<!DOCTYPE html>
<html lang="pt-br">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title><?php echo h($serverName); ?></title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Cinzel:wght@600;700&family=Inter:wght@400;600;700&display=swap" rel="stylesheet">
<link rel="stylesheet" href="style.css">
</head>
<body>
<a class="skip-link" href="#main">Pular para o conteúdo</a>
<header class="site-header">
  <a class="brand" href="index.php">
    <img src="images/logo.jpg" alt="<?php echo h($serverName); ?>" width="34" height="34">
    <span class="brand-text">
      <span class="brand-name">KORENTECH</span>
      <span class="brand-sub">LINEAGE II INTERLUDE</span>
    </span>
  </a>
  <nav class="site-nav" aria-label="Navegação principal">
    <a href="index.php"<?php echo $activePage === 'index' ? ' aria-current="page"' : ''; ?>>Início</a>
    <a href="rules.php"<?php echo $activePage === 'rules' ? ' aria-current="page"' : ''; ?>>Regras</a>
    <a href="downloads.php"<?php echo $activePage === 'downloads' ? ' aria-current="page"' : ''; ?>>Downloads</a>
    <a href="ranking.php"<?php echo $activePage === 'ranking' ? ' aria-current="page"' : ''; ?>>Ranking</a>
    <a href="bosses.php"<?php echo $activePage === 'bosses' ? ' aria-current="page"' : ''; ?>>Bosses</a>
  </nav>
  <div class="header-right">
    <?php $online = isServerOnline($serverIp, $loginPort); ?>
    <span class="status-badge <?php echo $online ? 'online' : 'offline'; ?>">
      <span class="dot"></span>
      <span class="sub-wrap">
        <strong><?php echo $online ? 'Servidor Online' : 'Servidor Offline'; ?></strong>
        <span class="sub"><?php echo $online ? getOnlinePlayerCount($mysqli) . ' Jogadores' : 'Tente novamente em instantes'; ?></span>
      </span>
    </span>
    <a class="btn btn-primary" href="register.php">Registrar Agora</a>
  </div>
</header>
<main id="main">
