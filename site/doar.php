<?php
require 'config.php';
requireAuth();
$activePage = 'doar';

$login = getLoggedInUser();

// Consulta doacoes recentes da conta
$donations = [];
$stmt = $mysqli->prepare('SELECT id, external_reference, amount, currency, status, created_at, approved_at FROM donations WHERE login = ? ORDER BY id DESC LIMIT 10');
if ($stmt) {
    $stmt->bind_param('s', $login);
    $stmt->execute();
    $result = $stmt->get_result();
    if ($result) {
        while ($row = $result->fetch_assoc()) {
            $donations[] = $row;
        }
    }
    $stmt->close();
}

$csrfToken = generateCsrfToken();
require 'header.php';
?>

<section class="section">
  <div class="section-head">
    <h2>Apoie o Servidor</h2>
    <p>Contribuição voluntária vinculada à conta: <strong style="color: #ecc94b;"><?php echo h($login); ?></strong></p>
  </div>

  <div class="form-card" style="max-width: 640px;">
    <div class="donation-notice" role="note">
      <strong>⚠️ AVISO IMPORTANTE</strong>
      Esta é uma contribuição voluntária destinada exclusivamente a custear os custos de infraestrutura e hospedagem do servidor. Não é uma compra de itens, privilégios ou vantagens no jogo. Todos os itens e conquistas especiais são obtidos exclusivamente jogando.
    </div>

    <?php if (isset($_GET['err'])): ?>
      <div class="alert alert-error" role="alert">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 9v4M12 17h.01M10.3 3.9L2.6 18a1.5 1.5 0 0 0 1.3 2.2h16.2a1.5 1.5 0 0 0 1.3-2.2L13.7 3.9a1.5 1.5 0 0 0-2.6 0z"/></svg>
        <span><?php echo h($_GET['err']); ?></span>
      </div>
    <?php endif; ?>

    <form method="post" action="donate_create.php" id="donationForm">
      <input type="hidden" name="csrf_token" value="<?php echo h($csrfToken); ?>">

      <div class="field">
        <label>Selecione um valor sugerido:</label>
        <div class="amount-presets">
          <button type="button" class="amount-btn" data-val="10.00">R$ 10</button>
          <button type="button" class="amount-btn" data-val="25.00">R$ 25</button>
          <button type="button" class="amount-btn" data-val="50.00">R$ 50</button>
          <button type="button" class="amount-btn" data-val="100.00">R$ 100</button>
        </div>
      </div>

      <div class="field">
        <label for="amount">Ou digite o valor da contribuição (R$)</label>
        <input type="number" id="amount" name="amount" step="1.00" min="<?php echo number_format($minDonationAmount, 2, '.', ''); ?>" max="<?php echo number_format($maxDonationAmount, 2, '.', ''); ?>" value="25.00" required>
        <p class="field-hint">Mínimo: R$ <?php echo number_format($minDonationAmount, 2, ',', '.'); ?> | Máximo: R$ <?php echo number_format($maxDonationAmount, 2, ',', '.'); ?></p>
      </div>

      <button type="submit" class="btn btn-primary btn-block" style="font-size: 1.05rem; padding: 14px;">Gerar Pix via Mercado Pago</button>
    </form>
  </div>

  <?php if (!empty($donations)): ?>
    <div style="max-width: 640px; margin: 40px auto 0;">
      <h3 style="font-family: var(--font-heading); font-size: 1.2rem; margin-bottom: 16px; color: var(--fg);">Minhas Contribuições Recentes</h3>
      <div style="background: var(--surface); border: 1px solid var(--border); border-radius: 8px; overflow-x: auto;">
        <table style="width: 100%; border-collapse: collapse; text-align: left; font-size: 0.9rem;">
          <thead>
            <tr style="border-bottom: 1px solid var(--border); color: var(--muted);">
              <th style="padding: 12px 16px;">Data</th>
              <th style="padding: 12px 16px;">Valor</th>
              <th style="padding: 12px 16px;">Status</th>
              <th style="padding: 12px 16px; text-align: right;">Ação</th>
            </tr>
          </thead>
          <tbody>
            <?php foreach ($donations as $d): ?>
              <tr style="border-bottom: 1px solid rgba(255,255,255,0.05);">
                <td style="padding: 12px 16px; color: var(--muted);"><?php echo date('d/m/Y H:i', strtotime($d['created_at'])); ?></td>
                <td style="padding: 12px 16px; font-weight: 600;">R$ <?php echo number_format((float)$d['amount'], 2, ',', '.'); ?></td>
                <td style="padding: 12px 16px;">
                  <?php if ($d['status'] === 'approved'): ?>
                    <span class="badge-status badge-approved">Aprovado</span>
                  <?php elseif ($d['status'] === 'pending'): ?>
                    <span class="badge-status badge-pending">Pendente</span>
                  <?php else: ?>
                    <span class="badge-status badge-cancelled"><?php echo h($d['status']); ?></span>
                  <?php endif; ?>
                </td>
                <td style="padding: 12px 16px; text-align: right;">
                  <a href="donate_status.php?ref=<?php echo urlencode($d['external_reference']); ?>" class="btn btn-secondary" style="padding: 4px 10px; font-size: 0.8rem;">Ver Detalhes</a>
                </td>
              </tr>
            <?php endforeach; ?>
          </tbody>
        </table>
      </div>
    </div>
  <?php endif; ?>
</section>

<script>
document.querySelectorAll('.amount-btn').forEach(btn => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('.amount-btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    const input = document.getElementById('amount');
    input.value = btn.getAttribute('data-val');
  });
});
</script>

<?php require 'footer.php'; ?>
