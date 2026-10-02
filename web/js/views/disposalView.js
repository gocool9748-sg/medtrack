/**
 * Safe Drug Disposal & Hazardous Biomedical Waste Compliance View
 */
class DisposalView {
  async render(container) {
    container.innerHTML = `
      <div class="view-header">
        <div class="view-title-group">
          <h1>Biomedical Safe Disposal & Destruction Log</h1>
          <p>Regulatory compliance registry for expired, contaminated, and recalled pharmaceutical waste</p>
        </div>
        <div class="view-actions">
          <button class="btn btn-danger btn-sm" id="btn-record-disposal">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
            Record Disposal Event
          </button>
        </div>
      </div>

      <!-- Compliance Guidelines Card -->
      <div class="glass-card" style="margin-bottom: 24px; border-left: 4px solid var(--status-purple);">
        <div style="display: flex; gap: 16px; align-items: flex-start;">
          <div style="font-size: 24px;">☣️</div>
          <div>
            <div style="font-weight: 800; font-size: 15px; color: var(--text-primary); margin-bottom: 4px;">
              EPA & FDA Healthcare Hazardous Waste Disposal Standards
            </div>
            <p style="font-size: 12.5px; color: var(--text-secondary); line-height: 1.6;">
              All expired antibiotics, cytotoxics, and scheduled medications must undergo certified high-temperature incineration, chemical encapsulation, or return-to-manufacturer logging. Every entry generates an immutable regulatory certificate for environmental safety audits.
            </p>
          </div>
        </div>
      </div>

      <!-- Disposal History Table -->
      <div class="glass-card">
        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>Certificate #</th>
                <th>Drug Name & Generic</th>
                <th>Batch Number</th>
                <th>Quantity Destroyed</th>
                <th>Destruction Method</th>
                <th>Disposal Reason</th>
                <th>Total Financial Loss</th>
                <th>Authorized By</th>
                <th>Disposal Date</th>
              </tr>
            </thead>
            <tbody id="disposal-table-body">
              <tr><td colspan="9" style="text-align: center; padding: 24px;">Loading destruction logs...</td></tr>
            </tbody>
          </table>
        </div>
      </div>
    `;

    this.loadDisposals(container);

    container.querySelector('#btn-record-disposal').addEventListener('click', () => {
      this.openRecordDisposalModal(container);
    });
  }

  async loadDisposals(container) {
    const tbody = container.querySelector('#disposal-table-body');
    try {
      const list = await window.api.getDisposals();
      if (!list || list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" style="text-align: center; padding: 30px; color: var(--text-muted);">No disposal records found.</td></tr>';
        return;
      }

      tbody.innerHTML = list.map(d => `
        <tr>
          <td><strong style="font-family: monospace; color: var(--brand-primary);">${d.certificateNumber}</strong></td>
          <td>
            <div style="font-weight: 700;">${d.medicineName}</div>
            <div style="font-size: 11px; color: var(--text-muted);">${d.genericName}</div>
          </td>
          <td><span style="font-family: monospace;">${d.batchNumber}</span></td>
          <td><strong style="color: var(--status-danger);">${d.quantity}</strong> units</td>
          <td><span class="badge badge-purple">${d.destructionMethod}</span></td>
          <td><span class="badge badge-neutral">${d.reason}</span></td>
          <td style="font-weight: 800; color: var(--status-danger);">-$${d.totalLoss.toFixed(2)}</td>
          <td style="font-size: 11.5px; color: var(--text-secondary);">${d.authorizerName || 'Chief Inspector'}</td>
          <td>${d.disposalDate}</td>
        </tr>
      `).join('');
    } catch (e) {
      tbody.innerHTML = `<tr><td colspan="9" style="text-align: center; color: var(--status-danger); padding: 20px;">${e.message}</td></tr>`;
    }
  }

  async openRecordDisposalModal(container) {
    try {
      const [batches, meds] = await Promise.all([
        window.api.getBatches(),
        window.api.getMedicines()
      ]);

      const batchOptions = batches.map(b => `<option value="${b.id}" data-medid="${b.medicineId}" data-batch="${b.batchNumber}" data-qty="${b.quantity}" data-cost="${b.unitCost}">${b.batchNumber} - ${b.medicineName} (${b.quantity} units, Exp: ${b.expiryDate})</option>`).join('');

      const today = new Date().toISOString().substring(0, 10);
      const certNo = 'DISP-CERT-' + Math.floor(100000 + Math.random() * 900000);

      const bodyHtml = `
        <form id="form-record-disposal">
          <div class="form-grid-2">
            <div class="form-group">
              <label class="form-label">Certificate ID</label>
              <input type="text" class="form-control" name="certificateNumber" value="${certNo}" readonly style="font-family: monospace; color: var(--brand-primary);">
            </div>
            <div class="form-group">
              <label class="form-label">Disposal Date *</label>
              <input type="date" class="form-control" name="disposalDate" value="${today}" required>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">Select Target Batch to Destroy *</label>
            <select class="form-control" id="disp-batch-select" required>
              ${batchOptions}
            </select>
          </div>

          <div class="form-grid-2">
            <div class="form-group">
              <label class="form-label">Quantity for Destruction *</label>
              <input type="number" class="form-control" name="quantity" id="disp-qty-input" value="10" min="1" required>
            </div>
            <div class="form-group">
              <label class="form-label">Destruction Method *</label>
              <select class="form-control" name="destructionMethod" required>
                <option value="INCINERATION">High-Temperature Incineration (1200°C)</option>
                <option value="ENCAPSULATION">Chemical Encapsulation & Solidification</option>
                <option value="RETURN_TO_VENDOR">Return to Vendor / Manufacturer Recall</option>
                <option value="CHEMICAL_INACTIVATION">Chemical Neutralization</option>
              </select>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">Disposal Justification Reason *</label>
            <select class="form-control" name="reason" required>
              <option value="EXPIRED">Expired Beyond Safety Grace Period</option>
              <option value="COLD_CHAIN_FAILURE">Cold Chain Refrigeration Breach</option>
              <option value="DAMAGED">Damaged in Transit / Broken Packaging</option>
              <option value="RECALLED">FDA / Manufacturer Safety Recall</option>
              <option value="CONTAMINATED">Suspected Contamination</option>
            </select>
          </div>

          <div class="form-group">
            <label class="form-label">Compliance Inspector Notes</label>
            <input type="text" class="form-control" name="notes" placeholder="e.g. Incinerated at Biohazard Waste Station 4 with witness verification.">
          </div>
        </form>
      `;

      const footerHtml = `
        <button class="btn btn-secondary" onclick="window.modal.close()">Cancel</button>
        <button class="btn btn-danger" id="btn-submit-disposal">Authorize Destruction</button>
      `;

      const modalEl = window.modal.open('Authorize Biomedical Drug Disposal', bodyHtml, footerHtml);
      const batchSel = modalEl.querySelector('#disp-batch-select');
      const qtyInput = modalEl.querySelector('#disp-qty-input');

      // Update max qty on selection
      batchSel.addEventListener('change', () => {
        const selectedOpt = batchSel.options[batchSel.selectedIndex];
        const maxQty = parseInt(selectedOpt.dataset.qty);
        qtyInput.max = maxQty;
        qtyInput.value = Math.min(parseInt(qtyInput.value), maxQty);
      });

      modalEl.querySelector('#btn-submit-disposal').addEventListener('click', async () => {
        const form = modalEl.querySelector('#form-record-disposal');
        if (!form.checkValidity()) {
          form.reportValidity();
          return;
        }

        const fd = new FormData(form);
        const selectedOpt = batchSel.options[batchSel.selectedIndex];
        const batchId = parseInt(batchSel.value);
        const medId = parseInt(selectedOpt.dataset.medid);
        const batchNum = selectedOpt.dataset.batch;
        const unitCost = parseFloat(selectedOpt.dataset.cost);
        const qty = parseInt(fd.get('quantity'));

        const payload = {
          certificateNumber: fd.get('certificateNumber'),
          disposalDate: fd.get('disposalDate'),
          batchId,
          medicineId: medId,
          batchNumber: batchNum,
          quantity: qty,
          unitCost,
          totalLoss: qty * unitCost,
          destructionMethod: fd.get('destructionMethod'),
          reason: fd.get('reason'),
          notes: fd.get('notes')
        };

        try {
          await window.api.createDisposal(payload);
          window.toast.success('Disposal Authorized', `Recorded destruction of ${qty} units under ${payload.certificateNumber}.`);
          window.modal.close();
          this.loadDisposals(container);
        } catch (err) {
          window.toast.error('Disposal Failed', err.message);
        }
      });

    } catch (e) {
      window.toast.error('Error', e.message);
    }
  }
}

window.disposalView = new DisposalView();
