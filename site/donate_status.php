<?php
require 'config.php';
requireAuth();
$activePage = 'doar';

$ref = trim($_GET['ref'] ?? '');
if (empty($ref)) {
    header('Location: doar.php');
    exit;
}

$login = getLoggedInUser();
$stmt = $mysqli->prepare('SELECT id, login, external_reference, payment_id, amount, currency, status, pix_qr_code, pix_qr_code_base64, created_at, approved_at FROM donations WHERE external_reference = ? AND login = ? LIMIT 1');
if (!$stmt) {
    die('Erro ao consultar doação.');
}

$stmt->bind_param('ss', $ref, $login);
$stmt->execute();
$result = $stmt->get_result();
$donation = $result ? $result->fetch_assoc() : null;
$stmt->close();

if (!$donation) {
    header('Location: doar.php?err=' . urlencode('Doação não encontrada.'));
    exit;
}

$isPending = ($donation['status'] === 'pending');
$isApproved = ($donation['status'] === 'approved');
$noticeNoToken = isset($_GET['notice']) && $_GET['notice'] === 'no_token';

require 'header.php';
?>

<section class="section">
  <div class="section-head">
    <h2>Status da Contribuição</h2>
    <p>Acompanhe o pagamento Pix da sua conta <strong><?php echo h($login); ?></strong>.</p>
  </div>

  <div class="form-card" style="max-width: 600px;">
    <?php if ($noticeNoToken): ?>
      <div class="alert alert-error" role="alert">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="10"/><path d="M12 8v4M12 16h.01"/></svg>
        <span><strong>Aviso de Configuração:</strong> O Access Token do Mercado Pago (<code>MP_ACCESS_TOKEN</code>) ainda não foi definido no ambiente do servidor. Defina a variável no <code>.env</code> para gerar cobranças reais.</span>
      </div>
    <?php endif; ?>

    <div style="display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--border); padding-bottom: 14px; margin-bottom: 16px;">
      <div>
        <span style="font-size: 0.85rem; color: var(--muted); display: block;">Valor da Contribuição</span>
        <strong style="font-size: 1.4rem; color: var(--primary);">R$ <?php echo number_format((float)$donation['amount'], 2, ',', '.'); ?></strong>
      </div>
      <div id="statusBadgeWrap">
        <?php if ($isApproved): ?>
          <span class="badge-status badge-approved">PAGAMENTO APROVADO</span>
        <?php elseif ($isPending): ?>
          <span class="badge-status badge-pending">AGUARDANDO PAGAMENTO</span>
        <?php else: ?>
          <span class="badge-status badge-cancelled"><?php echo strtoupper(h($donation['status'])); ?></span>
        <?php endif; ?>
      </div>
    </div>

    <?php if ($isApproved): ?>
      <div class="alert alert-success" style="margin-top: 14px;">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M8 12l3 3 5-6"/></svg>
        <div>
          <strong>Muito obrigado pelo apoio!</strong><br>
          Sua contribuição voluntária foi confirmada e ajuda diretamente a manter o servidor online e seguro para toda a comunidade.
        </div>
      </div>
      <a href="doar.php" class="btn btn-secondary btn-block" style="margin-top: 20px;">Voltar à Página de Apoio</a>

    <?php elseif ($isPending): ?>
      <div id="pendingSection">
        <p style="font-size: 0.95rem; color: var(--muted); text-align: center; margin-bottom: 12px;">
          Abra o aplicativo do seu banco, escolha <strong>Pagar via Pix</strong> e escaneie o QR Code abaixo ou copie o código Pix:
        </p>

        <?php if (!empty($donation['pix_qr_code_base64'])): ?>
          <div class="pix-box">
            <img class="pix-qr-img" src="data:image/png;base64,<?php echo h($donation['pix_qr_code_base64']); ?>" alt="QR Code Pix">
            <span style="font-size: 0.85rem; color: var(--muted);">QR Code gerado diretamente pelo Mercado Pago</span>
          </div>
        <?php endif; ?>

        <?php if (!empty($donation['pix_qr_code'])): ?>
          <div style="margin-top: 20px;">
            <label style="font-size: 0.85rem; font-weight: 600; color: var(--muted); display: block; margin-bottom: 6px;">Código Pix Copia e Cola:</label>
            <div class="pix-code-wrap">
              <input type="text" class="pix-code-input" id="pixCodeInput" value="<?php echo h($donation['pix_qr_code']); ?>" readonly>
              <button type="button" class="btn btn-primary" id="copyPixBtn" style="padding: 0 16px; font-size: 0.9rem;">Copiar</button>
            </div>
          </div>
        <?php endif; ?>

        <div style="text-align: center; margin-top: 24px; font-size: 0.85rem; color: var(--muted);">
          <span style="display: inline-block; animation: pulse 2s infinite;">⏳</span>
          Esta tela verifica automaticamente a aprovação do seu pagamento. Não feche se preferir aguardar a confirmação em tempo real.
        </div>
      </div>

      <div id="approvedSection" style="display: none; margin-top: 20px;">
        <div class="alert alert-success">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M8 12l3 3 5-6"/></svg>
          <div>
            <strong>Contribuição Confirmada!</strong><br>
            Recebemos seu apoio. Muito obrigado por ajudar a manter o <?php echo h($serverName); ?>!
          </div>
        </div>
        <a href="doar.php" class="btn btn-primary btn-block" style="margin-top: 20px;">Concluir</a>
      </div>

    <?php else: ?>
      <div class="alert alert-error">
        <span>Esta cobrança está finalizada com status: <strong><?php echo h($donation['status']); ?></strong>.</span>
      </div>
      <a href="doar.php" class="btn btn-primary btn-block">Fazer Nova Contribuição</a>
    <?php endif; ?>

    <div style="margin-top: 24px; text-align: center;">
      <a href="doar.php" style="color: var(--muted); font-size: 0.9rem; text-decoration: underline;">Voltar às contribuições</a>
    </div>
  </div>
</section>

<script>
// Copiar código Pix
const copyBtn = document.getElementById('copyPixBtn');
if (copyBtn) {
  copyBtn.addEventListener('click', () => {
    const input = document.getElementById('pixCodeInput');
    input.select();
    input.setSelectionRange(0, 99999);
    navigator.clipboard.writeText(input.value).then(() => {
      copyBtn.textContent = 'Copiado!';
      setTimeout(() => { copyBtn.textContent = 'Copiar'; }, 2500);
    });
  });
}

// Verificação de status em tempo real
<?php if ($isPending): ?>
const externalRef = <?php echo json_encode($donation['external_reference']); ?>;
let pollInterval = setInterval(() => {
  fetch('donate_check.php?ref=' + encodeURIComponent(externalRef))
    .then(r => r.json())
    .then(data => {
      if (data && data.status === 'approved') {
        clearInterval(pollInterval);
        document.getElementById('statusBadgeWrap').innerHTML = '<span class="badge-status badge-approved">PAGAMENTO APROVADO</span>';
        const pendingSec = document.getElementById('pendingSection');
        if (pendingSec) pendingSec.style.display = 'none';
        const approvedSec = document.getElementById('approvedSection');
        if (approvedSec) approvedSec.style.display = 'block';
      } else if (data && (data.status === 'cancelled' || data.status === 'rejected')) {
        clearInterval(pollInterval);
        location.reload();
      }
    })
    .catch(() => {});
}, 4000);
<?php endif; ?>
</script>

<?php require 'footer.php'; ?>
