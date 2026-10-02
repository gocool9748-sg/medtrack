/**
 * Purchase Orders & Replenishment Re-Ordering View
 */
class OrderView {
  constructor() {
    this.suppliers = [];
    this.medicines = [];
    this.statusFilter = 'ALL';
  }

  async render(container) {
    container.innerHTML = `
      <div class="view-header">
        <div class="view-title-group">
          <h1>Purchase Orders & Replenishment</h1>
          <p>Procurement management, automated re-order suggestions & stock ingestion on arrival</p>
        </div>
        <div class="view-actions">
          <button class="btn btn-secondary btn-sm" id="btn-auto-reorder-suggest">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
            Auto-Generate Low Stock PO
          </button>
          <button class="btn btn-gradient btn-sm" id="btn-create-po">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" x2="12" y1="5" y2="19"/><line x1="5" x2="19" y1="12" y2="12"/></svg>
            Create Purchase Order
          </button>
        </div>
      </div>

      <!-- Status Filters -->
      <div class="glass-card" style="margin-bottom: 24px; padding: 18px 24px;">
        <div class="filter-pill-group" id="po-status-pills">
          <div class="filter-pill active" data-status="ALL">All Orders</div>
          <div class="filter-pill" data-status="SENT">🚚 In Transit / Sent</div>
          <div class="filter-pill" data-status="RECEIVED">✅ Received &amp; Ingested</div>
          <div class="filter-pill" data-status="DRAFT">📝 Drafts</div>
          <div class="filter-pill" data-status="CANCELLED">❌ Cancelled</div>
        </div>
      </div>

      <!-- Orders List Table -->
      <div class="glass-card">
        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>PO Number</th>
                <th>Supplier / Distributor</th>
                <th>Order Date</th>
                <th>Expected Date</th>
                <th>Items Count</th>
                <th>Total Value</th>
                <th>Status</th>
                <th>Created By</th>
                <th style="text-align: right;">Actions</th>
              </tr>
            </thead>
            <tbody id="orders-table-body">
              <tr><td colspan="9" style="text-align: center; padding: 24px;">Loading purchase orders...</td></tr>
            </tbody>
          </table>
        </div>
      </div>
    `;

    try {
      const [supps, meds] = await Promise.all([
        window.api.getSuppliers(),
        window.api.getMedicines()
      ]);
      this.suppliers = supps || [];
      this.medicines = meds || [];
      this.loadOrders(container);
    } catch (e) {
      window.toast.error('Error', e.message);
    }

    // Status filter
    const pills = container.querySelector('#po-status-pills');
    pills.querySelectorAll('.filter-pill').forEach(pill => {
      pill.addEventListener('click', () => {
        pills.querySelectorAll('.filter-pill').forEach(p => p.classList.remove('active'));
        pill.classList.add('active');
        this.statusFilter = pill.dataset.status;
        this.loadOrders(container);
      });
    });

    // Create PO
    container.querySelector('#btn-create-po').addEventListener('click', () => {
      this.openCreatePoModal(container);
    });

    // Auto Reorder Suggestion
    container.querySelector('#btn-auto-reorder-suggest').addEventListener('click', () => {
      this.generateAutoReorderPo(container);
    });
  }

  async loadOrders(container) {
    const tbody = container.querySelector('#orders-table-body');
    tbody.innerHTML = '<tr><td colspan="9" style="text-align: center; padding: 24px;">Fetching purchase orders...</td></tr>';

    try {
      const orders = await window.api.getOrders(this.statusFilter === 'ALL' ? '' : this.statusFilter);
      if (!orders || orders.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" style="text-align: center; padding: 30px; color: var(--text-muted);">No purchase orders found.</td></tr>';
        return;
      }

      tbody.innerHTML = orders.map(po => {
        let badgeClass = 'badge-neutral';
        if (po.status === 'RECEIVED') badgeClass = 'badge-safe';
        else if (po.status === 'SENT') badgeClass = 'badge-warning';
        else if (po.status === 'CANCELLED') badgeClass = 'badge-danger';

        return `
          <tr>
            <td><strong style="font-family: monospace;">${po.poNumber}</strong></td>
            <td><strong>${po.supplierName}</strong></td>
            <td>${po.orderDate}</td>
            <td>${po.expectedDate || 'Flexible'}</td>
            <td>${po.itemCount || 1} line items</td>
            <td style="font-weight: 700; color: var(--brand-primary);">$${po.totalAmount.toFixed(2)}</td>
            <td><span class="badge ${badgeClass}">${po.status}</span></td>
            <td style="font-size: 11.5px; color: var(--text-secondary);">${po.creatorName || 'Logistics Staff'}</td>
            <td style="text-align: right;">
              <div style="display: flex; gap: 6px; justify-content: flex-end;">
                <button class="btn btn-secondary btn-sm btn-view-po" data-id="${po.id}">
                  View Details
                </button>
                ${po.status === 'SENT' ? `
                  <button class="btn btn-success btn-sm btn-receive-po" data-id="${po.id}" data-num="${po.poNumber}">
                    Receive Stock
                  </button>
                ` : ''}
              </div>
            </td>
          </tr>
        `;
      }).join('');

      tbody.querySelectorAll('.btn-view-po').forEach(btn => {
        btn.addEventListener('click', () => {
          this.openViewPoModal(btn.dataset.id);
        });
      });

      tbody.querySelectorAll('.btn-receive-po').forEach(btn => {
        btn.addEventListener('click', async () => {
          if (confirm(`Confirm receipt of PO #${btn.dataset.num}? This will automatically generate new batches in your stock inventory.`)) {
            try {
              await window.api.updateOrderStatus(btn.dataset.id, 'RECEIVED');
              window.toast.success('Stock Ingested', `PO #${btn.dataset.num} received and added to inventory batches!`);
              this.loadOrders(container);
            } catch (e) {
              window.toast.error('Error', e.message);
            }
          }
        });
      });

    } catch (e) {
      tbody.innerHTML = `<tr><td colspan="9" style="text-align: center; color: var(--status-danger); padding: 20px;">${e.message}</td></tr>`;
    }
  }

  async openViewPoModal(id) {
    try {
      const po = await window.api.getOrderById(id);
      const items = po.items || [];

      const bodyHtml = `
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 20px; font-size: 13px;">
          <div>
            <strong>PO Number:</strong> ${po.poNumber}<br>
            <strong>Supplier:</strong> ${po.supplierName}<br>
            <strong>Order Date:</strong> ${po.orderDate}
          </div>
          <div style="text-align: right;">
            <strong>Status:</strong> <span class="badge badge-neutral">${po.status}</span><br>
            <strong>Expected:</strong> ${po.expectedDate || 'Standard Delivery'}<br>
            <strong>Created By:</strong> ${po.creatorName || 'Admin'}
          </div>
        </div>

        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>Medicine</th>
                <th>Quantity</th>
                <th>Unit Cost</th>
                <th style="text-align: right;">Subtotal</th>
              </tr>
            </thead>
            <tbody>
              ${items.map(i => `
                <tr>
                  <td>
                    <strong>${i.medicineName}</strong><br>
                    <small style="color: var(--text-muted);">${i.genericName}</small>
                  </td>
                  <td>${i.quantity} units</td>
                  <td>$${i.unitCost.toFixed(2)}</td>
                  <td style="text-align: right; font-weight: 700;">$${i.subtotal.toFixed(2)}</td>
                </tr>
              `).join('')}
            </tbody>
          </table>
        </div>

        <div style="margin-top: 16px; text-align: right; font-size: 16px; font-weight: 800; color: var(--brand-primary);">
          Total Order Value: $${po.totalAmount.toFixed(2)}
        </div>

        ${po.notes ? `<div style="margin-top: 12px; font-size: 12px; color: var(--text-secondary);"><strong>Notes:</strong> ${po.notes}</div>` : ''}
      `;

      const footerHtml = `
        <button class="btn btn-secondary" onclick="window.modal.close()">Close</button>
        ${po.status === 'SENT' ? `
          <button class="btn btn-success" id="btn-modal-rec-po">Receive Stock Now</button>
        ` : ''}
      `;

      const modalEl = window.modal.open(`Purchase Order: ${po.poNumber}`, bodyHtml, footerHtml, '650px');
      const recBtn = modalEl.querySelector('#btn-modal-rec-po');
      if (recBtn) {
        recBtn.addEventListener('click', async () => {
          try {
            await window.api.updateOrderStatus(po.id, 'RECEIVED');
            window.toast.success('Stock Ingested', `PO #${po.poNumber} received and added to inventory!`);
            window.modal.close();
            this.loadOrders(document.getElementById('content-viewport'));
          } catch (e) {
            window.toast.error('Error', e.message);
          }
        });
      }

    } catch (e) {
      window.toast.error('Error', e.message);
    }
  }

  openCreatePoModal(container, preloadedItems = null) {
    const suppOptions = this.suppliers.map(s => `<option value="${s.id}">${s.name}</option>`).join('');
    const medOptions = this.medicines.map(m => `<option value="${m.id}">${m.name} (${m.genericName})</option>`).join('');

    const today = new Date().toISOString().substring(0, 10);
    const expDate = new Date();
    expDate.setDate(expDate.getDate() + 7);
    const defaultExpDate = expDate.toISOString().substring(0, 10);

    const generatedPoNo = 'PO-' + Math.floor(1000 + Math.random() * 9000);

    const bodyHtml = `
      <form id="form-create-po">
        <div class="form-grid-2">
          <div class="form-group">
            <label class="form-label">PO Number *</label>
            <input type="text" class="form-control" name="poNumber" value="${generatedPoNo}" required>
          </div>
          <div class="form-group">
            <label class="form-label">Select Supplier *</label>
            <select class="form-control" name="supplierId" required>
              ${suppOptions}
            </select>
          </div>
        </div>

        <div class="form-grid-2">
          <div class="form-group">
            <label class="form-label">Order Date *</label>
            <input type="date" class="form-control" name="orderDate" value="${today}" required>
          </div>
          <div class="form-group">
            <label class="form-label">Expected Delivery Date *</label>
            <input type="date" class="form-control" name="expectedDate" value="${defaultExpDate}" required>
          </div>
        </div>

        <div style="font-size: 14px; font-weight: 700; margin: 12px 0 6px 0;">Order Line Items</div>
        <div id="po-line-items-container" style="display: flex; flex-direction: column; gap: 8px;">
          <!-- Items will be inserted here -->
        </div>

        <button type="button" class="btn btn-secondary btn-sm" id="btn-add-po-row" style="margin-top: 10px;">
          + Add Drug Item
        </button>

        <div class="form-group" style="margin-top: 16px;">
          <label class="form-label">Notes & Procurement Instructions</label>
          <input type="text" class="form-control" name="notes" placeholder="e.g. Please ensure cold chain compliance during transport.">
        </div>
      </form>
    `;

    const footerHtml = `
      <button class="btn btn-secondary" onclick="window.modal.close()">Cancel</button>
      <button class="btn btn-primary" id="btn-submit-create-po">Dispatch Purchase Order</button>
    `;

    const modalEl = window.modal.open('Create Supplier Purchase Order', bodyHtml, footerHtml, '720px');
    const itemsWrap = modalEl.querySelector('#po-line-items-container');

    const addRow = (medId = null, qty = 100, cost = 5.00) => {
      const row = document.createElement('div');
      row.className = 'po-item-row';
      row.style.cssText = 'display: grid; grid-template-columns: 2fr 1fr 1fr 36px; gap: 8px; align-items: center;';
      row.innerHTML = `
        <select class="form-control po-med-select" required>
          ${this.medicines.map(m => `<option value="${m.id}" ${m.id === medId ? 'selected' : ''}>${m.name} (${m.genericName})</option>`).join('')}
        </select>
        <input type="number" class="form-control po-qty-input" placeholder="Qty" value="${qty}" min="1" required>
        <input type="number" step="0.01" class="form-control po-cost-input" placeholder="Cost ($)" value="${cost.toFixed(2)}" min="0.01" required>
        <button type="button" class="btn btn-secondary btn-icon-only btn-sm btn-del-row" style="color: var(--status-danger);">✕</button>
      `;
      row.querySelector('.btn-del-row').addEventListener('click', () => row.remove());
      itemsWrap.appendChild(row);
    };

    if (preloadedItems && preloadedItems.length > 0) {
      preloadedItems.forEach(item => addRow(item.medId, item.qty, item.cost));
    } else {
      addRow(this.medicines[0]?.id, 100, 5.00);
      addRow(this.medicines[1]?.id, 50, 8.50);
    }

    modalEl.querySelector('#btn-add-po-row').addEventListener('click', () => addRow());

    modalEl.querySelector('#btn-submit-create-po').addEventListener('click', async () => {
      const form = modalEl.querySelector('#form-create-po');
      if (!form.checkValidity()) {
        form.reportValidity();
        return;
      }

      const fd = new FormData(form);
      const rows = itemsWrap.querySelectorAll('.po-item-row');
      if (rows.length === 0) {
        window.toast.warning('Empty Items', 'Please add at least one line item to the order.');
        return;
      }

      const items = [];
      let total = 0;
      rows.forEach(r => {
        const medId = parseInt(r.querySelector('.po-med-select').value);
        const qty = parseInt(r.querySelector('.po-qty-input').value);
        const cost = parseFloat(r.querySelector('.po-cost-input').value);
        const subtotal = qty * cost;
        total += subtotal;
        items.push({
          medicineId: medId,
          quantity: qty,
          unitCost: cost,
          subtotal
        });
      });

      const poPayload = {
        poNumber: fd.get('poNumber'),
        supplierId: parseInt(fd.get('supplierId')),
        orderDate: fd.get('orderDate'),
        expectedDate: fd.get('expectedDate'),
        totalAmount: total,
        status: 'SENT',
        notes: fd.get('notes'),
        items
      };

      try {
        await window.api.createOrder(poPayload);
        window.toast.success('PO Created', `Purchase order #${poPayload.poNumber} sent to supplier.`);
        window.modal.close();
        this.loadOrders(container);
      } catch (err) {
        window.toast.error('PO Error', err.message);
      }
    });
  }

  async generateAutoReorderPo(container) {
    try {
      const lowStockMeds = await window.api.getMedicines({ stockStatus: 'LOW_STOCK' });
      if (!lowStockMeds || lowStockMeds.length === 0) {
        window.toast.info('Stock Levels Healthy', 'No medicines are currently below their safety reorder threshold.');
        return;
      }

      const suggestedItems = lowStockMeds.map(m => ({
        medId: m.id,
        qty: Math.max(50, m.reorderLevel * 2),
        cost: m.minPrice > 0 ? (m.minPrice * 0.55) : 5.00
      }));

      window.toast.success('Reorder Generated', `Auto-populated ${suggestedItems.length} low-stock medicines for replenishment.`);
      this.openCreatePoModal(container, suggestedItems);

    } catch (e) {
      window.toast.error('Reorder Error', e.message);
    }
  }
}

window.orderView = new OrderView();
