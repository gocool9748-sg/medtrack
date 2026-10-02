/**
 * Advanced Financial & Operational Analytics View
 */
class AnalyticsView {
  async render(container) {
    container.innerHTML = `
      <div class="view-header">
        <div class="view-title-group">
          <h1>Healthcare Business & Stock Analytics</h1>
          <p>Revenue trends, margin analysis, therapeutic distribution & wastage mitigation</p>
        </div>
        <div class="view-actions">
          <a href="/api/export/csv/sales" target="_blank" class="btn btn-secondary btn-sm">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" x2="12" y1="15" y2="3"/></svg>
            Export Sales Ledger (CSV)
          </a>
        </div>
      </div>

      <!-- Charts Grid -->
      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 24px; margin-bottom: 28px;">
        <!-- Top Fast Moving Drugs -->
        <div class="glass-card">
          <div class="glass-panel-header">
            <div class="glass-panel-title">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--brand-primary)" stroke-width="2"><path d="M12 2v20"/><path d="m17 5-5-3-5 3"/><path d="m17 19-5 3-5-3"/></svg>
              Top Fast-Moving Medicines (By Units Dispensed)
            </div>
          </div>
          <div id="top-meds-chart-box" style="display: flex; flex-direction: column; gap: 12px;">
            <div style="text-align: center; padding: 20px; color: var(--text-muted);">Analyzing sales transactions...</div>
          </div>
        </div>

        <!-- Monthly Financial Revenue Trend -->
        <div class="glass-card">
          <div class="glass-panel-header">
            <div class="glass-panel-title">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--status-safe)" stroke-width="2"><polyline points="23 6 13.5 15.5 8.5 10.5 1 18"/><polyline points="17 6 23 6 23 12"/></svg>
              Monthly Revenue Performance
            </div>
          </div>
          <div id="monthly-revenue-chart-box" style="height: 240px; display: flex; align-items: flex-end; gap: 20px; padding: 20px 10px 0 10px;">
            <div style="text-align: center; margin: auto; color: var(--text-muted);">Loading revenue history...</div>
          </div>
        </div>
      </div>

      <!-- Category Inventory & Margin Table -->
      <div class="glass-card">
        <div class="glass-panel-header">
          <div class="glass-panel-title">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--status-purple)" stroke-width="2"><rect width="7" height="9" x="3" y="3" rx="1"/><rect width="7" height="5" x="14" y="3" rx="1"/><rect width="7" height="9" x="14" y="12" rx="1"/><rect width="7" height="5" x="3" y="16" rx="1"/></svg>
            Therapeutic Category Margin & Valuation Analysis
          </div>
        </div>
        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>Category</th>
                <th>Drug Count</th>
                <th>Total Stock (Units)</th>
                <th>Inventory Acquisition Cost</th>
                <th>Retail Inventory Value</th>
                <th>Gross Potential Margin</th>
              </tr>
            </thead>
            <tbody id="analytics-categories-tbody">
              <tr><td colspan="6" style="text-align: center; padding: 24px;">Computing category metrics...</td></tr>
            </tbody>
          </table>
        </div>
      </div>
    `;

    this.loadAnalytics(container);
  }

  async loadAnalytics(container) {
    try {
      const [categories, topMeds, monthly] = await Promise.all([
        window.api.getCategoryAnalytics(),
        window.api.getTopMedicines(6),
        window.api.getFinancialTrends()
      ]);

      this.renderTopMeds(container, topMeds);
      this.renderMonthlyRevenue(container, monthly);
      this.renderCategoryTable(container, categories);

    } catch (e) {
      console.error('Analytics Error:', e);
      window.toast.error('Analytics Error', e.message);
    }
  }

  renderTopMeds(container, topMeds) {
    const box = container.querySelector('#top-meds-chart-box');
    if (!topMeds || topMeds.length === 0) {
      box.innerHTML = '<div style="text-align: center; padding: 20px; color: var(--text-muted);">No fast moving drug data recorded yet.</div>';
      return;
    }

    const maxUnits = Math.max(...topMeds.map(m => m.totalSold || 1));

    box.innerHTML = topMeds.map(m => {
      const pct = Math.max(10, Math.round((m.totalSold / maxUnits) * 100));
      return `
        <div>
          <div style="display: flex; justify-content: space-between; font-size: 13px; margin-bottom: 4px;">
            <span><strong>${m.name}</strong> <small style="color: var(--text-muted);">(${m.dosageForm})</small></span>
            <span><strong style="color: var(--brand-primary);">${m.totalSold} units</strong> ($${m.totalRevenue.toFixed(2)})</span>
          </div>
          <div style="height: 8px; width: 100%; background: var(--bg-surface-elevated); border-radius: var(--radius-full); overflow: hidden;">
            <div style="height: 100%; width: ${pct}%; background: var(--brand-gradient); border-radius: var(--radius-full);"></div>
          </div>
        </div>
      `;
    }).join('');
  }

  renderMonthlyRevenue(container, monthly) {
    const box = container.querySelector('#monthly-revenue-chart-box');
    if (!monthly || monthly.length === 0) {
      box.innerHTML = '<div style="text-align: center; margin: auto; color: var(--text-muted);">No monthly transaction data available.</div>';
      return;
    }

    const maxRev = Math.max(...monthly.map(m => m.revenue || 1));

    box.innerHTML = monthly.map(m => {
      const heightP = Math.max(15, Math.min(100, Math.round((m.revenue / maxRev) * 100)));
      return `
        <div style="flex: 1; display: flex; flex-direction: column; align-items: center; height: 100%; justify-content: flex-end; gap: 8px;">
          <div style="font-size: 11px; font-weight: 700; color: var(--status-safe);">$${Math.round(m.revenue)}</div>
          <div style="width: 100%; height: ${heightP}%; background: var(--brand-gradient-teal); border-radius: var(--radius-sm) var(--radius-sm) 0 0; box-shadow: var(--glow-emerald); transition: height 0.5s ease;"></div>
          <div style="font-size: 11px; font-weight: 600; color: var(--text-secondary);">${m.month}</div>
        </div>
      `;
    }).join('');
  }

  renderCategoryTable(container, categories) {
    const tbody = container.querySelector('#analytics-categories-tbody');
    if (!categories || categories.length === 0) {
      tbody.innerHTML = '<tr><td colspan="6" style="text-align: center; padding: 20px;">No category data available.</td></tr>';
      return;
    }

    tbody.innerHTML = categories.map(c => {
      const margin = c.retailValue > 0 ? (((c.retailValue - c.costValue) / c.retailValue) * 100) : 0;
      return `
        <tr>
          <td><strong>${c.categoryName}</strong></td>
          <td>${c.medicineCount} medicines</td>
          <td><strong>${c.totalUnits}</strong> units</td>
          <td>$${c.costValue.toFixed(2)}</td>
          <td style="font-weight: 700; color: var(--brand-primary);">$${c.retailValue.toFixed(2)}</td>
          <td>
            <span class="badge ${margin > 40 ? 'badge-safe' : 'badge-warning'}">
              +${margin.toFixed(1)}% Margin
            </span>
          </td>
        </tr>
      `;
    }).join('');
  }
}

window.analyticsView = new AnalyticsView();
