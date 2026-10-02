/**
 * Medicine Inventory Catalog View
 */
class InventoryView {
  constructor() {
    this.categories = [];
    this.currentCategory = null;
    this.currentStockStatus = null;
    this.searchQuery = '';
  }

  async render(container) {
    container.innerHTML = `
      <div class="view-header">
        <div class="view-title-group">
          <h1>Pharmaceutical Medicine Catalog</h1>
          <p>Comprehensive drug repository, dosage formats, storage conditions & stock tracking</p>
        </div>
        <div class="view-actions">
          <a href="/api/export/csv/medicines" target="_blank" class="btn btn-secondary btn-sm">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" x2="12" y1="15" y2="3"/></svg>
            Export Catalog (CSV)
          </a>
          <button class="btn btn-gradient btn-sm" id="btn-add-medicine">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" x2="12" y1="5" y2="19"/><line x1="5" x2="19" y1="12" y2="12"/></svg>
            Add Medicine
          </button>
        </div>
      </div>

      <!-- Filter Controls & Category Tabs -->
      <div class="glass-card" style="margin-bottom: 24px; padding: 18px 24px;">
        <div style="display: flex; gap: 16px; margin-bottom: 14px; flex-wrap: wrap; align-items: center;">
          <div style="flex: 1; min-width: 260px; position: relative;">
            <input type="text" id="inv-search-input" class="form-control" placeholder="Search by brand name, generic name, barcode, or dosage..." value="${this.searchQuery}">
          </div>
          <div style="display: flex; gap: 10px;">
            <select id="inv-stock-filter" class="form-control" style="width: 170px;">
              <option value="">All Stock Levels</option>
              <option value="IN_STOCK">In Stock (&gt; Reorder)</option>
              <option value="LOW_STOCK">Low Stock (≤ Reorder)</option>
              <option value="OUT_OF_STOCK">Out of Stock (0 Units)</option>
            </select>
          </div>
        </div>

        <!-- Category Filter Pills -->
        <div class="filter-pill-group" id="inv-category-pills">
          <div class="filter-pill active" data-cat="">All Categories</div>
          <!-- Populated dynamically -->
        </div>
      </div>

      <!-- Medicines Data Table -->
      <div class="glass-card">
        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>Drug Name & Generic Formula</th>
                <th>Category</th>
                <th>Dosage & Strength</th>
                <th>Packaging Unit</th>
                <th>Storage Condition</th>
                <th>Total Stock</th>
                <th>Active Batches</th>
                <th>Price Range</th>
                <th>Rx Required</th>
                <th style="text-align: right;">Actions</th>
              </tr>
            </thead>
            <tbody id="medicines-table-body">
              <tr><td colspan="10" style="text-align: center; padding: 24px;">Loading medicine catalog...</td></tr>
            </tbody>
          </table>
        </div>
      </div>
    `;

    // Fetch Categories and Medicines
    try {
      this.categories = await window.api.getCategories();
      this.renderCategoryPills(container);
      this.loadMedicines(container);
    } catch (e) {
      window.toast.error('Catalog Error', e.message);
    }

    // Search Input binding with debounce
    let timeout = null;
    const searchInput = container.querySelector('#inv-search-input');
    searchInput.addEventListener('input', (e) => {
      clearTimeout(timeout);
      timeout = setTimeout(() => {
        this.searchQuery = e.target.value.trim();
        this.loadMedicines(container);
      }, 250);
    });

    // Stock level dropdown
    const stockSelect = container.querySelector('#inv-stock-filter');
    stockSelect.addEventListener('change', (e) => {
      this.currentStockStatus = e.target.value;
      this.loadMedicines(container);
    });

    // Add Medicine button
    container.querySelector('#btn-add-medicine').addEventListener('click', () => {
      this.openAddMedicineModal(container);
    });
  }

  renderCategoryPills(container) {
    const pillGroup = container.querySelector('#inv-category-pills');
    const pillsHtml = `
      <div class="filter-pill ${!this.currentCategory ? 'active' : ''}" data-cat="">All Categories</div>
      ${this.categories.map(c => `
        <div class="filter-pill ${this.currentCategory == c.id ? 'active' : ''}" data-cat="${c.id}">
          ${c.name} (${c.medicineCount || 0})
        </div>
      `).join('')}
    `;
    pillGroup.innerHTML = pillsHtml;

    pillGroup.querySelectorAll('.filter-pill').forEach(pill => {
      pill.addEventListener('click', () => {
        pillGroup.querySelectorAll('.filter-pill').forEach(p => p.classList.remove('active'));
        pill.classList.add('active');
        const catId = pill.dataset.cat;
        this.currentCategory = catId ? parseInt(catId) : null;
        this.loadMedicines(container);
      });
    });
  }

  async loadMedicines(container) {
    const tbody = container.querySelector('#medicines-table-body');
    tbody.innerHTML = '<tr><td colspan="10" style="text-align: center; padding: 24px;">Filtering catalog...</td></tr>';

    try {
      const params = {};
      if (this.searchQuery) params.search = this.searchQuery;
      if (this.currentCategory) params.categoryId = this.currentCategory;
      if (this.currentStockStatus) params.stockStatus = this.currentStockStatus;

      const medicines = await window.api.getMedicines(params);

      if (!medicines || medicines.length === 0) {
        tbody.innerHTML = '<tr><td colspan="10" style="text-align: center; padding: 30px; color: var(--text-muted);">No medicines found matching the current filters.</td></tr>';
        return;
      }

      tbody.innerHTML = medicines.map(m => {
        const isLowStock = m.totalStock <= m.reorderLevel && m.totalStock > 0;
        const isOutOfStock = m.totalStock === 0;
        let stockBadge = `<span class="badge badge-safe">In Stock (${m.totalStock})</span>`;
        if (isOutOfStock) {
          stockBadge = `<span class="badge badge-danger">Out of Stock (0)</span>`;
        } else if (isLowStock) {
          stockBadge = `<span class="badge badge-warning">Low Stock (${m.totalStock})</span>`;
        }

        const priceDisplay = m.minPrice > 0 ? (m.minPrice === m.maxPrice ? `$${m.minPrice.toFixed(2)}` : `$${m.minPrice.toFixed(2)} - $${m.maxPrice.toFixed(2)}`) : 'N/A';

        return `
          <tr>
            <td>
              <div style="font-weight: 700; font-size: 14px; color: var(--text-primary);">${m.name}</div>
              <div style="font-size: 11.5px; color: var(--text-muted);">${m.genericName}</div>
              ${m.barcode ? `<div style="font-size: 10px; color: var(--brand-primary); font-family: monospace;">BARCODE: ${m.barcode}</div>` : ''}
            </td>
            <td><span class="badge badge-neutral">${m.categoryName || 'General'}</span></td>
            <td><strong>${m.dosageForm}</strong> (${m.strength})</td>
            <td>${m.unit}</td>
            <td style="font-size: 11.5px; color: var(--text-secondary);">${m.storageCondition || 'Room Temp'}</td>
            <td>${stockBadge}</td>
            <td><span class="badge badge-neutral">${m.activeBatches} batches</span></td>
            <td style="font-weight: 700; color: var(--brand-primary);">${priceDisplay}</td>
            <td>
              ${m.requiresPrescription ? '<span class="badge badge-critical">Rx Required</span>' : '<span class="badge badge-neutral">OTC</span>'}
            </td>
            <td style="text-align: right;">
              <div style="display: flex; gap: 6px; justify-content: flex-end;">
                <button class="btn btn-secondary btn-icon-only btn-sm btn-barcode-med" data-id="${m.id}" data-name="${m.name}" data-code="${m.barcode}" title="Print Drug Label & Barcode">
                  🏷️
                </button>
                <button class="btn btn-secondary btn-icon-only btn-sm btn-batches-med" data-id="${m.id}" title="View Active Stock Batches">
                  📦
                </button>
                <button class="btn btn-secondary btn-icon-only btn-sm btn-edit-med" data-id="${m.id}" title="Edit Medicine Metadata">
                  ✏️
                </button>
              </div>
            </td>
          </tr>
        `;
      }).join('');

      // Bind Row Action Buttons
      tbody.querySelectorAll('.btn-barcode-med').forEach(btn => {
        btn.addEventListener('click', () => {
          this.openBarcodeModal(btn.dataset.name, btn.dataset.code);
        });
      });

      tbody.querySelectorAll('.btn-batches-med').forEach(btn => {
        btn.addEventListener('click', () => {
          this.openMedicineBatchesModal(btn.dataset.id);
        });
      });

      tbody.querySelectorAll('.btn-edit-med').forEach(btn => {
        btn.addEventListener('click', () => {
          this.openEditMedicineModal(container, btn.dataset.id);
        });
      });

    } catch (err) {
      tbody.innerHTML = `<tr><td colspan="10" style="text-align: center; color: var(--status-danger); padding: 20px;">${err.message}</td></tr>`;
    }
  }

  openBarcodeModal(name, barcode) {
    const code = barcode || ('MED' + Math.floor(100000000000 + Math.random() * 900000000000));
    const bodyHtml = `
      <div style="text-align: center; padding: 10px;">
        <div style="font-weight: 800; font-size: 18px; margin-bottom: 4px;">${name}</div>
        <div style="font-size: 12px; color: var(--text-muted); margin-bottom: 16px;">Apex Care Pharmacy • Verification Code: ${code}</div>
        
        <div style="background: #ffffff; padding: 20px; border-radius: 8px; display: inline-block; box-shadow: var(--shadow-md);">
          <canvas id="label-barcode-canvas" style="display: block; margin: 0 auto;"></canvas>
          <div style="display: flex; justify-content: center; margin-top: 14px;">
            <canvas id="label-qr-canvas"></canvas>
          </div>
        </div>

        <div style="margin-top: 20px; font-size: 12px; color: var(--text-secondary);">
          Ready for clinical thermal label printing (4x2 inch standard standard prescription format).
        </div>
      </div>
    `;

    const footerHtml = `
      <button class="btn btn-secondary" onclick="window.modal.close()">Close</button>
      <button class="btn btn-primary" onclick="window.print()">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 6 2 18 2 18 9"/><path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"/><rect width="12" height="8" x="6" y="14"/></svg>
        Print Label
      </button>
    `;

    const modalEl = window.modal.open('Print Drug Label & Barcode', bodyHtml, footerHtml);
    setTimeout(() => {
      const bcCanvas = modalEl.querySelector('#label-barcode-canvas');
      const qrCanvas = modalEl.querySelector('#label-qr-canvas');
      if (window.barcodeGen) {
        window.barcodeGen.drawBarcode(bcCanvas, code, { width: 320, height: 90 });
        window.barcodeGen.drawQRCode(qrCanvas, `MED:${code}:${name}`, 100);
      }
    }, 50);
  }

  async openMedicineBatchesModal(medId) {
    try {
      const data = await window.api.getMedicineById(medId);
      const m = data.medicine;
      const batches = data.batches || [];

      const bodyHtml = `
        <div style="margin-bottom: 16px;">
          <div style="font-size: 16px; font-weight: 800; color: var(--text-primary);">${m.name} (${m.genericName})</div>
          <div style="font-size: 12px; color: var(--text-muted);">Reorder Level: ${m.reorderLevel} units | Storage: ${m.storageCondition}</div>
        </div>
        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>Batch No</th>
                <th>Mfg Date</th>
                <th>Expiry Date</th>
                <th>Stock</th>
                <th>Price</th>
                <th>Discount</th>
                <th>Shelf</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              ${batches.length === 0 ? '<tr><td colspan="8" style="text-align: center; padding: 16px;">No active batches recorded.</td></tr>' : batches.map(b => `
                <tr>
                  <td><strong>${b.batchNumber}</strong></td>
                  <td>${b.mfgDate}</td>
                  <td>${b.expiryDate}</td>
                  <td><strong>${b.quantity}</strong></td>
                  <td>$${b.unitPrice.toFixed(2)}</td>
                  <td>${b.discountPercent > 0 ? `<span class="badge badge-warning">${b.discountPercent}% OFF</span>` : '0%'}</td>
                  <td>${b.shelfLocation}</td>
                  <td><span class="badge badge-${b.expiryStatus.toLowerCase()}">${b.expiryStatus}</span></td>
                </tr>
              `).join('')}
            </tbody>
          </table>
        </div>
      `;

      const footerHtml = `
        <button class="btn btn-secondary" onclick="window.modal.close()">Close</button>
        <button class="btn btn-primary" id="btn-modal-add-batch-for-med">Add Stock Batch</button>
      `;

      const modalEl = window.modal.open(`Stock Batches: ${m.name}`, bodyHtml, footerHtml, '740px');
      modalEl.querySelector('#btn-modal-add-batch-for-med').addEventListener('click', () => {
        window.modal.close();
        if (window.batchView) window.batchView.openAddBatchModal(document.getElementById('content-viewport'), m.id);
      });

    } catch (e) {
      window.toast.error('Error', e.message);
    }
  }

  openAddMedicineModal(container) {
    const catOptions = this.categories.map(c => `<option value="${c.id}">${c.name}</option>`).join('');
    const randomBarcode = '890' + Math.floor(100000000 + Math.random() * 900000000);

    const bodyHtml = `
      <form id="form-add-medicine">
        <div class="form-grid-2">
          <div class="form-group">
            <label class="form-label">Brand Name *</label>
            <input type="text" class="form-control" name="name" required placeholder="e.g. Amoxil 500">
          </div>
          <div class="form-group">
            <label class="form-label">Generic / Chemical Name *</label>
            <input type="text" class="form-control" name="genericName" required placeholder="e.g. Amoxicillin Trihydrate">
          </div>
        </div>

        <div class="form-grid-3">
          <div class="form-group">
            <label class="form-label">Category *</label>
            <select class="form-control" name="categoryId" required>
              ${catOptions}
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">Dosage Form *</label>
            <select class="form-control" name="dosageForm" required>
              <option value="Tablet">Tablet</option>
              <option value="Capsule">Capsule</option>
              <option value="Syrup">Syrup</option>
              <option value="Injection">Injection</option>
              <option value="Inhaler">Inhaler</option>
              <option value="Drops">Drops (Eye/Ear)</option>
              <option value="Ointment">Ointment / Cream</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">Strength *</label>
            <input type="text" class="form-control" name="strength" required placeholder="e.g. 500mg, 10ml">
          </div>
        </div>

        <div class="form-grid-3">
          <div class="form-group">
            <label class="form-label">Unit of Measure *</label>
            <input type="text" class="form-control" name="unit" required placeholder="e.g. Strip (10 Tabs)">
          </div>
          <div class="form-group">
            <label class="form-label">Reorder Safety Level</label>
            <input type="number" class="form-control" name="reorderLevel" value="20" min="1">
          </div>
          <div class="form-group">
            <label class="form-label">Min Expiry Alert (Days)</label>
            <input type="number" class="form-control" name="minAlertDays" value="60" min="5">
          </div>
        </div>

        <div class="form-grid-2">
          <div class="form-group">
            <label class="form-label">Storage Condition</label>
            <select class="form-control" name="storageCondition">
              <option value="Room Temperature (15-25°C)">Room Temperature (15-25°C)</option>
              <option value="Refrigerated (2-8°C)">Refrigerated (2-8°C) - Cold Chain</option>
              <option value="Cool & Dry Place (<25°C)">Cool & Dry Place (&lt;25°C)</option>
              <option value="Frozen (Below -10°C)">Frozen (Below -10°C)</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">Barcode / SKU Number</label>
            <input type="text" class="form-control" name="barcode" value="${randomBarcode}">
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">Side Effects & Contraindications</label>
          <input type="text" class="form-control" name="sideEffects" placeholder="e.g. Nausea, dizziness, avoid in pregnancy">
        </div>

        <div class="form-group" style="display: flex; flex-direction: row; align-items: center; gap: 10px; margin-top: 6px;">
          <input type="checkbox" id="add-rx-req" name="requiresPrescription" style="width: 18px; height: 18px;">
          <label for="add-rx-req" class="form-label" style="margin: 0; cursor: pointer;">
            Prescription Required by Law (Schedule H / Rx Only)
          </label>
        </div>
      </form>
    `;

    const footerHtml = `
      <button class="btn btn-secondary" onclick="window.modal.close()">Cancel</button>
      <button class="btn btn-primary" id="btn-submit-add-med">Save Medicine</button>
    `;

    const modalEl = window.modal.open('Register New Pharmaceutical Medicine', bodyHtml, footerHtml, '680px');
    modalEl.querySelector('#btn-submit-add-med').addEventListener('click', async () => {
      const form = modalEl.querySelector('#form-add-medicine');
      if (!form.checkValidity()) {
        form.reportValidity();
        return;
      }

      const fd = new FormData(form);
      const data = {
        name: fd.get('name'),
        genericName: fd.get('genericName'),
        categoryId: parseInt(fd.get('categoryId')),
        dosageForm: fd.get('dosageForm'),
        strength: fd.get('strength'),
        unit: fd.get('unit'),
        reorderLevel: parseInt(fd.get('reorderLevel')),
        minAlertDays: parseInt(fd.get('minAlertDays')),
        storageCondition: fd.get('storageCondition'),
        barcode: fd.get('barcode'),
        sideEffects: fd.get('sideEffects'),
        requiresPrescription: fd.get('requiresPrescription') === 'on'
      };

      try {
        await window.api.createMedicine(data);
        window.toast.success('Medicine Created', `Registered ${data.name} to the catalog.`);
        window.modal.close();
        this.loadMedicines(container);
      } catch (err) {
        window.toast.error('Save Failed', err.message);
      }
    });
  }

  async openEditMedicineModal(container, id) {
    try {
      const data = await window.api.getMedicineById(id);
      const m = data.medicine;
      const catOptions = this.categories.map(c => `<option value="${c.id}" ${c.id === m.categoryId ? 'selected' : ''}>${c.name}</option>`).join('');

      const bodyHtml = `
        <form id="form-edit-medicine">
          <div class="form-grid-2">
            <div class="form-group">
              <label class="form-label">Brand Name *</label>
              <input type="text" class="form-control" name="name" value="${m.name}" required>
            </div>
            <div class="form-group">
              <label class="form-label">Generic / Chemical Name *</label>
              <input type="text" class="form-control" name="genericName" value="${m.genericName}" required>
            </div>
          </div>

          <div class="form-grid-3">
            <div class="form-group">
              <label class="form-label">Category *</label>
              <select class="form-control" name="categoryId" required>
                ${catOptions}
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">Dosage Form *</label>
              <select class="form-control" name="dosageForm" required>
                <option value="Tablet" ${m.dosageForm === 'Tablet' ? 'selected' : ''}>Tablet</option>
                <option value="Capsule" ${m.dosageForm === 'Capsule' ? 'selected' : ''}>Capsule</option>
                <option value="Syrup" ${m.dosageForm === 'Syrup' ? 'selected' : ''}>Syrup</option>
                <option value="Injection" ${m.dosageForm === 'Injection' ? 'selected' : ''}>Injection</option>
                <option value="Inhaler" ${m.dosageForm === 'Inhaler' ? 'selected' : ''}>Inhaler</option>
                <option value="Drops" ${m.dosageForm === 'Drops' ? 'selected' : ''}>Drops (Eye/Ear)</option>
                <option value="Ointment" ${m.dosageForm === 'Ointment' ? 'selected' : ''}>Ointment / Cream</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">Strength *</label>
              <input type="text" class="form-control" name="strength" value="${m.strength}" required>
            </div>
          </div>

          <div class="form-grid-3">
            <div class="form-group">
              <label class="form-label">Unit *</label>
              <input type="text" class="form-control" name="unit" value="${m.unit}" required>
            </div>
            <div class="form-group">
              <label class="form-label">Reorder Level</label>
              <input type="number" class="form-control" name="reorderLevel" value="${m.reorderLevel}">
            </div>
            <div class="form-group">
              <label class="form-label">Min Alert Days</label>
              <input type="number" class="form-control" name="minAlertDays" value="${m.minAlertDays}">
            </div>
          </div>

          <div class="form-grid-2">
            <div class="form-group">
              <label class="form-label">Storage Condition</label>
              <input type="text" class="form-control" name="storageCondition" value="${m.storageCondition}">
            </div>
            <div class="form-group">
              <label class="form-label">Barcode</label>
              <input type="text" class="form-control" name="barcode" value="${m.barcode || ''}">
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">Side Effects</label>
            <input type="text" class="form-control" name="sideEffects" value="${m.sideEffects || ''}">
          </div>

          <div class="form-group" style="display: flex; flex-direction: row; align-items: center; gap: 10px;">
            <input type="checkbox" id="edit-rx-req" name="requiresPrescription" ${m.requiresPrescription ? 'checked' : ''} style="width: 18px; height: 18px;">
            <label for="edit-rx-req" class="form-label" style="margin: 0; cursor: pointer;">
              Prescription Required by Law (Rx Only)
            </label>
          </div>
        </form>
      `;

      const footerHtml = `
        <button class="btn btn-secondary" onclick="window.modal.close()">Cancel</button>
        <button class="btn btn-primary" id="btn-submit-edit-med">Update Medicine</button>
      `;

      const modalEl = window.modal.open(`Edit Medicine: ${m.name}`, bodyHtml, footerHtml, '680px');
      modalEl.querySelector('#btn-submit-edit-med').addEventListener('click', async () => {
        const form = modalEl.querySelector('#form-edit-medicine');
        const fd = new FormData(form);
        const updateData = {
          name: fd.get('name'),
          genericName: fd.get('genericName'),
          categoryId: parseInt(fd.get('categoryId')),
          dosageForm: fd.get('dosageForm'),
          strength: fd.get('strength'),
          unit: fd.get('unit'),
          reorderLevel: parseInt(fd.get('reorderLevel')),
          minAlertDays: parseInt(fd.get('minAlertDays')),
          storageCondition: fd.get('storageCondition'),
          barcode: fd.get('barcode'),
          sideEffects: fd.get('sideEffects'),
          requiresPrescription: fd.get('requiresPrescription') === 'on'
        };

        try {
          await window.api.updateMedicine(id, updateData);
          window.toast.success('Medicine Updated', `Saved changes for ${updateData.name}`);
          window.modal.close();
          this.loadMedicines(container);
        } catch (err) {
          window.toast.error('Update Failed', err.message);
        }
      });

    } catch (e) {
      window.toast.error('Load Error', e.message);
    }
  }
}

window.inventoryView = new InventoryView();
