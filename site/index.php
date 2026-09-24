<?php require 'config.php'; $activePage = 'index'; ?>
<?php require 'header.php'; ?>

<section class="hero">
  <div class="hero-bg" style="--hero-image:url('images/hero.png');"></div>
  <div class="hero-inner">
    <div class="hero-text">
      <?php if (isServerOnline($serverIp, $loginPort)): ?>
        <span class="eyebrow online"><span class="dot"></span> Servidor Online 24/7</span>
      <?php else: ?>
        <span class="eyebrow offline"><span class="dot"></span> Servidor Offline</span>
      <?php endif; ?>
      <h1>Bem-vindo ao<span class="accent"><?php echo h($serverName); ?></span></h1>
      <p class="lead">Servidor privado de Lineage II Interlude, feito para jogar com os amigos. Cadastre sua conta e entre na aventura.</p>
      <div class="cta-row">
        <a class="btn btn-primary" href="register.php">Registrar Agora</a>
        <a class="btn btn-secondary" href="rules.php">Ver Regras</a>
      </div>
      <div class="indicators">
        <div class="indicator">
          <span class="indicator-icon">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M22 12h-4l-3 9L9 3l-3 9H2"/></svg>
          </span>
          <span>
            <span class="indicator-title">ESTÁVEL</span><br>
            <span class="indicator-sub">99.9% Uptime</span>
          </span>
        </div>
        <div class="indicator">
          <span class="indicator-icon">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 2l8 3v6c0 5-3.5 8.5-8 11-4.5-2.5-8-6-8-11V5l8-3z"/></svg>
          </span>
          <span>
            <span class="indicator-title">PROTEGIDO</span><br>
            <span class="indicator-sub">Anti-Hack</span>
          </span>
        </div>
        <div class="indicator">
          <span class="indicator-icon">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="9" cy="7" r="4"/><path d="M2 21v-2a6 6 0 0 1 6-6h2a6 6 0 0 1 6 6v2"/><circle cx="17" cy="7" r="3"/><path d="M22 21v-2a5 5 0 0 0-3-4.6"/></svg>
          </span>
          <span>
            <span class="indicator-title">COMUNIDADE ATIVA</span><br>
            <span class="indicator-sub">Suporte e Eventos</span>
          </span>
        </div>
      </div>
    </div>
  </div>
</section>

<section class="section">
  <div class="section-head">
    <span class="section-eyebrow">→ Por que jogar aqui ←</span>
    <h2>Um servidor feito para você</h2>
    <p>Pequeno, estável e pensado para a diversão de verdade.</p>
  </div>
  <div class="grid">
    <div class="card feature-card">
      <span class="feature-icon">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M14.5 17.5L3 6V3h3l11.5 11.5"/><path d="M13 19l6-6"/><path d="M16 16l4 4"/><path d="M19 21l2-2"/></svg>
      </span>
      <h3>Chronicle Interlude</h3>
      <p>A versão clássica do Lineage II, com o gameplay original que marcou época.</p>
    </div>
    <div class="card feature-card">
      <span class="feature-icon">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="9" cy="7" r="4"/><path d="M2 21v-2a6 6 0 0 1 6-6h2a6 6 0 0 1 6 6v2"/><circle cx="17" cy="7" r="3"/><path d="M22 21v-2a5 5 0 0 0-3-4.6"/></svg>
      </span>
      <h3>Comunidade Fechada</h3>
      <p>Feito para um grupo de amigos, sem multidões nem overpower. Respeito e diversão em primeiro lugar.</p>
    </div>
    <div class="card feature-card">
      <span class="feature-icon">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 2l8 3v6c0 5-3.5 8.5-8 11-4.5-2.5-8-6-8-11V5l8-3z"/></svg>
      </span>
      <h3>Cadastro Simples</h3>
      <p>Crie sua conta em segundos e entre direto no jogo. Sem burocracia.</p>
    </div>
  </div>
</section>

<div class="cta-final">
  <div class="cta-final-inner">
    <div class="cta-final-left">
      <span class="cta-final-shield">
        <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 2l8 3v6c0 5-3.5 8.5-8 11-4.5-2.5-8-6-8-11V5l8-3z"/></svg>
      </span>
      <div>
        <h2>Pronto para aventura?</h2>
        <p>Junte-se à nossa comunidade e escreva sua história no mundo de Lineage II.</p>
      </div>
    </div>
    <a class="btn btn-primary" href="register.php">Criar Conta Agora →</a>
  </div>
</div>

<?php require 'footer.php'; ?>
