<?php require 'config.php'; $activePage = 'rules'; ?>
<?php require 'header.php'; ?>

<section class="section">
  <div class="section-head">
    <h2>Regras do Servidor</h2>
    <p>Pra manter o jogo divertido pra todo mundo.</p>
  </div>
  <ul class="rules-list">
    <?php
    $rules = [
        'Proibido criar personagens ou contas com palavrões.',
        'Proibido usar linguagem ofensiva ou racista com outros jogadores.',
        'Proibido se passar por GM ou ADMIN.',
        'Proibido uso de bots ou cheats.',
        'Quebra das regras pode resultar em banimento.',
    ];
    foreach ($rules as $rule):
    ?>
      <li>
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="10"/><path d="M15 9l-6 6M9 9l6 6"/></svg>
        <span><?php echo h($rule); ?></span>
      </li>
    <?php endforeach; ?>
  </ul>
  <p class="field-hint" style="margin-top:16px;">Edite a lista <code>$rules</code> em rules.php para personalizar.</p>
</section>
<?php require 'footer.php'; ?>
