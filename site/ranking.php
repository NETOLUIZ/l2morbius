<?php require 'config.php'; $activePage = 'ranking'; ?>
<?php require 'header.php'; ?>

<section class="section">
  <div class="section-head">
    <h2>Ranking</h2>
    <p>Os personagens mais fortes do <?php echo h($serverName); ?>.</p>
  </div>
  <div class="form-card" style="max-width:640px;">
    <?php
    $result = @mysqli_query($mysqli, "SELECT char_name, level, exp FROM characters ORDER BY level DESC, exp DESC LIMIT 10");
    if ($result && mysqli_num_rows($result) > 0):
    ?>
      <table style="width:100%; border-collapse:collapse;">
        <thead>
          <tr style="text-align:left; color:var(--muted); font-size:13px;">
            <th style="padding:8px 6px;">#</th>
            <th style="padding:8px 6px;">Personagem</th>
            <th style="padding:8px 6px;">Nível</th>
          </tr>
        </thead>
        <tbody>
          <?php $i = 1; while ($row = mysqli_fetch_assoc($result)): ?>
            <tr style="border-top:1px solid var(--border);">
              <td style="padding:10px 6px; color:var(--primary); font-weight:700;"><?php echo $i++; ?></td>
              <td style="padding:10px 6px;"><?php echo h($row['char_name']); ?></td>
              <td style="padding:10px 6px;"><?php echo (int) $row['level']; ?></td>
            </tr>
          <?php endwhile; ?>
        </tbody>
      </table>
    <?php else: ?>
      <p class="field-hint">Ainda não há personagens suficientes para exibir um ranking.</p>
    <?php endif; ?>
  </div>
</section>
<?php require 'footer.php'; ?>
