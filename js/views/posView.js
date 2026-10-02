/**
 * Point of Sale (POS) & Clinical Dispensing Terminal
 */
class PosView {
  constructor() {
    this.medicines = [];
    this.searchQuery = '';
  }

  async render(container) {
    container.innerHTML = `
      <div class="pos-container">
        <!-- Left Panel: Medicine Search & Quick Add Grid -->
        <div class="pos-catalog-panel">
          <div class="pos-search-header">
            <div style="flex: 1; position: relative;">
              <svg class="search-icon-pos" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" x2="16.65" y1="21" y2="16.65"/></svg>
              <input type="text" id="pos-med-search" class="pos-search-input" placeholder="Scan barcode or search medicine name / generic formula (Press / to focus)..." autofocus>
            </div>
            <button class="btn btn-secondary" id="btn-simulate-barcode" title="Simulate Barcode Scanner">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 5v14"/><path d="M8 5v14"/><path d="M12 5v14"/><path d="M17 5v14"/><path d="M21 5v14"/></svg>
              Scan
            </button>
          </div>

          <div class="pos-items-grid" id="pos-grid-items">
            <div style="grid-column: 1/-1; text-align: center; padding: 40px; color: var(--text-muted);">
              Loading dispensary catalog...
            </div>
          </div>
        </div>

        <!-- Right Panel: Cart, Patient Rx Details & Fast Checkout -->
        <div class="pos-checkout-panel">
          <div class="pos-cart-header">
            <div class="pos-cart-title">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--brand-primary)" stroke-width="2"><circle cx="8" cy="21" r="1"/><circle cx="19" cy="21" r="1"/><path d="M2.05 2.05h2l2.66 12.42a2 2 0 0 0 2 1.58h9.78a2 2 0 0 0 1.95-1.57l1.65-7.43H5.12"/></svg>
              Dispensary Cart (<span id="cart-item-count">0</span> items)
            </div>
            <button class="btn btn-secondary btn-sm" id="btn-clear-cart" style="font-size: 11px;">Clear</button>
          </div>

          <!-- Patient & Prescription Details -->
          <div class="pos-patient-info">
            <input type="text" id="pos-cust-name" placeholder="Patient / Customer Name" value="${window.state.posCustomer.name || ''}">
            <input type="text" id="pos-cust-phone" placeholder="Phone Number" value="${window.state.posCustomer.phone || ''}">
            <input type="text" id="pos-doc-name" placeholder="Prescribing Doctor" value="${window.state.posCustomer.doctor || ''}">
            <input type="text" id="pos-rx-no" placeholder="Prescription / Rx #" value="${window.state.posCustomer.prescriptionNo || ''}">
          </div>

          <!-- Cart Line Items -->
          <div class="pos-cart-items-wrap" id="pos-cart-list">
            <div style="margin: auto; text-align: center; color: var(--text-muted); font-size: 13px;">
              🛒 Cart is empty.<br>Click any medicine or scan a barcode to add.
            </div>
          </div>

          <!-- Order Financial Summary -->
          <div class="pos-summary-section">
            <div class="summary-row">
              <span>Subtotal</span>
              <span id="pos-sum-subtotal">$0.00</span>
            </div>
            <div class="summary-row" style="color: var(--status-safe);">
              <span>Discounts Applied</span>
              <span id="pos-sum-discount">-$0.00</span>
            </div>
            <div class="summary-row">
              <span>Tax / GST (5.0%)</span>
              <span id="pos-sum-tax">$0.00</span>
            </div>
            <div class="summary-row total">
              <span>Final Total</span>
              <span id="pos-sum-total" style="color: var(--brand-primary);">$0.00</span>
            </div>

            <!-- Payment Method Selector -->
            <div class="pos-payment-selector" id="payment-methods-box">
              <div class="pay-opt-btn active" data-method="CASH">💵 CASH</div>
              <div class="pay-opt-btn" data-method="CARD">💳 CARD</div>
              <div class="pay-opt-btn" data-method="UPI">📱 UPI</div>
              <div class="pay-opt-btn" data-method="INSURANCE">🏥 INS</div>
            </div>

            <button class="btn-checkout-now" id="btn-complete-pos-sale">
              Complete & Print Invoice (F4)
            </button>
          </div>
        </div>
      </div>
    `;

    // Load medicines
    this.loadCatalog(container);

    // Bind Search Input
    let timeout = null;
    const searchInput = container.querySelector('#pos-med-search');
    searchInput.addEventListener('input', (e) => {
      clearTimeout(timeout);
      timeout = setTimeout(() => {
        this.searchQuery = e.target.value.trim();
        this.renderCatalogGrid(container);
      }, 200);
    });

    // Simulate Barcode Scanner
    container.querySelector('#btn-simulate-barcode').addEventListener('click', () => {
      this.openBarcodeScannerSimulation(container);
    });

    // Clear cart button
    container.querySelector('#btn-clear-cart').addEventListener('click', () => {
      window.state.clearCart();
    });

    // Payment methods
    const payBox = container.querySelector('#payment-methods-box');
    payBox.querySelectorAll('.pay-opt-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        payBox.querySelectorAll('.pay-opt-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        window.state.posCustomer.paymentMethod = btn.dataset.method;
      });
    });

    // Sync Customer fields with state
    ['pos-cust-name', 'pos-cust-phone', 'pos-doc-name', 'pos-rx-no'].forEach(id => {
      const el = container.querySelector(`#${id}`);
      el.addEventListener('input', () => {
        window.state.posCustomer.name = container.querySelector('#pos-cust-name').value;
        window.state.posCustomer.phone = container.querySelector('#pos-cust-phone').value;
        window.state.posCustomer.doctor = container.querySelector('#pos-doc-name').value;
        window.state.posCustomer.prescriptionNo = container.querySelector('#pos-rx-no').value;
      });
    });

    // Cart changes listener
    window.state.on('cartChanged', () => {
      this.updateCartUI(container);
    });

    // Checkout button
    container.querySelector('#btn-complete-pos-sale').addEventListener('click', () => {
      this.processCheckout(container);
    });

    this.updateCartUI(container);
  }

  async loadCatalog(container) {
    try {
      this.medicines = await window.api.getMedicines();
      this.renderCatalogGrid(container);
    } catch (e) {
      window.toast.error('Catalog Error', e.message);
    }
  }

  renderCatalogGrid(container) {
    const grid = container.querySelector('#pos-grid-items');
    let list = this.medicines;

    if (this.searchQuery) {
      const q = this.searchQuery.toLowerCase();
      list = list.filter(m => 
        m.name.toLowerCase().includes(q) || 
        m.genericName.toLowerCase().includes(q) || 
        (m.barcode && m.barcode.toLowerCase().includes(q))
      );
    }

    if (list.length === 0) {
      grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; padding: 40px; color: var(--text-muted);">No matching medicines found.</div>';
      return;
    }

    grid.innerHTML = list.map(m => {
      const inStock = m.totalStock > 0;
      return `
        <div class="pos-med-card ${!inStock ? 'out-of-stock' : ''}" data-id="${m.id}" style="${!inStock ? 'opacity: 0.6; cursor: not-allowed;' : ''}">
          <div>
            <div class="pos-med-name">${m.name}</div>
            <div class="pos-med-generic">${m.genericName} • ${m.strength}</div>
            <div style="font-size: 11px; color: var(--text-muted); margin-top: 4px;">${m.unit}</div>
          </div>
          <div class="pos-med-footer">
            <div class="pos-med-price">$${m.minPrice > 0 ? m.minPrice.toFixed(2) : '0.00'}</div>
            <span class="pos-stock-pill ${inStock ? 'badge-safe' : 'badge-danger'}">
              ${inStock ? `${m.totalStock} in stock` : 'Out of Stock'}
            </span>
          </div>
        </div>
      `;
    }).join('');

    // Bind Card Clicks for instant FEFO batch resolution
    grid.querySelectorAll('.pos-med-card').forEach(card => {
      card.addEventListener('click', async () => {
        const medId = parseInt(card.dataset.id);
        const med = this.medicines.find(m => m.id === medId);
        if (!med || med.totalStock <= 0) {
          window.toast.warning('Out of Stock', 'No active stock batches available for this medicine.');
          return;
        }

        try {
          // Fetch FEFO ordered batches for this medicine
          const batches = await window.api.getFEFOBatches(medId);
          if (!batches || batches.length === 0) {
            window.toast.warning('No Safe Batches', 'All batches for this drug are expired or quarantined.');
            return;
          }

          // Pick the first batch (Earliest unexpired batch - FEFO algorithm!)
          const fefoBatch = batches[0];
          window.state.addToCart(med, fefoBatch, 1);
          window.toast.success('FEFO Auto-Assigned', `Added ${med.name} (Oldest safe batch: ${fefoBatch.batchNumber}, Exp: ${fefoBatch.expiryDate})`);
        } catch (e) {
          window.toast.error('FEFO Error', e.message);
        }
      });
    });
  }

  updateCartUI(container) {
    const cartList = container.querySelector('#pos-cart-list');
    const countEl = container.querySelector('#cart-item-count');
    const cart = window.state.posCart;

    countEl.textContent = cart.reduce((sum, i) => sum + i.quantity, 0);

    if (cart.length === 0) {
      cartList.innerHTML = `
        <div style="margin: auto; text-align: center; color: var(--text-muted); font-size: 13px;">
          🛒 Cart is empty.<br>Click any medicine or scan a barcode to add.
        </div>
      `;
    } else {
      cartList.innerHTML = cart.map(item => `
        <div class="pos-cart-item">
          <div class="cart-item-info">
            <div class="cart-item-title">${item.medicineName}</div>
            <div class="cart-item-batch-tag">Batch: ${item.batchNumber} (Exp: ${item.expiryDate})</div>
            <div style="font-size: 11px; color: var(--text-muted);">$${item.unitPrice.toFixed(2)}/unit ${item.discountPercent > 0 ? `(${item.discountPercent}% off)` : ''}</div>
          </div>
          <div class="cart-item-controls">
            <button class="cart-qty-btn btn-cart-minus" data-id="${item.batchId}">-</button>
            <span class="cart-qty-val">${item.quantity}</span>
            <button class="cart-qty-btn btn-cart-plus" data-id="${item.batchId}">+</button>
          </div>
          <div class="cart-item-total">$${item.subtotal.toFixed(2)}</div>
        </div>
      `).join('');

      cartList.querySelectorAll('.btn-cart-minus').forEach(b => {
        b.addEventListener('click', () => window.state.updateCartQty(parseInt(b.dataset.id), -1));
      });
      cartList.querySelectorAll('.btn-cart-plus').forEach(b => {
        b.addEventListener('click', () => window.state.updateCartQty(parseInt(b.dataset.id), 1));
      });
    }

    // Update Totals
    const totals = window.state.getCartTotals();
    container.querySelector('#pos-sum-subtotal').textContent = `$${totals.subtotal.toFixed(2)}`;
    container.querySelector('#pos-sum-discount').textContent = `-$${totals.totalDiscount.toFixed(2)}`;
    container.querySelector('#pos-sum-tax').textContent = `$${totals.tax.toFixed(2)}`;
    container.querySelector('#pos-sum-total').textContent = `$${totals.finalAmount.toFixed(2)}`;
  }

  async processCheckout(container) {
    const cart = window.state.posCart;
    if (cart.length === 0) {
      window.toast.warning('Empty Cart', 'Please add items before checkout.');
      return;
    }

    const totals = window.state.getCartTotals();
    const customer = window.state.posCustomer;

    const salePayload = {
      customerName: customer.name ? customer.name.trim() : 'Walk-in Patient',
      customerPhone: customer.phone ? customer.phone.trim() : null,
      doctorName: customer.doctor ? customer.doctor.trim() : 'Prescribing Physician / OTC',
      prescriptionNo: customer.prescriptionNo ? customer.prescriptionNo.trim() : null,
      totalAmount: totals.subtotal,
      discountAmount: totals.totalDiscount,
      taxAmount: totals.tax,
      finalAmount: totals.finalAmount,
      paymentMethod: customer.paymentMethod || 'CASH',
      items: cart.map(i => ({
        medicineId: i.medicineId,
        medicineName: i.medicineName,
        batchId: i.batchId,
        quantity: i.quantity,
        unitPrice: i.unitPrice,
        discountPercent: i.discountPercent,
        subtotal: i.subtotal
      }))
    };

    try {
      const completedSale = await window.api.createSale(salePayload);
      window.toast.success('Sale Dispensed', `Invoice ${completedSale.invoiceNumber} processed successfully!`);
      
      // Clear cart
      window.state.clearCart();
      this.loadCatalog(container);

      // Open printable tax invoice modal
      this.openInvoiceReceiptModal(completedSale);

    } catch (e) {
      window.toast.error('Checkout Failed', e.message);
    }
  }

  openInvoiceReceiptModal(sale) {
    const bodyHtml = `
      <div class="printable-area">
        <div class="invoice-container">
          <div class="invoice-header">
            <div class="inv-pharm-name">APEX CARE PHARMACY</div>
            <div class="inv-pharm-meta">Clinical Dispensing & Healthcare Unit</div>
            <div class="inv-pharm-meta">Drug Lic: DL-MED-2026-98741X • GSTIN: 36AAACH7412K1Z9</div>
            <div class="inv-pharm-meta">Plot 42, Healthcare Blvd, Tech City • Phone: +91 98765 43210</div>
          </div>

          <div class="inv-meta-grid">
            <div>
              <strong>INVOICE NO:</strong> ${sale.invoiceNumber}<br>
              <strong>DATE:</strong> ${sale.saleDate}<br>
              <strong>PAYMENT:</strong> ${sale.paymentMethod}
            </div>
            <div style="text-align: right;">
              <strong>PATIENT:</strong> ${sale.customerName}<br>
              <strong>PHONE:</strong> ${sale.customerPhone || 'N/A'}<br>
              <strong>DOCTOR:</strong> ${sale.doctorName || 'N/A'}<br>
              ${sale.prescriptionNo ? `<strong>Rx NO:</strong> ${sale.prescriptionNo}` : ''}
            </div>
          </div>

          <table class="inv-table">
            <thead>
              <tr>
                <th>Item & Batch</th>
                <th>Qty</th>
                <th>Rate</th>
                <th>Disc</th>
                <th style="text-align: right;">Total</th>
              </tr>
            </thead>
            <tbody>
              ${sale.items.map(i => `
                <tr>
                  <td>
                    <strong>${i.medicineName}</strong><br>
                    <small style="color: #64748b;">Batch: ${i.batchNumber || 'FEFO'} (Exp: ${i.expiryDate || 'Verified'})</small>
                  </td>
                  <td>${i.quantity}</td>
                  <td>$${i.unitPrice.toFixed(2)}</td>
                  <td>${i.discountPercent > 0 ? `${i.discountPercent}%` : '-'}</td>
                  <td style="text-align: right;">$${i.subtotal.toFixed(2)}</td>
                </tr>
              `).join('')}
            </tbody>
          </table>

          <div class="inv-totals-box">
            <div class="inv-total-row">
              <span>Gross Total:</span>
              <span>$${sale.totalAmount.toFixed(2)}</span>
            </div>
            ${sale.discountAmount > 0 ? `
              <div class="inv-total-row" style="color: #10b981;">
                <span>Total Discount:</span>
                <span>-$${sale.discountAmount.toFixed(2)}</span>
              </div>
            ` : ''}
            <div class="inv-total-row">
              <span>GST / Healthcare Tax (5.0%):</span>
              <span>$${sale.taxAmount.toFixed(2)}</span>
            </div>
            <div class="inv-total-row inv-grand-total">
              <span>NET AMOUNT PAID:</span>
              <span>$${sale.finalAmount.toFixed(2)}</span>
            </div>
          </div>

          <div class="inv-qr-wrap">
            <canvas id="inv-qr-canvas"></canvas>
          </div>

          <div class="inv-footer">
            Thank you for trusting Apex Care Pharmacy.<br>
            Medicines once sold cannot be returned without verified doctor prescription slip.<br>
            *** Pharmacist Signature & Stamp ***
          </div>
        </div>
      </div>
    `;

    const footerHtml = `
      <button class="btn btn-secondary" onclick="window.modal.close()">Close</button>
      <button class="btn btn-primary" onclick="window.print()">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 6 2 18 2 18 9"/><path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"/><rect width="12" height="8" x="6" y="14"/></svg>
        Print Tax Invoice
      </button>
    `;

    const modalEl = window.modal.open(`Invoice: ${sale.invoiceNumber}`, bodyHtml, footerHtml, '650px');
    setTimeout(() => {
      const qrCanvas = modalEl.querySelector('#inv-qr-canvas');
      if (window.barcodeGen) {
        window.barcodeGen.drawQRCode(qrCanvas, `INVOICE:${sale.invoiceNumber}:TOTAL:${sale.finalAmount}`, 90);
      }
    }, 50);
  }

  openBarcodeScannerSimulation(container) {
    const bodyHtml = `
      <div style="text-align: center; padding: 10px;">
        <div style="font-size: 14px; color: var(--text-secondary); margin-bottom: 16px;">
          Select or scan a medicine barcode to simulate a high-speed hardware laser barcode reader.
        </div>
        <div style="display: flex; flex-direction: column; gap: 8px; max-height: 280px; overflow-y: auto;">
          ${this.medicines.slice(0, 10).map(m => `
            <button class="btn btn-secondary btn-simulate-item" data-code="${m.barcode}" data-name="${m.name}" style="justify-content: space-between; text-align: left;">
              <span><strong>${m.name}</strong> (${m.genericName})</span>
              <span style="font-family: monospace; color: var(--brand-primary);">${m.barcode}</span>
            </button>
          `).join('')}
        </div>
      </div>
    `;

    const modalEl = window.modal.open('Simulate Barcode Scanner', bodyHtml, '<button class="btn btn-secondary" onclick="window.modal.close()">Cancel</button>');
    modalEl.querySelectorAll('.btn-simulate-item').forEach(btn => {
      btn.addEventListener('click', async () => {
        const barcode = btn.dataset.code;
        window.modal.close();
        try {
          const res = await window.api.getMedicineByBarcode(barcode);
          if (res && res.batches && res.batches.length > 0) {
            window.state.addToCart(res.medicine, res.batches[0], 1);
            window.toast.success('Barcode Scanned', `Scanned ${res.medicine.name} (Code: ${barcode})`);
          } else {
            window.toast.warning('Out of Stock', 'Scanned drug has no active stock batches.');
          }
        } catch (e) {
          window.toast.error('Scan Error', e.message);
        }
      });
    });
  }
}

window.posView = new PosView();
