<?php require 'config.php'; $activePage = 'downloads'; ?>
<?php require 'header.php'; ?>

<section class="section">
  <div class="section-head">
    <h2>Downloads</h2>
    <p>Cliente e arquivos necessários para jogar no <?php echo h($serverName); ?>.</p>
  </div>
  <div class="grid">
    <div class="card feature-card">
      <span class="feature-icon">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 3v12"/><path d="M7 10l5 5 5-5"/><path d="M4 20h16"/></svg>
      </span>
      <h3>Cliente Interlude</h3>
      <p>Cliente completo pronto pra jogar no <?php echo h($serverName); ?>.</p>
      <a class="btn btn-primary" style="margin-top:12px;" href="https://drive.google.com/file/d/1oRtvHeAf_SFg8cGlsCwVV5bwFbVe34LD/view?usp=drive_link" target="_blank" rel="noopener">Baixar Cliente</a>
    </div>
    <div class="card feature-card">
      <span class="feature-icon">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 3v12"/><path d="M7 10l5 5 5-5"/><path d="M4 20h16"/></svg>
      </span>
      <h3>Patch / System Oficial</h3>
      <p>Pasta system oficial configurada e pronta para conectar ao servidor (15 MB).</p>
      <a class="btn btn-primary" style="margin-top:12px;" href="System_L2Korentech.zip" download>Baixar System (.zip)</a>
    </div>
  </div>

  <div class="form-card" style="margin-top:32px; max-width:680px;">
    <h4 style="margin-bottom:8px; color:var(--primary);">Como instalar:</h4>
    <ol style="margin-left:20px; line-height:1.7; color:var(--text-muted, #ccc);">
      <li>Baixe o <strong>Cliente Interlude</strong> se você ainda não tem o jogo instalado.</li>
      <li>Baixe a nossa <strong>System Oficial (.zip)</strong>.</li>
      <li>Extraia a pasta <code>system</code> dentro do diretório do seu jogo Lineage II (substituindo se pedir).</li>
      <li>Abra o jogo pelo arquivo <code>system/L2.exe</code>, coloque seu login e senha e bom jogo!</li>
    </ol>
  </div>
</section>
<?php require 'footer.php'; ?>
