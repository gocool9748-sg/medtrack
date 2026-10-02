/**
 * Real-Time Security & System Audit Trail View
 */
class AuditView {
  constructor() {
    this.searchQuery = '';
    this.actionFilter = 'ALL';
  }

  async render(container) {
    container.innerHTML = `
      <div class="view-header">
        <div class="view-title-group">
          <h1>Security & System Audit Trail</h1>
          <p>Immutable event stream recording logins, stock mutations, POS sales, and clinical disposal records</p>
        </div>
      </div>

      <!-- Filters -->
      <div class="glass-card" style="margin-bottom: 24px; padding: 18px 24px;">
        <div style="display: flex; gap: 16px; flex-wrap: wrap;">
          <div style="flex: 1; min-width: 260px;">
            <input type="text" id="audit-search-input" class="form-control" placeholder="Search actor username, action type, IP address, or details..." value="${this.searchQuery}">
          </div>
          <select id="audit-action-filter" class="form-control" style="width: 200px;">
            <option value="ALL">All Event Types</option>
            <option value="LOGIN_SUCCESS">User Logins</option>
            <option value="DISPENSE_SALE">POS Sales / Dispensing</option>
            <option value="CREATE_MEDICINE">Drug Catalog Creations</option>
            <option value="CREATE_BATCH">Stock Batch Additions</option>
            <option value="ADJUST_STOCK">Stock Adjustments</option>
            <option value="QUARANTINE_BATCH">Quarantine Locks</option>
            <option value="DISCOUNT_BATCH">Markdown Discounts</option>
            <option value="DISPOSAL_AUTHORIZED">Disposal Events</option>
            <option value="CREATE_PO">Purchase Orders</option>
          </select>
        </div>
      </div>

      <!-- Audit Logs Table -->
      <div class="glass-card">
        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>Event ID</th>
                <th>Timestamp</th>
                <th>Actor User</th>
                <th>Role</th>
                <th>Action Type</th>
                <th>Entity Target</th>
                <th>Details &amp; Audit Payload</th>
                <th>IP Address</th>
              </tr>
            </thead>
            <tbody id="audit-logs-tbody">
              <tr><td colspan="8" style="text-align: center; padding: 24px;">Reading immutable audit records...</td></tr>
            </tbody>
          </table>
        </div>
      </div>
    `;

    this.loadAuditLogs(container);

    let timeout = null;
    container.querySelector('#audit-search-input').addEventListener('input', (e) => {
      clearTimeout(timeout);
      timeout = setTimeout(() => {
        this.searchQuery = e.target.value.trim();
        this.loadAuditLogs(container);
      }, 250);
    });

    container.querySelector('#audit-action-filter').addEventListener('change', (e) => {
      this.actionFilter = e.target.value;
      this.loadAuditLogs(container);
    });
  }

  async loadAuditLogs(container) {
    const tbody = container.querySelector('#audit-logs-tbody');
    try {
      const params = {};
      if (this.searchQuery) params.search = this.searchQuery;
      if (this.actionFilter && this.actionFilter !== 'ALL') params.action = this.actionFilter;

      const logs = await window.api.getAuditLogs(params);
      if (!logs || logs.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; padding: 30px; color: var(--text-muted);">No audit logs matching current query.</td></tr>';
        return;
      }

      tbody.innerHTML = logs.map(l => {
        let badgeClass = 'badge-neutral';
        if (l.action.includes('LOGIN')) badgeClass = 'badge-safe';
        else if (l.action.includes('SALE')) badgeClass = 'badge-safe';
        else if (l.action.includes('QUARANTINE') || l.action.includes('DISPOSAL')) badgeClass = 'badge-danger';
        else if (l.action.includes('DISCOUNT')) badgeClass = 'badge-warning';

        return `
          <tr>
            <td><strong style="color: var(--text-muted);">#${l.id}</strong></td>
            <td style="font-size: 11.5px; color: var(--text-secondary); white-space: nowrap;">${l.createdAt}</td>
            <td><strong>${l.username}</strong></td>
            <td><span class="badge badge-neutral">${l.role || 'USER'}</span></td>
            <td><span class="badge ${badgeClass}">${l.action}</span></td>
            <td><code style="color: var(--brand-primary);">${l.entityType} ${l.entityId ? `[${l.entityId}]` : ''}</code></td>
            <td style="font-size: 12.5px; color: var(--text-primary); max-width: 320px;">${l.details}</td>
            <td><span style="font-family: monospace; font-size: 11px; color: var(--text-muted);">${l.ipAddress || '127.0.0.1'}</span></td>
          </tr>
        `;
      }).join('');

    } catch (e) {
      tbody.innerHTML = `<tr><td colspan="8" style="text-align: center; color: var(--status-danger); padding: 20px;">${e.message}</td></tr>`;
    }
  }
}

window.auditView = new AuditView();
