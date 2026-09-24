<?php require 'config.php'; $activePage = 'bosses'; ?>
<?php require 'header.php'; ?>

<section class="section boss-ledger">
  <div class="section-head">
    <h2>Bosses do Servidor</h2>
    <p>Todo Raid Boss e Grand Boss acima do nível 50 no <?php echo h($serverName); ?>, com a tabela de drop completa de cada um.</p>
  </div>

  <div class="bl-tally" id="blTally"></div>

  <div class="bl-controls">
    <div class="bl-search-wrap">
      <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="7"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
      <input type="search" id="blSearch" placeholder="Buscar boss ou item…" autocomplete="off">
    </div>
    <div class="bl-chip-group" role="group" aria-label="Filtrar por tipo">
      <button class="bl-chip" data-filter="all" aria-pressed="true">Todos</button>
      <button class="bl-chip" data-filter="RaidBoss" aria-pressed="false">Raid</button>
      <button class="bl-chip" data-filter="GrandBoss" aria-pressed="false">Grand</button>
    </div>
    <select id="blSort" aria-label="Ordenar">
      <option value="lvl-asc">Nível, menor → maior</option>
      <option value="lvl-desc">Nível, maior → menor</option>
      <option value="name-asc">Nome, A → Z</option>
      <option value="drops-desc">Mais drops</option>
    </select>
  </div>

  <div class="bl-list" id="blList"></div>
  <div class="bl-empty" id="blEmpty" hidden>Nenhum boss encontrado pra essa busca.</div>

  <p class="bl-note">A chance de drop é a rolagem individual de cada categoria, em porcentagem (itens da mesma linha podem sortear de forma independente).</p>
</section>

<style>
  .boss-ledger { padding-top: 48px; }

  .bl-tally {
    display: flex;
    gap: 28px;
    flex-wrap: wrap;
    justify-content: center;
    margin: -8px 0 32px;
  }
  .bl-tally-item { display: flex; flex-direction: column; align-items: center; gap: 2px; }
  .bl-tally-num {
    font-family: ui-monospace, "SFMono-Regular", Menlo, Consolas, monospace;
    font-variant-numeric: tabular-nums;
    font-size: 22px;
    font-weight: 700;
    color: var(--primary);
  }
  .bl-tally-label { font-size: 11px; text-transform: uppercase; letter-spacing: .08em; color: var(--muted); }

  .bl-controls {
    display: flex;
    gap: 10px;
    flex-wrap: wrap;
    align-items: center;
    margin: 0 auto 24px;
    max-width: 900px;
  }
  .bl-search-wrap { position: relative; flex: 1 1 240px; }
  .bl-search-wrap svg { position: absolute; left: 12px; top: 50%; transform: translateY(-50%); color: var(--muted); pointer-events: none; }
  .boss-ledger input[type="search"] {
    width: 100%;
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: var(--radius);
    padding: 11px 12px 11px 36px;
    font-family: inherit;
    font-size: 14px;
    color: var(--fg);
  }
  .boss-ledger input[type="search"]::placeholder { color: var(--muted); }

  .bl-chip-group { display: flex; gap: 4px; background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius); padding: 3px; }
  .bl-chip {
    font-size: 12.5px;
    letter-spacing: .02em;
    padding: 8px 13px;
    border-radius: 8px;
    border: none;
    background: transparent;
    color: var(--muted);
    cursor: pointer;
    font-family: inherit;
  }
  .bl-chip:hover { color: var(--fg); }
  .bl-chip[aria-pressed="true"] { background: rgba(217,169,40,.14); color: var(--primary); }

  .boss-ledger select#blSort {
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: var(--radius);
    padding: 10px 10px;
    font-family: inherit;
    font-size: 13px;
    color: var(--fg);
  }

  .bl-list { display: flex; flex-direction: column; gap: 8px; max-width: 900px; margin: 0 auto; }

  details.bl-boss { background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius); overflow: hidden; }
  details.bl-boss[open] { border-color: rgba(217,169,40,.35); }

  summary.bl-row {
    list-style: none;
    cursor: pointer;
    display: grid;
    grid-template-columns: 44px 1fr 90px 110px 20px;
    align-items: center;
    gap: 14px;
    padding: 14px 18px;
  }
  summary.bl-row::-webkit-details-marker { display: none; }

  .bl-lvl {
    font-family: ui-monospace, "SFMono-Regular", Menlo, Consolas, monospace;
    font-variant-numeric: tabular-nums;
    font-size: 13px;
    font-weight: 600;
    color: var(--fg);
    background: rgba(255,255,255,.05);
    border-radius: 6px;
    padding: 4px 0;
    text-align: center;
  }
  .bl-name { font-size: 14.5px; font-weight: 600; color: var(--fg); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

  .bl-pill { justify-self: start; font-size: 10.5px; letter-spacing: .05em; text-transform: uppercase; padding: 3px 9px; border-radius: 100px; white-space: nowrap; }
  .bl-pill.raid { color: var(--primary); background: rgba(217,169,40,.14); }
  .bl-pill.grand { color: #8b93e0; background: rgba(139,147,224,.14); }

  .bl-count { justify-self: end; font-size: 12px; color: var(--muted); white-space: nowrap; }
  .bl-chevron { color: var(--muted); transition: transform .15s ease; justify-self: end; }
  details[open] .bl-chevron { transform: rotate(180deg); }

  .bl-panel { border-top: 1px solid var(--border); padding: 4px 18px 16px 76px; }
  table.bl-drops { width: 100%; border-collapse: collapse; font-size: 13px; }
  table.bl-drops th {
    text-align: left;
    font-size: 10.5px;
    text-transform: uppercase;
    letter-spacing: .06em;
    color: var(--muted);
    font-weight: 600;
    padding: 8px 10px 6px 0;
    border-bottom: 1px solid var(--border);
  }
  table.bl-drops td { padding: 7px 10px 7px 0; border-bottom: 1px solid var(--border); color: var(--fg); }
  table.bl-drops tr:last-child td { border-bottom: none; }
  td.bl-qty, td.bl-pct, th.bl-qty, th.bl-pct { font-family: ui-monospace, "SFMono-Regular", Menlo, Consolas, monospace; font-variant-numeric: tabular-nums; white-space: nowrap; }
  td.bl-pct { color: var(--primary); font-weight: 600; }
  .bl-drops-wrap { overflow-x: auto; }

  .bl-empty { text-align: center; padding: 48px 16px; color: var(--muted); font-size: 14px; }
  .bl-note { max-width: 900px; margin: 28px auto 0; font-size: 12px; color: var(--muted); line-height: 1.6; }

  @media (max-width: 560px) {
    summary.bl-row {
      grid-template-columns: 36px 1fr 20px;
      grid-template-areas: "lvl name chev" "lvl type drops";
      row-gap: 6px;
    }
    .bl-lvl { grid-area: lvl; }
    .bl-name { grid-area: name; }
    .bl-pill { grid-area: type; }
    .bl-count { grid-area: drops; justify-self: start; }
    .bl-chevron { grid-area: chev; }
    .bl-panel { padding-left: 18px; }
  }
</style>

<script>
<?php
$scriptPath = __DIR__ . '/data/bosses_data.js';
if (file_exists($scriptPath)) {
    echo file_get_contents($scriptPath);
} else {
    echo 'const BOSSES = [];';
}
?>
</script>
<script>
(function () {
  const list = document.getElementById('blList');
  const tally = document.getElementById('blTally');
  const emptyState = document.getElementById('blEmpty');
  const searchBox = document.getElementById('blSearch');
  const sortSel = document.getElementById('blSort');
  const chips = document.querySelectorAll('.bl-chip');

  let typeFilter = 'all';
  let query = '';
  let sortMode = 'lvl-asc';

  const totalItems = new Set();
  BOSSES.forEach(b => b.drops.forEach(d => totalItems.add(d.n)));
  const levels = BOSSES.map(b => b.lvl);

  if (BOSSES.length) {
    tally.innerHTML = `
      <div class="bl-tally-item"><span class="bl-tally-num">${BOSSES.length}</span><span class="bl-tally-label">Bosses</span></div>
      <div class="bl-tally-item"><span class="bl-tally-num">${Math.min(...levels)}&ndash;${Math.max(...levels)}</span><span class="bl-tally-label">Faixa de nível</span></div>
      <div class="bl-tally-item"><span class="bl-tally-num">${totalItems.size}</span><span class="bl-tally-label">Itens únicos</span></div>
    `;
  }

  function fmtPct(p) {
    return (p >= 100 ? '100' : p.toFixed(p < 1 ? 3 : p < 10 ? 2 : 1)) + '%';
  }
  function fmtQty(min, max) {
    return min === max ? min.toLocaleString('pt-BR') : `${min.toLocaleString('pt-BR')}&ndash;${max.toLocaleString('pt-BR')}`;
  }
  function escapeHtml(s) {
    return s.replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  }

  function render() {
    const q = query.trim().toLowerCase();

    let rows = BOSSES.filter(b => {
      if (typeFilter !== 'all' && b.type !== typeFilter) return false;
      if (!q) return true;
      if (b.name.toLowerCase().includes(q)) return true;
      return b.drops.some(d => d.n.toLowerCase().includes(q));
    });

    rows = rows.slice().sort((a, b) => {
      if (sortMode === 'lvl-asc') return a.lvl - b.lvl;
      if (sortMode === 'lvl-desc') return b.lvl - a.lvl;
      if (sortMode === 'name-asc') return a.name.localeCompare(b.name);
      if (sortMode === 'drops-desc') return b.drops.length - a.drops.length;
      return 0;
    });

    list.innerHTML = '';
    emptyState.hidden = rows.length > 0;

    const frag = document.createDocumentFragment();
    rows.forEach(b => {
      const det = document.createElement('details');
      det.className = 'bl-boss';

      const typeLabel = b.type === 'GrandBoss' ? 'Grand' : 'Raid';
      const typeClass = b.type === 'GrandBoss' ? 'grand' : 'raid';

      const dropRows = b.drops.map(d => `
        <tr>
          <td>${escapeHtml(d.n)}</td>
          <td class="bl-qty">${fmtQty(d.min, d.max)}</td>
          <td class="bl-pct">${fmtPct(d.pct)}</td>
        </tr>
      `).join('');

      det.innerHTML = `
        <summary class="bl-row">
          <span class="bl-lvl">${b.lvl}</span>
          <span class="bl-name">${escapeHtml(b.name)}</span>
          <span class="bl-pill ${typeClass}">${typeLabel}</span>
          <span class="bl-count">${b.drops.length} drop${b.drops.length === 1 ? '' : 's'}</span>
          <svg class="bl-chevron" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"></polyline></svg>
        </summary>
        <div class="bl-panel">
          ${b.drops.length ? `
          <div class="bl-drops-wrap">
            <table class="bl-drops">
              <thead><tr><th>Item</th><th class="bl-qty">Qtd</th><th class="bl-pct">Chance</th></tr></thead>
              <tbody>${dropRows}</tbody>
            </table>
          </div>` : '<p style="color:var(--muted);font-size:13px;margin:8px 0;">Esse NPC não tem tabela de drop.</p>'}
        </div>
      `;
      frag.appendChild(det);
    });
    list.appendChild(frag);
  }

  searchBox.addEventListener('input', e => { query = e.target.value; render(); });
  sortSel.addEventListener('change', e => { sortMode = e.target.value; render(); });
  chips.forEach(chip => {
    chip.addEventListener('click', () => {
      chips.forEach(c => c.setAttribute('aria-pressed', 'false'));
      chip.setAttribute('aria-pressed', 'true');
      typeFilter = chip.dataset.filter;
      render();
    });
  });

  render();
})();
</script>

<?php require 'footer.php'; ?>
