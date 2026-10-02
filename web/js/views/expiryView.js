/**
 * Smart Expiry Intelligence & Wastage Prevention Hub
 */
class ExpiryView {
  async render(container) {
    container.innerHTML = `
      <div class="view-header">
        <div class="view-title-group">
          <h1>Smart Expiry Intelligence Hub</h1>
          <p>Proactive pharmaceutical waste mitigation, AI clearance markdowns & quarantine compliance</p>
        </div>
        <div class="view-actions">
          <a href="/api/export/csv/expiry" target="_blank" class="btn btn-secondary btn-sm">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" x2="12" y1="15" y2="3"/></svg>
            Export Expiry Report (CSV)
          </a>
          <button class="btn btn-danger btn-sm" id="btn-quarantine-all-expired">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="7.86 2 16.14 2 22 7.86 22 16.14 16.14 22 7.86 22 2 16.14 2 7.86 7.86 2"/><line x1="15" x2="9" y1="9" y2="15"/><line x1="9" x2="15" y1="9" y2="15"/></svg>
            Quarantine Expired Batches
          </button>
        </div>
      </div>

      <!-- AI Clearance Markdown Recommendations Banner -->
      <div class="glass-card" style="margin-bottom: 24px; border-left: 4px solid var(--brand-primary); background: linear-gradient(135deg, rgba(6,182,212,0.1) 0%, rgba(17,24,39,0.7) 100%);">
        <div class="glass-panel-header">
          <div class="glass-panel-title">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--brand-primary)" stroke-width="2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
            AI-Powered Expiry Markdown Strategies
          </div>
          <button class="btn btn-primary btn-sm" id="btn-apply-all-markdowns">Bulk Apply Recommended Markdowns</button>
        </div>
        <p style="font-size: 13px; color: var(--text-secondary); margin-bottom: 16px;">
          The system evaluates velocity, days-to-expiry, and acquisition cost to suggest promotional markdowns that recover revenue prior to expiration date.
        </p>
        <div id="ai-recommendations-grid" style="display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 16px;">
          <div style="text-align: center; padding: 20px; color: var(--text-muted);">Generating markdown algorithms...</div>
        </div>
      </div>

      <!-- Critical & Expired Batches Tables -->
      <div style="display: flex; flex-direction: column; gap: 24px;">
        <!-- Critical Batches (<30 Days) -->
        <div class="glass-card">
          <div class="glass-panel-header">
            <div class="glass-panel-title" style="color: var(--status-critical);">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" x2="12" y1="8" y2="12"/><line x1="12" x2="12.01" y1="16" y2="16"/></svg>
              Critical Near-Expiry Batches (&lt; 30 Days Remaining)
            </div>
            <span class="badge badge-critical" id="crit-count-badge">0 Batches</span>
          </div>
          <div class="table-container">
            <table class="data-table">
              <thead>
                <tr>
                  <th>Batch #</th>
                  <th>Medicine</th>
                  <th>Units</th>
                  <th>Expiry Date</th>
                  <th>Days Left</th>
                  <th>Current Price</th>
                  <th>Current Markdown</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody id="crit-batches-tbody">
                <tr><td colspan="8" style="text-align: center; padding: 20px;">Scanning critical batches...</td></tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- Warning Batches (30-90 Days) -->
        <div class="glass-card">
          <div class="glass-panel-header">
            <div class="glass-panel-title" style="color: var(--status-warning);">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z"/><line x1="12" x2="12" y1="9" y2="13"/><line x1="12" x2="12.01" y1="17" y2="17"/></svg>
              Early Warning Batches (30 to 90 Days Remaining)
            </div>
            <span class="badge badge-warning" id="warn-count-badge">0 Batches</span>
          </div>
          <div class="table-container">
            <table class="data-table">
              <thead>
                <tr>
                  <th>Batch #</th>
                  <th>Medicine</th>
                  <th>Units</th>
                  <th>Expiry Date</th>
                  <th>Days Left</th>
                  <th>Current Price</th>
                  <th>Shelf Location</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody id="warn-batches-tbody">
                <tr><td colspan="8" style="text-align: center; padding: 20px;">Scanning early warning batches...</td></tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    `;

    // Load recommendations and batches
    this.loadExpiryData(container);

    container.querySelector('#btn-quarantine-all-expired').addEventListener('click', async () => {
      if (confirm('Quarantine all past-expiry batches in the system? This prevents accidental dispensing and routes them to the disposal register.')) {
        try {
          const expiredBatches = await window.api.getBatches({ status: 'EXPIRED' });
          for (const b of expiredBatches) {
            await window.api.quarantineBatch(b.id, 'Bulk quarantine by Expiry Hub');
          }
          window.toast.success('Quarantine Complete', `Quarantined ${expiredBatches.length} expired batches.`);
          this.loadExpiryData(container);
        } catch (e) {
          window.toast.error('Error', e.message);
        }
      }
    });

    container.querySelector('#btn-apply-all-markdowns').addEventListener('click', async () => {
      try {
        const recs = await window.api.getExpiryRecommendations();
        let applied = 0;
        for (const r of recs) {
          if (r.recommendedDiscountPercent > 0) {
            await window.api.applyMarkdown(r.batchId, r.recommendedDiscountPercent);
            applied++;
          }
        }
        window.toast.success('Markdowns Applied', `Applied AI discount strategies to ${applied} near-expiry batches!`);
        this.loadExpiryData(container);
      } catch (e) {
        window.toast.error('Error', e.message);
      }
    });
  }

  async loadExpiryData(container) {
    try {
      const [recs, critBatches, warnBatches] = await Promise.all([
        window.api.getExpiryRecommendations(),
        window.api.getBatches({ status: 'CRITICAL' }),
        window.api.getBatches({ status: 'WARNING' })
      ]);

      this.renderRecommendations(container, recs);
      this.renderCriticalBatches(container, critBatches);
      this.renderWarningBatches(container, warnBatches);

    } catch (e) {
      console.error('Expiry Hub error:', e);
      window.toast.error('Load Error', e.message);
    }
  }

  renderRecommendations(container, recs) {
    const grid = container.querySelector('#ai-recommendations-grid');
    if (!recs || recs.length === 0) {
      grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; color: var(--status-safe); padding: 20px;">✅ All batches are healthy! No markdown actions needed.</div>';
      return;
    }

    grid.innerHTML = recs.slice(0, 4).map(r => `
      <div style="background: var(--bg-surface); border: 1px solid var(--bg-card-border); border-radius: var(--radius-md); padding: 16px; display: flex; flex-direction: column; justify-content: space-between;">
        <div>
          <div style="display: flex; justify-content: space-between; align-items: flex-start;">
            <strong style="font-size: 14px;">${r.medicineName}</strong>
            <span class="badge ${r.daysToExpiry < 0 ? 'badge-danger' : (r.daysToExpiry <= 30 ? 'badge-critical' : 'badge-warning')}">
              ${r.daysToExpiry < 0 ? 'EXPIRED' : `${r.daysToExpiry}d left`}
            </span>
          </div>
          <div style="font-size: 12px; color: var(--text-muted); font-family: monospace; margin-top: 2px;">Batch: ${r.batchNumber}</div>
          <p style="font-size: 12px; color: var(--text-secondary); margin: 10px 0;">${r.justification}</p>
        </div>

        <div style="margin-top: 10px; padding-top: 10px; border-top: 1px solid var(--bg-card-border); display: flex; align-items: center; justify-content: space-between;">
          <div>
            ${r.recommendedDiscountPercent > 0 ? `
              <span style="font-size: 11px; text-decoration: line-through; color: var(--text-muted);">$${r.currentPrice.toFixed(2)}</span>
              <strong style="font-size: 15px; color: var(--brand-primary);">$${r.recommendedSalePrice.toFixed(2)}</strong>
              <span class="badge badge-safe" style="font-size: 10px;">${r.recommendedDiscountPercent}% OFF</span>
            ` : '<span style="font-size: 12px; color: var(--status-danger);">Zero Saleable Value</span>'}
          </div>
          ${r.recommendedDiscountPercent > 0 ? `
            <button class="btn btn-outline-primary btn-sm btn-apply-rec" data-id="${r.batchId}" data-disc="${r.recommendedDiscountPercent}">
              Apply
            </button>
          ` : `
            <button class="btn btn-danger btn-sm btn-rec-quar" data-id="${r.batchId}">
              Quarantine
            </button>
          `}
        </div>
      </div>
    `).join('');

    grid.querySelectorAll('.btn-apply-rec').forEach(btn => {
      btn.addEventListener('click', async () => {
        try {
          await window.api.applyMarkdown(parseInt(btn.dataset.id), parseFloat(btn.dataset.disc));
          window.toast.success('Strategy Applied', `Applied ${btn.dataset.disc}% discount.`);
          this.loadExpiryData(container);
        } catch (e) {
          window.toast.error('Error', e.message);
        }
      });
    });

    grid.querySelectorAll('.btn-rec-quar').forEach(btn => {
      btn.addEventListener('click', async () => {
        try {
          await window.api.quarantineBatch(parseInt(btn.dataset.id), 'Quarantine advised by Expiry Engine');
          window.toast.success('Batch Quarantined', 'Batch isolated from active stock.');
          this.loadExpiryData(container);
        } catch (e) {
          window.toast.error('Error', e.message);
        }
      });
    });
  }

  renderCriticalBatches(container, batches) {
    const tbody = container.querySelector('#crit-batches-tbody');
    const badge = container.querySelector('#crit-count-badge');
    badge.textContent = `${batches.length} Batches`;

    if (batches.length === 0) {
      tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: var(--status-safe); padding: 20px;">✅ No critical batches (&lt;30 days) currently in inventory.</td></tr>';
      return;
    }

    tbody.innerHTML = batches.map(b => `
      <tr>
        <td><strong>${b.batchNumber}</strong></td>
        <td>
          <div style="font-weight: 700;">${b.medicineName}</div>
          <div style="font-size: 11px; color: var(--text-muted);">${b.genericName}</div>
        </td>
        <td><strong>${b.quantity}</strong> units</td>
        <td>${b.expiryDate}</td>
        <td><span class="badge badge-critical">${b.daysToExpiry} Days</span></td>
        <td>$${b.unitPrice.toFixed(2)}</td>
        <td>${b.discountPercent > 0 ? `<span class="badge badge-safe">${b.discountPercent}% OFF</span>` : '<span class="badge badge-neutral">No Discount</span>'}</td>
        <td>
          <div style="display: flex; gap: 6px;">
            <button class="btn btn-outline-primary btn-sm btn-quick-markdown" data-id="${b.id}" data-disc="30">
              Set 30% Off
            </button>
            <button class="btn btn-danger btn-sm btn-quick-quarantine" data-id="${b.id}">
              Quarantine
            </button>
          </div>
        </td>
      </tr>
    `).join('');

    tbody.querySelectorAll('.btn-quick-markdown').forEach(b => {
      b.addEventListener('click', async () => {
        await window.api.applyMarkdown(b.dataset.id, 30.0);
        window.toast.success('Discount Applied', 'Set 30% markdown on batch.');
        this.loadExpiryData(container);
      });
    });

    tbody.querySelectorAll('.btn-quick-quarantine').forEach(b => {
      b.addEventListener('click', async () => {
        await window.api.quarantineBatch(b.dataset.id, 'Quarantine triggered from Expiry view');
        window.toast.success('Batch Quarantined', 'Batch locked from sales.');
        this.loadExpiryData(container);
      });
    });
  }

  renderWarningBatches(container, batches) {
    const tbody = container.querySelector('#warn-batches-tbody');
    const badge = container.querySelector('#warn-count-badge');
    badge.textContent = `${batches.length} Batches`;

    if (batches.length === 0) {
      tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: var(--status-safe); padding: 20px;">✅ No warning batches (30-90 days) detected.</td></tr>';
      return;
    }

    tbody.innerHTML = batches.map(b => `
      <tr>
        <td><strong>${b.batchNumber}</strong></td>
        <td>
          <div style="font-weight: 700;">${b.medicineName}</div>
          <div style="font-size: 11px; color: var(--text-muted);">${b.genericName}</div>
        </td>
        <td><strong>${b.quantity}</strong> units</td>
        <td>${b.expiryDate}</td>
        <td><span class="badge badge-warning">${b.daysToExpiry} Days</span></td>
        <td>$${b.unitPrice.toFixed(2)}</td>
        <td>${b.shelfLocation}</td>
        <td>
          <button class="btn btn-secondary btn-sm btn-quick-markdown" data-id="${b.id}" data-disc="15">
            Set 15% Early Off
          </button>
        </td>
      </tr>
    `).join('');

    tbody.querySelectorAll('.btn-quick-markdown').forEach(b => {
      b.addEventListener('click', async () => {
        await window.api.applyMarkdown(b.dataset.id, 15.0);
        window.toast.success('Discount Applied', 'Set 15% early markdown.');
        this.loadExpiryData(container);
      });
    });
  }
}

window.expiryView = new ExpiryView();
