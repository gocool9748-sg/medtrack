/**
 * Supplier & Vendor Directory View
 */
class SupplierView {
  async render(container) {
    container.innerHTML = `
      <div class="view-header">
        <div class="view-title-group">
          <h1>Pharmaceutical Supplier Directory</h1>
          <p>Verified pharmaceutical manufacturers, authorized distributors & logistics vendors</p>
        </div>
        <div class="view-actions">
          <button class="btn btn-gradient btn-sm" id="btn-add-supplier">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" x2="12" y1="5" y2="19"/><line x1="5" x2="19" y1="12" y2="12"/></svg>
            Register Supplier
          </button>
        </div>
      </div>

      <!-- Supplier Cards Grid -->
      <div id="suppliers-cards-grid" style="display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 20px;">
        <div style="grid-column: 1/-1; text-align: center; padding: 40px; color: var(--text-muted);">
          Loading verified suppliers...
        </div>
      </div>
    `;

    this.loadSuppliers(container);

    container.querySelector('#btn-add-supplier').addEventListener('click', () => {
      this.openAddSupplierModal(container);
    });
  }

  async loadSuppliers(container) {
    const grid = container.querySelector('#suppliers-cards-grid');
    try {
      const suppliers = await window.api.getSuppliers();
      if (!suppliers || suppliers.length === 0) {
        grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; padding: 40px; color: var(--text-muted);">No suppliers registered yet.</div>';
        return;
      }

      grid.innerHTML = suppliers.map(s => {
        const ratingStars = '⭐'.repeat(Math.round(s.rating || 5));
        return `
          <div class="glass-card" style="display: flex; flex-direction: column; justify-content: space-between;">
            <div>
              <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 8px;">
                <div style="font-size: 16px; font-weight: 800; color: var(--text-primary);">${s.name}</div>
                <span class="badge badge-safe" style="font-size: 11px;">${ratingStars} ${s.rating.toFixed(1)}</span>
              </div>
              <div style="font-size: 12px; color: var(--text-muted); margin-bottom: 12px;">Tax / GST ID: <code style="color: var(--brand-primary);">${s.taxId || 'Verified'}</code></div>

              <div style="display: flex; flex-direction: column; gap: 6px; font-size: 13px; color: var(--text-secondary); margin-bottom: 16px;">
                <div>👤 <strong>Contact:</strong> ${s.contactPerson || 'Logistics Desk'}</div>
                <div>✉️ <strong>Email:</strong> <a href="mailto:${s.email}" style="color: var(--brand-primary); text-decoration: none;">${s.email || 'N/A'}</a></div>
                <div>📞 <strong>Phone:</strong> ${s.phone || 'N/A'}</div>
                <div>🏢 <strong>Address:</strong> ${s.address || 'Global Supply Hub'}</div>
              </div>
            </div>

            <div style="padding-top: 14px; border-top: 1px solid var(--bg-card-border); display: flex; align-items: center; justify-content: space-between;">
              <span class="badge badge-neutral">${s.totalOrders || 0} Orders Fulfilled</span>
              <div style="display: flex; gap: 6px;">
                <button class="btn btn-secondary btn-sm btn-edit-supp" data-id="${s.id}">Edit</button>
              </div>
            </div>
          </div>
        `;
      }).join('');

      grid.querySelectorAll('.btn-edit-supp').forEach(b => {
        b.addEventListener('click', () => {
          this.openEditSupplierModal(container, parseInt(b.dataset.id));
        });
      });

    } catch (e) {
      grid.innerHTML = `<div style="grid-column: 1/-1; text-align: center; color: var(--status-danger); padding: 20px;">${e.message}</div>`;
    }
  }

  openAddSupplierModal(container) {
    const bodyHtml = `
      <form id="form-add-supplier">
        <div class="form-group">
          <label class="form-label">Company / Distributor Name *</label>
          <input type="text" class="form-control" name="name" required placeholder="e.g. Novartis Global Supply">
        </div>
        <div class="form-grid-2">
          <div class="form-group">
            <label class="form-label">Contact Person *</label>
            <input type="text" class="form-control" name="contactPerson" required placeholder="e.g. David Kim">
          </div>
          <div class="form-group">
            <label class="form-label">Tax / GST / EIN ID *</label>
            <input type="text" class="form-control" name="taxId" required placeholder="e.g. US-EIN-9482710">
          </div>
        </div>
        <div class="form-grid-2">
          <div class="form-group">
            <label class="form-label">Email Address *</label>
            <input type="email" class="form-control" name="email" required placeholder="orders@pharma-corp.com">
          </div>
          <div class="form-group">
            <label class="form-label">Phone Number *</label>
            <input type="tel" class="form-control" name="phone" required placeholder="+1-800-555-0199">
          </div>
        </div>
        <div class="form-group">
          <label class="form-label">Warehouse / Headquarters Address</label>
          <input type="text" class="form-control" name="address" placeholder="100 Technology Square, Cambridge, MA">
        </div>
        <div class="form-group">
          <label class="form-label">Supplier Rating (1.0 to 5.0)</label>
          <input type="number" step="0.1" min="1.0" max="5.0" class="form-control" name="rating" value="4.9">
        </div>
      </form>
    `;

    const footerHtml = `
      <button class="btn btn-secondary" onclick="window.modal.close()">Cancel</button>
      <button class="btn btn-primary" id="btn-submit-add-supp">Register Supplier</button>
    `;

    const modalEl = window.modal.open('Register Pharmaceutical Supplier', bodyHtml, footerHtml);
    modalEl.querySelector('#btn-submit-add-supp').addEventListener('click', async () => {
      const form = modalEl.querySelector('#form-add-supplier');
      if (!form.checkValidity()) {
        form.reportValidity();
        return;
      }
      const fd = new FormData(form);
      const data = {
        name: fd.get('name'),
        contactPerson: fd.get('contactPerson'),
        taxId: fd.get('taxId'),
        email: fd.get('email'),
        phone: fd.get('phone'),
        address: fd.get('address'),
        rating: parseFloat(fd.get('rating'))
      };

      try {
        await window.api.createSupplier(data);
        window.toast.success('Supplier Registered', `Added ${data.name} to vendors.`);
        window.modal.close();
        this.loadSuppliers(container);
      } catch (err) {
        window.toast.error('Save Error', err.message);
      }
    });
  }

  async openEditSupplierModal(container, id) {
    try {
      const s = await window.api.request(`/suppliers/${id}`);
      const bodyHtml = `
        <form id="form-edit-supplier">
          <div class="form-group">
            <label class="form-label">Company Name *</label>
            <input type="text" class="form-control" name="name" value="${s.name}" required>
          </div>
          <div class="form-grid-2">
            <div class="form-group">
              <label class="form-label">Contact Person *</label>
              <input type="text" class="form-control" name="contactPerson" value="${s.contactPerson}" required>
            </div>
            <div class="form-group">
              <label class="form-label">Tax / GST ID *</label>
              <input type="text" class="form-control" name="taxId" value="${s.taxId}" required>
            </div>
          </div>
          <div class="form-grid-2">
            <div class="form-group">
              <label class="form-label">Email *</label>
              <input type="email" class="form-control" name="email" value="${s.email}" required>
            </div>
            <div class="form-group">
              <label class="form-label">Phone *</label>
              <input type="tel" class="form-control" name="phone" value="${s.phone}" required>
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">Address</label>
            <input type="text" class="form-control" name="address" value="${s.address || ''}">
          </div>
          <div class="form-group">
            <label class="form-label">Rating</label>
            <input type="number" step="0.1" min="1.0" max="5.0" class="form-control" name="rating" value="${s.rating}">
          </div>
        </form>
      `;

      const footerHtml = `
        <button class="btn btn-secondary" onclick="window.modal.close()">Cancel</button>
        <button class="btn btn-primary" id="btn-submit-edit-supp">Save Changes</button>
      `;

      const modalEl = window.modal.open(`Edit Supplier: ${s.name}`, bodyHtml, footerHtml);
      modalEl.querySelector('#btn-submit-edit-supp').addEventListener('click', async () => {
        const form = modalEl.querySelector('#form-edit-supplier');
        const fd = new FormData(form);
        const data = {
          name: fd.get('name'),
          contactPerson: fd.get('contactPerson'),
          taxId: fd.get('taxId'),
          email: fd.get('email'),
          phone: fd.get('phone'),
          address: fd.get('address'),
          rating: parseFloat(fd.get('rating'))
        };

        try {
          await window.api.updateSupplier(id, data);
          window.toast.success('Updated', `Saved changes for ${data.name}`);
          window.modal.close();
          this.loadSuppliers(container);
        } catch (err) {
          window.toast.error('Update Error', err.message);
        }
      });

    } catch (e) {
      window.toast.error('Error', e.message);
    }
  }
}

window.supplierView = new SupplierView();
