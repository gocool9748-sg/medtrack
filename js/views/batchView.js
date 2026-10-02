/**
 * Batch & Stock Registry View (FEFO Priority Driven)
 */
class BatchView {
  constructor() {
    this.statusFilter = 'ALL';
    this.searchQuery = '';
    this.medicines = [];
    this.suppliers = [];
  }

  async render(container) {
    container.innerHTML = `
      <div class="view-header">
        <div class="view-title-group">
          <h1>Stock Batches & FEFO Registry</h1>
          <p>Strict First-Expiry-First-Out (FEFO) stock priority tracking, storage locations & lifecycle control</p>
        </div>
        <div class="view-actions">
          <a href="/api/export/csv/batches" target="_blank" class="btn btn-secondary btn-sm">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" x2="12" y1="15" y2="3"/></svg>
            Export Batches (CSV)
          </a>
          <button class="btn btn-gradient btn-sm" id="btn-add-batch">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" x2="12" y1="5" y2="19"/><line x1="5" x2="19" y1="12" y2="12"/></svg>
            Receive Stock Batch
          </button>
        </div>
      </div>

      <!-- Filters -->
      <div class="glass-card" style="margin-bottom: 24px; padding: 18px 24px;">
        <div style="display: flex; gap: 16px; margin-bottom: 14px; flex-wrap: wrap; align-items: center;">
          <div style="flex: 1; min-width: 260px;">
            <input type="text" id="batch-search-input" class="form-control" placeholder="Search batch number, drug name, or shelf rack location..." value="${this.searchQuery}">
          </div>
        </div>

        <div class="filter-pill-group" id="batch-status-pills">
          <div class="filter-pill active" data-status="ALL">All Batches</div>
          <div class="filter-pill" data-status="CRITICAL">🟠 Critical (&lt;30 Days)</div>
          <div class="filter-pill" data-status="WARNING">🟡 Warning (30-90 Days)</div>
          <div class="filter-pill" data-status="SAFE">🟢 Safe (&gt;90 Days)</div>
          <div class="filter-pill" data-status="EXPIRED">🔴 Expired</div>
          <div class="filter-pill" data-status="QUARANTINED">🟣 Quarantined</div>
        </div>
      </div>

      <!-- Batches Table -->
      <div class="glass-card">
        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>FEFO Rank</th>
                <th>Batch #</th>
                <th>Medicine & Generic</th>
                <th>Units Available</th>
                <th>Mfg Date</th>
                <th>Expiry Date</th>
                <th>Days Remaining</th>
                <th>Unit Cost</th>
                <th>Retail Price</th>
                <th>Shelf Rack</th>
                <th>Supplier</th>
                <th style="text-align: right;">Actions</th>
              </tr>
            </thead>
            <tbody id="batches-table-body">
              <tr><td colspan="12" style="text-align: center; padding: 24px;">Loading stock batches...</td></tr>
            </tbody>
          </table>
        </div>
      </div>
    `;

    // Fetch dependencies
    try {
      const [meds, supps] = await Promise.all([
        window.api.getMedicines(),
        window.api.getSuppliers()
      ]);
      this.medicines = meds || [];
      this.suppliers = supps || [];
      this.loadBatches(container);
    } catch (e) {
      window.toast.error('Error', e.message);
    }

    // Filter pills
    const pillGroup = container.querySelector('#batch-status-pills');
    pillGroup.querySelectorAll('.filter-pill').forEach(pill => {
      pill.addEventListener('click', () => {
        pillGroup.querySelectorAll('.filter-pill').forEach(p => p.classList.remove('active'));
        pill.classList.add('active');
        this.statusFilter = pill.dataset.status;
        this.loadBatches(container);
      });
    });

    // Search input
    let timeout = null;
    const searchInput = container.querySelector('#batch-search-input');
    searchInput.addEventListener('input', (e) => {
      clearTimeout(timeout);
      timeout = setTimeout(() => {
        this.searchQuery = e.target.value.trim();
        this.loadBatches(container);
      }, 250);
    });

    // Add Batch button
    container.querySelector('#btn-add-batch').addEventListener('click', () => {
      this.openAddBatchModal(container);
    });
  }

  async loadBatches(container) {
    const tbody = container.querySelector('#batches-table-body');
    tbody.innerHTML = '<tr><td colspan="12" style="text-align: center; padding: 24px;">Sorting FEFO inventory...</td></tr>';

    try {
      const params = {};
      if (this.searchQuery) params.search = this.searchQuery;
      if (this.statusFilter && this.statusFilter !== 'ALL') params.status = this.statusFilter;

      const batches = await window.api.getBatches(params);

      if (!batches || batches.length === 0) {
        tbody.innerHTML = '<tr><td colspan="12" style="text-align: center; padding: 30px; color: var(--text-muted);">No batches found matching current filters.</td></tr>';
        return;
      }

      tbody.innerHTML = batches.map((b, index) => {
        const days = b.daysToExpiry;
        const isExp = days < 0;
        let badgeClass = 'badge-safe';
        let badgeLabel = `${days} Days`;
        let meterFillClass = 'safe';
        let meterPercent = Math.min(100, Math.max(10, Math.round((days / 365) * 100)));

        if ('QUARANTINED'.equalsIgnoreCase(b.status)) {
          badgeClass = 'badge-purple';
          badgeLabel = 'QUARANTINED';
          meterFillClass = 'danger';
          meterPercent = 0;
        } else if (isExp) {
          badgeClass = 'badge-danger';
          badgeLabel = `EXPIRED (${Math.abs(days)}d)`;
          meterFillClass = 'danger';
          meterPercent = 100;
        } else if (days <= 30) {
          badgeClass = 'badge-critical';
          badgeLabel = `CRITICAL (${days}d)`;
          meterFillClass = 'critical';
          meterPercent = Math.round((days / 30) * 100);
        } else if (days <= 90) {
          badgeClass = 'badge-warning';
          badgeLabel = `WARNING (${days}d)`;
          meterFillClass = 'warning';
          meterPercent = Math.round((days / 90) * 100);
        }

        const effectivePriceDisplay = b.discountPercent > 0 ? `
          <div>
            <span style="text-decoration: line-through; color: var(--text-muted); font-size: 11px;">$${b.unitPrice.toFixed(2)}</span>
            <strong style="color: var(--status-safe); font-size: 13px;">$${b.effectivePrice.toFixed(2)}</strong>
            <span class="badge badge-warning" style="font-size: 9px; padding: 1px 4px;">${b.discountPercent}% OFF</span>
          </div>
        ` : `<strong>$${b.unitPrice.toFixed(2)}</strong>`;

        return `
          <tr class="${b.expiryStatus === 'CRITICAL' ? 'row-critical-alert' : ''}">
            <td><strong style="color: var(--brand-primary);">#${index + 1}</strong></td>
            <td>
              <div style="font-weight: 700; font-family: monospace;">${b.batchNumber}</div>
            </td>
            <td>
              <div style="font-weight: 700;">${b.medicineName}</div>
              <div style="font-size: 11px; color: var(--text-muted);">${b.genericName} (${b.strength})</div>
            </td>
            <td>
              <strong style="font-size: 14px;">${b.quantity}</strong>
              <span style="font-size: 11px; color: var(--text-muted);">/ ${b.originalQuantity}</span>
            </td>
            <td style="font-size: 11.5px; color: var(--text-secondary);">${b.mfgDate}</td>
            <td style="font-size: 12px; font-weight: 600;">${b.expiryDate}</td>
            <td>
              <div class="expiry-meter-wrap">
                <span class="badge ${badgeClass}" style="width: fit-content;">${badgeLabel}</span>
                <div class="expiry-meter-bar">
                  <div class="expiry-meter-fill ${meterFillClass}" style="width: ${meterPercent}%;"></div>
                </div>
              </div>
            </td>
            <td style="font-size: 12px;">$${b.unitCost.toFixed(2)}</td>
            <td>${effectivePriceDisplay}</td>
            <td><span class="badge badge-neutral">${b.shelfLocation}</span></td>
            <td style="font-size: 11.5px; color: var(--text-secondary);">${b.supplierName || 'Direct'}</td>
            <td style="text-align: right;">
              <div style="display: flex; gap: 6px; justify-content: flex-end;">
                <button class="btn btn-secondary btn-icon-only btn-sm btn-batch-qr" data-num="${b.batchNumber}" data-name="${b.medicineName}" title="Print Batch QR / Barcode">
                  🏷️
                </button>
                <button class="btn btn-secondary btn-icon-only btn-sm btn-batch-adjust" data-id="${b.id}" data-num="${b.batchNumber}" data-qty="${b.quantity}" title="Physical Stock Adjustment">
                  ⚖️
                </button>
                <button class="btn btn-secondary btn-icon-only btn-sm btn-batch-discount" data-id="${b.id}" data-num="${b.batchNumber}" data-disc="${b.discountPercent}" title="Apply Markdown Discount">
                  🏷️%
                </button>
                ${b.status !== 'QUARANTINED' ? `
                  <button class="btn btn-secondary btn-icon-only btn-sm btn-batch-quarantine" data-id="${b.id}" data-num="${b.batchNumber}" title="Quarantine Batch">
                    ⛔
                  </button>
                ` : ''}
              </div>
            </td>
          </tr>
        `;
      }).join('');

      // Bind Row Buttons
      tbody.querySelectorAll('.btn-batch-qr').forEach(btn => {
        btn.addEventListener('click', () => {
          this.openBatchBarcodeModal(btn.dataset.name, btn.dataset.num);
        });
      });

      tbody.querySelectorAll('.btn-batch-adjust').forEach(btn => {
        btn.addEventListener('click', () => {
          this.openAdjustStockModal(container, btn.dataset.id, btn.dataset.num, btn.dataset.qty);
        });
      });

      tbody.querySelectorAll('.btn-batch-discount').forEach(btn => {
        btn.addEventListener('click', () => {
          this.openDiscountModal(container, btn.dataset.id, btn.dataset.num, btn.dataset.disc);
        });
      });

      tbody.querySelectorAll('.btn-batch-quarantine').forEach(btn => {
        btn.addEventListener('click', async () => {
          if (confirm(`Quarantine batch ${btn.dataset.num}? This locks it from active sales dispensing.`)) {
            try {
              await window.api.quarantineBatch(btn.dataset.id, 'Pharmacist manual quarantine lock');
              window.toast.success('Quarantine Applied', `Batch ${btn.dataset.num} quarantined.`);
              this.loadBatches(container);
            } catch (e) {
              window.toast.error('Error', e.message);
            }
          }
        });
      });

    } catch (err) {
      tbody.innerHTML = `<tr><td colspan="12" style="text-align: center; color: var(--status-danger); padding: 20px;">${err.message}</td></tr>`;
    }
  }

  openBatchBarcodeModal(name, batchNumber) {
    const bodyHtml = `
      <div style="text-align: center; padding: 10px;">
        <div style="font-weight: 800; font-size: 18px; margin-bottom: 4px;">${name}</div>
        <div style="font-size: 13px; color: var(--brand-primary); font-family: monospace; margin-bottom: 16px;">BATCH: ${batchNumber}</div>
        
        <div style="background: #ffffff; padding: 20px; border-radius: 8px; display: inline-block; box-shadow: var(--shadow-md);">
          <canvas id="batch-barcode-canvas" style="display: block; margin: 0 auto;"></canvas>
          <div style="display: flex; justify-content: center; margin-top: 14px;">
            <canvas id="batch-qr-canvas"></canvas>
          </div>
        </div>

        <div style="margin-top: 20px; font-size: 12px; color: var(--text-secondary);">
          FEFO stock bin tag with encoded batch ID and verification payload.
        </div>
      </div>
    `;

    const footerHtml = `
      <button class="btn btn-secondary" onclick="window.modal.close()">Close</button>
      <button class="btn btn-primary" onclick="window.print()">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 6 2 18 2 18 9"/><path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"/><rect width="12" height="8" x="6" y="14"/></svg>
        Print Bin Tag
      </button>
    `;

    const modalEl = window.modal.open(`Batch Tag: ${batchNumber}`, bodyHtml, footerHtml);
    setTimeout(() => {
      const bcCanvas = modalEl.querySelector('#batch-barcode-canvas');
      const qrCanvas = modalEl.querySelector('#batch-qr-canvas');
      if (window.barcodeGen) {
        window.barcodeGen.drawBarcode(bcCanvas, batchNumber, { width: 320, height: 85 });
        window.barcodeGen.drawQRCode(qrCanvas, `BATCH:${batchNumber}:${name}`, 110);
      }
    }, 50);
  }

  openAddBatchModal(container, preselectedMedId = null) {
    const medOptions = this.medicines.map(m => `<option value="${m.id}" ${m.id === preselectedMedId ? 'selected' : ''}>${m.name} (${m.genericName} - ${m.dosageForm})</option>`).join('');
    const suppOptions = this.suppliers.map(s => `<option value="${s.id}">${s.name}</option>`).join('');

    const today = new Date().toISOString().substring(0, 10);
    const futureDate = new Date();
    futureDate.setFullYear(futureDate.getFullYear() + 1);
    const defaultExp = futureDate.toISOString().substring(0, 10);

    const generatedBatchNo = 'BAT-' + Math.floor(1000 + Math.random() * 9000);

    const bodyHtml = `
      <form id="form-add-batch">
        <div class="form-group">
          <label class="form-label">Medicine *</label>
          <select class="form-control" name="medicineId" required>
            ${medOptions}
          </select>
        </div>

        <div class="form-grid-2">
          <div class="form-group">
            <label class="form-label">Batch Number *</label>
            <input type="text" class="form-control" name="batchNumber" value="${generatedBatchNo}" required>
          </div>
          <div class="form-group">
            <label class="form-label">Quantity Received (Units) *</label>
            <input type="number" class="form-control" name="quantity" value="100" min="1" required>
          </div>
        </div>

        <div class="form-grid-2">
          <div class="form-group">
            <label class="form-label">Manufacturing Date *</label>
            <input type="date" class="form-control" name="mfgDate" value="${today}" required>
          </div>
          <div class="form-group">
            <label class="form-label">Expiry Date *</label>
            <input type="date" class="form-control" name="expiryDate" value="${defaultExp}" required>
          </div>
        </div>

        <div class="form-grid-2">
          <div class="form-group">
            <label class="form-label">Unit Cost Price ($) *</label>
            <input type="number" step="0.01" class="form-control" name="unitCost" value="5.00" required>
          </div>
          <div class="form-group">
            <label class="form-label">Unit Selling / Retail Price ($) *</label>
            <input type="number" step="0.01" class="form-control" name="unitPrice" value="9.50" required>
          </div>
        </div>

        <div class="form-grid-2">
          <div class="form-group">
            <label class="form-label">Storage Location / Shelf Rack *</label>
            <input type="text" class="form-control" name="shelfLocation" value="Shelf A-1" required>
          </div>
          <div class="form-group">
            <label class="form-label">Supplier / Distributor</label>
            <select class="form-control" name="supplierId">
              <option value="">Direct / Self-Manufactured</option>
              ${suppOptions}
            </select>
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">Batch Notes & Storage Precautions</label>
          <input type="text" class="form-control" name="notes" placeholder="e.g. Received in temperature-controlled cooler. Verified seals intact.">
        </div>
      </form>
    `;

    const footerHtml = `
      <button class="btn btn-secondary" onclick="window.modal.close()">Cancel</button>
      <button class="btn btn-primary" id="btn-submit-add-batch">Receive Stock</button>
    `;

    const modalEl = window.modal.open('Receive New Stock Batch', bodyHtml, footerHtml, '680px');
    modalEl.querySelector('#btn-submit-add-batch').addEventListener('click', async () => {
      const form = modalEl.querySelector('#form-add-batch');
      if (!form.checkValidity()) {
        form.reportValidity();
        return;
      }

      const fd = new FormData(form);
      const data = {
        medicineId: parseInt(fd.get('medicineId')),
        batchNumber: fd.get('batchNumber'),
        quantity: parseInt(fd.get('quantity')),
        originalQuantity: parseInt(fd.get('quantity')),
        mfgDate: fd.get('mfgDate'),
        expiryDate: fd.get('expiryDate'),
        unitCost: parseFloat(fd.get('unitCost')),
        unitPrice: parseFloat(fd.get('unitPrice')),
        discountPercent: 0.0,
        shelfLocation: fd.get('shelfLocation'),
        supplierId: fd.get('supplierId') ? parseInt(fd.get('supplierId')) : null,
        notes: fd.get('notes'),
        status: 'ACTIVE'
      };

      try {
        await window.api.createBatch(data);
        window.toast.success('Stock Received', `Registered batch ${data.batchNumber} with ${data.quantity} units.`);
        window.modal.close();
        this.loadBatches(container);
      } catch (err) {
        window.toast.error('Save Error', err.message);
      }
    });
  }

  openAdjustStockModal(container, id, batchNum, currentQty) {
    const bodyHtml = `
      <form id="form-adjust-stock">
        <div style="margin-bottom: 14px; font-size: 13px; color: var(--text-secondary);">
          Adjusting physical stock for Batch <strong style="color: var(--text-primary); font-family: monospace;">${batchNum}</strong>.
        </div>
        <div class="form-group">
          <label class="form-label">Verified Physical Quantity On Hand *</label>
          <input type="number" class="form-control" name="quantity" value="${currentQty}" min="0" required>
        </div>
        <div class="form-group">
          <label class="form-label">Reason for Stock Adjustment *</label>
          <select class="form-control" name="reason" required>
            <option value="Physical count audit discrepancy">Physical count audit discrepancy</option>
            <option value="Damaged ampoules / broken packaging">Damaged ampoules / broken packaging</option>
            <option value="Found additional warehouse stock">Found additional warehouse stock</option>
            <option value="Sample distribution to physicians">Sample distribution to physicians</option>
          </select>
        </div>
      </form>
    `;

    const footerHtml = `
      <button class="btn btn-secondary" onclick="window.modal.close()">Cancel</button>
      <button class="btn btn-primary" id="btn-submit-adjust">Confirm Adjustment</button>
    `;

    const modalEl = window.modal.open(`Stock Count Adjustment`, bodyHtml, footerHtml);
    modalEl.querySelector('#btn-submit-adjust').addEventListener('click', async () => {
      const form = modalEl.querySelector('#form-adjust-stock');
      const fd = new FormData(form);
      const qty = parseInt(fd.get('quantity'));
      const reason = fd.get('reason');

      try {
        await window.api.adjustBatchStock(id, qty, reason);
        window.toast.success('Stock Adjusted', `Batch ${batchNum} count set to ${qty}.`);
        window.modal.close();
        this.loadBatches(container);
      } catch (e) {
        window.toast.error('Adjustment Failed', e.message);
      }
    });
  }

  openDiscountModal(container, id, batchNum, currentDisc) {
    const bodyHtml = `
      <form id="form-discount-batch">
        <div style="margin-bottom: 14px; font-size: 13px; color: var(--text-secondary);">
          Apply early markdown discount for Batch <strong style="color: var(--text-primary); font-family: monospace;">${batchNum}</strong> to accelerate FEFO dispensing.
        </div>
        <div class="form-group">
          <label class="form-label">Promotional Discount Percentage (%) *</label>
          <input type="number" step="1" class="form-control" name="discountPercent" value="${currentDisc || 20}" min="0" max="90" required>
        </div>
        <div style="display: flex; gap: 8px; margin-top: 8px;">
          <button type="button" class="btn btn-secondary btn-sm" onclick="this.form.discountPercent.value = 15">15% Early</button>
          <button type="button" class="btn btn-secondary btn-sm" onclick="this.form.discountPercent.value = 25">25% Promo</button>
          <button type="button" class="btn btn-secondary btn-sm" onclick="this.form.discountPercent.value = 40">40% Clearance</button>
          <button type="button" class="btn btn-secondary btn-sm" onclick="this.form.discountPercent.value = 0">Reset (0%)</button>
        </div>
      </form>
    `;

    const footerHtml = `
      <button class="btn btn-secondary" onclick="window.modal.close()">Cancel</button>
      <button class="btn btn-primary" id="btn-submit-discount">Apply Discount</button>
    `;

    const modalEl = window.modal.open(`Promotional Markdown Discount`, bodyHtml, footerHtml);
    modalEl.querySelector('#btn-submit-discount').addEventListener('click', async () => {
      const form = modalEl.querySelector('#form-discount-batch');
      const fd = new FormData(form);
      const discount = parseFloat(fd.get('discountPercent'));

      try {
        await window.api.discountBatch(id, discount);
        window.toast.success('Discount Applied', `Batch ${batchNum} discount set to ${discount}%.`);
        window.modal.close();
        this.loadBatches(container);
      } catch (e) {
        window.toast.error('Error', e.message);
      }
    });
  }
}

window.batchView = new BatchView();
