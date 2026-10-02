/**
 * Dashboard View - Executive Healthcare & Stock Intelligence
 */
class DashboardView {
  async render(container) {
    container.innerHTML = `
      <div class="view-header">
        <div class="view-title-group">
          <h1>Clinical Inventory Dashboard</h1>
          <p>Real-time pharmaceutical stock monitoring, FEFO tracking & expiry prevention</p>
        </div>
        <div class="view-actions">
          <button class="btn btn-secondary btn-sm" id="btn-run-expiry-scan">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12a9 9 0 0 0-9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/><path d="M3 3v5h5"/><path d="M3 12a9 9 0 0 0 9 9 9.75 9.75 0 0 0 6.74-2.74L21 16"/><path d="M16 16h5v5"/></svg>
            Run Expiry Sweep
          </button>
          <button class="btn btn-gradient btn-sm" id="btn-quick-pos">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect width="20" height="14" x="2" y="5" rx="2"/><line x1="2" x2="22" y1="10" y2="10"/></svg>
            Launch POS
          </button>
        </div>
      </div>

      <!-- KPI Summary Grid -->
      <div class="stats-grid" id="dashboard-stats-grid">
        <div class="kpi-card"><div class="kpi-info-group"><span class="kpi-label">Loading stats...</span></div></div>
      </div>

      <!-- Two-Column Analytics Section -->
      <div style="display: grid; grid-template-columns: 2fr 1fr; gap: 24px; margin-bottom: 28px;">
        <!-- Left: Category Distribution Chart -->
        <div class="glass-card">
          <div class="glass-panel-header">
            <div class="glass-panel-title">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--brand-primary)" stroke-width="2"><path d="M3 3v18h18"/><path d="M18 17V9"/><path d="M13 17V5"/><path d="M8 17v-3"/></svg>
              Inventory Value by Therapeutic Category
            </div>
          </div>
          <div id="category-chart-container" style="height: 240px; display: flex; align-items: flex-end; gap: 16px; padding-top: 20px;">
            <!-- Category bars rendered dynamically -->
          </div>
        </div>

        <!-- Right: Expiry Timeline Health -->
        <div class="glass-card">
          <div class="glass-panel-header">
            <div class="glass-panel-title">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--status-critical)" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
              Batch Expiry Health Status
            </div>
          </div>
          <div id="expiry-health-meters" style="display: flex; flex-direction: column; gap: 16px; padding-top: 10px;">
            <!-- Health meters rendered dynamically -->
          </div>
        </div>
      </div>

      <!-- Urgent Expiry Action Center -->
      <div class="glass-card" style="margin-bottom: 28px;">
        <div class="glass-panel-header">
          <div class="glass-panel-title">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--status-danger)" stroke-width="2"><path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z"/><line x1="12" x2="12" y1="9" y2="13"/><line x1="12" x2="12.01" y1="17" y2="17"/></svg>
            Urgent Expiry Action Center (Immediate Attention Required)
          </div>
          <a href="#expiry" class="btn btn-outline-primary btn-sm">View All Near-Expiry →</a>
        </div>
        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>Batch Number</th>
                <th>Medicine Name</th>
                <th>Category</th>
                <th>Stock Units</th>
                <th>Expiry Date</th>
                <th>Countdown</th>
                <th>Status</th>
                <th>Recommended Action</th>
              </tr>
            </thead>
            <tbody id="urgent-expiry-tbody">
              <tr><td colspan="8" style="text-align: center; padding: 20px;">Scanning batches...</td></tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Recent Sales Ledger Feed -->
      <div class="glass-card">
        <div class="glass-panel-header">
          <div class="glass-panel-title">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--status-safe)" stroke-width="2"><line x1="12" x2="12" y1="2" y2="22"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>
            Recent Dispensed Invoices & POS Stream
          </div>
          <a href="#pos" class="btn btn-secondary btn-sm">Open Register</a>
        </div>
        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>Invoice #</th>
                <th>Customer / Patient</th>
                <th>Doctor</th>
                <th>Payment Mode</th>
                <th>Items Count</th>
                <th>Total Paid</th>
                <th>Timestamp</th>
              </tr>
            </thead>
            <tbody id="recent-sales-tbody">
              <tr><td colspan="7" style="text-align: center; padding: 20px;">Loading recent sales...</td></tr>
            </tbody>
          </table>
        </div>
      </div>
    `;

    // Bind Event Listeners
    container.querySelector('#btn-quick-pos').addEventListener('click', () => {
      window.router.navigate('#pos');
    });

    container.querySelector('#btn-run-expiry-scan').addEventListener('click', async () => {
      try {
        const res = await window.api.runExpiryScan();
        window.toast.success('Expiry Sweep Completed', res.message);
        this.loadDashboardData(container);
      } catch (e) {
        window.toast.error('Sweep Failed', e.message);
      }
    });

    this.loadDashboardData(container);
  }

  async loadDashboardData(container) {
    try {
      const [stats, categories, urgentBatches, sales] = await Promise.all([
        window.api.getDashboardStats(),
        window.api.getCategoryAnalytics(),
        window.api.getBatches({ status: 'CRITICAL', limit: 8 }),
        window.api.getSales({ limit: 6 })
      ]);

      this.renderKPIs(container, stats);
      this.renderCategoryChart(container, categories);
      this.renderExpiryHealth(container, stats);
      this.renderUrgentBatches(container, urgentBatches);
      this.renderRecentSales(container, sales);

    } catch (err) {
      console.error('Error loading dashboard:', err);
      window.toast.error('Dashboard Load Error', err.message);
    }
  }

  renderKPIs(container, s) {
    const grid = container.querySelector('#dashboard-stats-grid');
    grid.innerHTML = `
      <div class="kpi-card">
        <div class="kpi-info-group">
          <span class="kpi-label">Active Medicines</span>
          <span class="kpi-value">${s.totalMedicines || 0}</span>
          <span class="kpi-subtitle">📦 ${s.totalStockUnits || 0} units in inventory</span>
        </div>
        <div class="kpi-icon-box cyan">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m10.5 20.5 10-10a4.95 4.95 0 1 0-7-7l-10 10a4.95 4.95 0 1 0 7 7Z"/><path d="m8.5 8.5 7 7"/></svg>
        </div>
      </div>

      <div class="kpi-card safe">
        <div class="kpi-info-group">
          <span class="kpi-label">Stock Valuation (Retail)</span>
          <span class="kpi-value">$${(s.inventoryRetailValue || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span>
          <span class="kpi-subtitle" style="color: var(--status-safe);">Cost: $${(s.inventoryCostValue || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span>
        </div>
        <div class="kpi-icon-box emerald">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" x2="12" y1="2" y2="22"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>
        </div>
      </div>

      <div class="kpi-card critical">
        <div class="kpi-info-group">
          <span class="kpi-label">Critical Expiring (&lt;30 Days)</span>
          <span class="kpi-value" style="color: var(--status-critical);">${s.criticalCount || 0}</span>
          <span class="kpi-subtitle" style="color: var(--status-critical);">⚠️ $${(s.criticalValue || 0).toFixed(2)} at risk</span>
        </div>
        <div class="kpi-icon-box orange">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" x2="12" y1="8" y2="12"/><line x1="12" x2="12.01" y1="16" y2="16"/></svg>
        </div>
      </div>

      <div class="kpi-card danger">
        <div class="kpi-info-group">
          <span class="kpi-label">Expired / Quarantined</span>
          <span class="kpi-value" style="color: var(--status-danger);">${s.expiredCount || 0}</span>
          <span class="kpi-subtitle" style="color: var(--status-danger);">Loss: $${(s.expiredLossValue || 0).toFixed(2)}</span>
        </div>
        <div class="kpi-icon-box rose">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="7.86 2 16.14 2 22 7.86 22 16.14 16.14 22 7.86 22 2 16.14 2 7.86 7.86 2"/><line x1="15" x2="9" y1="9" y2="15"/><line x1="9" x2="15" y1="9" y2="15"/></svg>
        </div>
      </div>

      <div class="kpi-card warning">
        <div class="kpi-info-group">
          <span class="kpi-label">Low Stock Alerts</span>
          <span class="kpi-value" style="color: var(--status-warning);">${s.lowStockCount || 0}</span>
          <span class="kpi-subtitle">Below safety threshold</span>
        </div>
        <div class="kpi-icon-box amber">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 12h-4l-3 9L9 3l-3 9H2"/></svg>
        </div>
      </div>

      <div class="kpi-card">
        <div class="kpi-info-group">
          <span class="kpi-label">Today's Sales</span>
          <span class="kpi-value">$${(s.todaySalesAmount || 0).toFixed(2)}</span>
          <span class="kpi-subtitle">💳 ${s.todaySalesCount || 0} prescriptions dispensed</span>
        </div>
        <div class="kpi-icon-box cyan">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect width="20" height="14" x="2" y="5" rx="2"/><line x1="2" x2="22" y1="10" y2="10"/></svg>
        </div>
      </div>
    `;
  }

  renderCategoryChart(container, categories) {
    const chart = container.querySelector('#category-chart-container');
    if (!categories || categories.length === 0) {
      chart.innerHTML = '<div style="color: var(--text-muted); margin: auto;">No category data available.</div>';
      return;
    }

    const maxVal = Math.max(...categories.map(c => c.retailValue || 1));
    const topCats = categories.slice(0, 6);

    chart.innerHTML = topCats.map(c => {
      const heightPercent = Math.max(15, Math.min(100, Math.round((c.retailValue / maxVal) * 100)));
      return `
        <div style="flex: 1; display: flex; flex-direction: column; align-items: center; height: 100%; justify-content: flex-end; gap: 8px;">
          <div style="font-size: 11px; font-weight: 700; color: var(--brand-primary);">$${Math.round(c.retailValue)}</div>
          <div style="width: 100%; height: ${heightPercent}%; background: var(--brand-gradient); border-radius: var(--radius-sm) var(--radius-sm) 0 0; transition: height 0.5s ease; box-shadow: var(--glow-cyan);"></div>
          <div style="font-size: 10.5px; font-weight: 600; color: var(--text-secondary); text-align: center; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 90px;" title="${c.categoryName}">
            ${c.categoryName.split(' ')[0]}
          </div>
        </div>
      `;
    }).join('');
  }

  renderExpiryHealth(container, s) {
    const box = container.querySelector('#expiry-health-meters');
    const total = (s.totalBatches || 1);
    const safeP = Math.round(((s.safeCount || 0) / total) * 100);
    const warnP = Math.round(((s.warningCount || 0) / total) * 100);
    const critP = Math.round(((s.criticalCount || 0) / total) * 100);
    const expP = Math.round(((s.expiredCount || 0) / total) * 100);

    box.innerHTML = `
      <div class="expiry-meter-wrap">
        <div class="expiry-meter-text">
          <span style="color: var(--status-safe);">🟢 Safe Stock (&gt;90 Days)</span>
          <span>${s.safeCount || 0} batches (${safeP}%)</span>
        </div>
        <div class="expiry-meter-bar"><div class="expiry-meter-fill safe" style="width: ${safeP}%"></div></div>
      </div>

      <div class="expiry-meter-wrap">
        <div class="expiry-meter-text">
          <span style="color: var(--status-warning);">🟡 Warning (30-90 Days)</span>
          <span>${s.warningCount || 0} batches (${warnP}%)</span>
        </div>
        <div class="expiry-meter-bar"><div class="expiry-meter-fill warning" style="width: ${warnP}%"></div></div>
      </div>

      <div class="expiry-meter-wrap">
        <div class="expiry-meter-text">
          <span style="color: var(--status-critical);">🟠 Critical (&lt;30 Days)</span>
          <span>${s.criticalCount || 0} batches (${critP}%)</span>
        </div>
        <div class="expiry-meter-bar"><div class="expiry-meter-fill critical" style="width: ${critP}%"></div></div>
      </div>

      <div class="expiry-meter-wrap">
        <div class="expiry-meter-text">
          <span style="color: var(--status-danger);">🔴 Expired Stock</span>
          <span>${s.expiredCount || 0} batches (${expP}%)</span>
        </div>
        <div class="expiry-meter-bar"><div class="expiry-meter-fill danger" style="width: ${expP}%"></div></div>
      </div>
    `;
  }

  renderUrgentBatches(container, batches) {
    const tbody = container.querySelector('#urgent-expiry-tbody');
    if (!batches || batches.length === 0) {
      tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: var(--status-safe); padding: 20px;">✅ No critical near-expiry batches detected at this time!</td></tr>';
      return;
    }

    tbody.innerHTML = batches.map(b => {
      const days = b.daysToExpiry;
      const isExp = days < 0;
      const badgeClass = isExp ? 'badge-danger' : 'badge-critical';
      const badgeText = isExp ? 'EXPIRED' : `${days} DAYS LEFT`;

      return `
        <tr>
          <td><strong>${b.batchNumber}</strong></td>
          <td>
            <div style="font-weight: 700;">${b.medicineName}</div>
            <div style="font-size: 11px; color: var(--text-muted);">${b.genericName} (${b.strength})</div>
          </td>
          <td><span class="badge badge-neutral">${b.categoryName || 'General'}</span></td>
          <td><strong>${b.quantity}</strong> units</td>
          <td>${b.expiryDate}</td>
          <td><span class="badge ${badgeClass}">${badgeText}</span></td>
          <td>${b.shelfLocation}</td>
          <td>
            <div style="display: flex; gap: 6px;">
              ${!isExp ? `
                <button class="btn btn-outline-primary btn-sm btn-quick-discount" data-id="${b.id}" data-num="${b.batchNumber}">
                  Markdown 25%
                </button>
              ` : `
                <button class="btn btn-danger btn-sm btn-quick-quarantine" data-id="${b.id}" data-num="${b.batchNumber}">
                  Quarantine
                </button>
              `}
            </div>
          </td>
        </tr>
      `;
    }).join('');

    // Bind Quick Action buttons
    tbody.querySelectorAll('.btn-quick-discount').forEach(btn => {
      btn.addEventListener('click', async () => {
        const id = btn.dataset.id;
        try {
          await window.api.discountBatch(id, 25.0);
          window.toast.success('Markdown Applied', `Applied 25% discount to batch ${btn.dataset.num}`);
          this.loadDashboardData(container);
        } catch (e) {
          window.toast.error('Error', e.message);
        }
      });
    });

    tbody.querySelectorAll('.btn-quick-quarantine').forEach(btn => {
      btn.addEventListener('click', async () => {
        const id = btn.dataset.id;
        try {
          await window.api.quarantineBatch(id, 'Quarantined from Dashboard urgent action center');
          window.toast.success('Batch Quarantined', `Locked batch ${btn.dataset.num} for disposal.`);
          this.loadDashboardData(container);
        } catch (e) {
          window.toast.error('Error', e.message);
        }
      });
    });
  }

  renderRecentSales(container, sales) {
    const tbody = container.querySelector('#recent-sales-tbody');
    if (!sales || sales.length === 0) {
      tbody.innerHTML = '<tr><td colspan="7" style="text-align: center; padding: 20px;">No sales transactions recorded yet.</td></tr>';
      return;
    }

    tbody.innerHTML = sales.map(s => `
      <tr>
        <td><strong>${s.invoiceNumber}</strong></td>
        <td>
          <div style="font-weight: 600;">${s.customerName}</div>
          <div style="font-size: 11px; color: var(--text-muted);">${s.customerPhone || 'No phone'}</div>
        </td>
        <td>${s.doctorName || 'Over The Counter'}</td>
        <td><span class="badge badge-neutral">${s.paymentMethod}</span></td>
        <td>${s.items ? s.items.length : 2} items</td>
        <td style="font-weight: 800; color: var(--brand-primary);">$${s.finalAmount.toFixed(2)}</td>
        <td style="font-size: 11.5px; color: var(--text-muted);">${s.saleDate}</td>
      </tr>
    `).join('');
  }
}

window.dashboardView = new DashboardView();
