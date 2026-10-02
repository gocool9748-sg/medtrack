/**
 * System Administration, User Management & Pharmacy Settings View
 */
class SettingsView {
  async render(container) {
    const u = window.state.currentUser || {};
    container.innerHTML = `
      <div class="view-header">
        <div class="view-title-group">
          <h1>System Settings & Administration</h1>
          <p>Role-based user management, clinical facility metadata & database backup utilities</p>
        </div>
      </div>

      <!-- Quick Role / Persona Switcher (For Demonstration & Hackathons) -->
      <div class="glass-card" style="margin-bottom: 24px; border-left: 4px solid var(--brand-primary); background: linear-gradient(135deg, rgba(6,182,212,0.1) 0%, rgba(17,24,39,0.7) 100%);">
        <div class="glass-panel-header">
          <div class="glass-panel-title">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--brand-primary)" stroke-width="2"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
            RBAC Demo Persona Switcher (Instant Evaluation Switcher)
          </div>
          <span class="badge badge-safe">Active Persona: ${u.role || 'ADMIN'}</span>
        </div>
        <p style="font-size: 13px; color: var(--text-secondary); margin-bottom: 14px;">
          Instantly switch between healthcare roles to demonstrate access controls, permissions, and specialized workflows:
        </p>
        <div style="display: flex; gap: 10px; flex-wrap: wrap;">
          <button class="btn btn-secondary btn-switch-persona" data-user="admin" data-pass="Admin@123">
            👑 Dr. Sarah Jenkins (ADMIN)
          </button>
          <button class="btn btn-secondary btn-switch-persona" data-user="pharmacist" data-pass="Pharm@123">
            💊 Alex Vance, PharmD (PHARMACIST)
          </button>
          <button class="btn btn-secondary btn-switch-persona" data-user="manager" data-pass="Manager@123">
            📦 Marcus Brody (INVENTORY_MANAGER)
          </button>
          <button class="btn btn-secondary btn-switch-persona" data-user="auditor" data-pass="Audit@123">
            🔍 Elena Rostova (AUDITOR)
          </button>
        </div>
      </div>

      <!-- User Accounts Table -->
      <div class="glass-card" style="margin-bottom: 24px;">
        <div class="glass-panel-header">
          <div class="glass-panel-title">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--brand-primary)" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/></svg>
            Staff User Accounts & Access Roles
          </div>
          <button class="btn btn-primary btn-sm" id="btn-add-staff-user">
            + Add Staff User
          </button>
        </div>
        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>Username</th>
                <th>Full Name</th>
                <th>Email Address</th>
                <th>System Role</th>
                <th>Status</th>
                <th>Last Login</th>
              </tr>
            </thead>
            <tbody id="settings-users-tbody">
              <tr><td colspan="6" style="text-align: center; padding: 20px;">Loading users...</td></tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Pharmacy Facility Profile -->
      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 24px; margin-bottom: 24px;">
        <div class="glass-card">
          <div class="glass-panel-header">
            <div class="glass-panel-title">🏥 Pharmacy Facility Metadata</div>
          </div>
          <div style="display: flex; flex-direction: column; gap: 10px; font-size: 13px;">
            <div><strong>Pharmacy Name:</strong> Apex Care Pharmacy & Healthcare</div>
            <div><strong>Drug License #:</strong> DL-MED-2026-98741X</div>
            <div><strong>GST / Tax ID:</strong> 36AAACH7412K1Z9</div>
            <div><strong>Phone Contact:</strong> +91 98765 43210</div>
            <div><strong>Dispensary Email:</strong> dispensary@apexmedtrack.io</div>
            <div><strong>Address:</strong> Plot 42, Healthcare Blvd, Tech City</div>
          </div>
        </div>

        <div class="glass-card">
          <div class="glass-panel-header">
            <div class="glass-panel-title">⚙️ Preferences & Utilities</div>
          </div>
          <div style="display: flex; flex-direction: column; gap: 14px;">
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <div>
                <strong>Dark / Light Theme</strong>
                <div style="font-size: 12px; color: var(--text-muted);">Toggle interface color scheme</div>
              </div>
              <button class="btn btn-secondary btn-sm" id="btn-settings-toggle-theme">
                ${window.state.theme === 'dark' ? '☀️ Light Mode' : '🌙 Dark Mode'}
              </button>
            </div>

            <div style="display: flex; justify-content: space-between; align-items: center;">
              <div>
                <strong>Synthesized Audio Feedback</strong>
                <div style="font-size: 12px; color: var(--text-muted);">Web Audio API chimes & beeps</div>
              </div>
              <button class="btn btn-secondary btn-sm" id="btn-toggle-sound">
                ${window.soundEffects.enabled ? '🔊 Audio ON' : '🔇 Audio OFF'}
              </button>
            </div>

            <div style="display: flex; justify-content: space-between; align-items: center; border-top: 1px solid var(--bg-card-border); padding-top: 12px;">
              <div>
                <strong style="color: var(--status-danger);">Re-Seed & Verify Demo Data</strong>
                <div style="font-size: 12px; color: var(--text-muted);">Restores clinical demo records if empty</div>
              </div>
              <button class="btn btn-outline-danger btn-sm" id="btn-reset-db">
                Reset Demo Data
              </button>
            </div>
          </div>
        </div>
      </div>
    `;

    this.loadUsers(container);

    // Persona Switcher
    container.querySelectorAll('.btn-switch-persona').forEach(btn => {
      btn.addEventListener('click', async () => {
        try {
          const res = await window.api.login(btn.dataset.user, btn.dataset.pass);
          window.api.setToken(res.token);
          window.state.setCurrentUser(res);
          window.toast.success('Switched Role', `Logged in as ${res.fullName} (${res.role})`);
          this.render(container);
          // Update sidebar badge
          if (window.app) window.app.updateUserHeader();
        } catch (e) {
          window.toast.error('Switch Error', e.message);
        }
      });
    });

    // Theme toggle
    container.querySelector('#btn-settings-toggle-theme').addEventListener('click', () => {
      window.state.toggleTheme();
      this.render(container);
    });

    // Sound toggle
    container.querySelector('#btn-toggle-sound').addEventListener('click', (e) => {
      window.soundEffects.enabled = !window.soundEffects.enabled;
      e.target.textContent = window.soundEffects.enabled ? '🔊 Audio ON' : '🔇 Audio OFF';
      window.toast.info('Audio Settings', `Sound effects ${window.soundEffects.enabled ? 'enabled' : 'muted'}.`);
    });

    // Reset Demo Data
    container.querySelector('#btn-reset-db').addEventListener('click', async () => {
      if (confirm('Verify and re-seed database with default clinical records?')) {
        try {
          await window.api.resetDatabase();
          window.toast.success('Database Verified', 'Healthcare dataset verified and ready.');
        } catch (e) {
          window.toast.error('Reset Failed', e.message);
        }
      }
    });

    // Add Staff User
    container.querySelector('#btn-add-staff-user').addEventListener('click', () => {
      this.openAddUserModal(container);
    });
  }

  async loadUsers(container) {
    const tbody = container.querySelector('#settings-users-tbody');
    try {
      const users = await window.api.getUsers();
      if (!users || users.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align: center; padding: 20px;">No users found or permission restricted.</td></tr>';
        return;
      }

      tbody.innerHTML = users.map(u => `
        <tr>
          <td><strong>${u.username}</strong></td>
          <td>${u.fullName}</td>
          <td>${u.email}</td>
          <td><span class="badge ${u.role === 'ADMIN' ? 'badge-safe' : 'badge-neutral'}">${u.role}</span></td>
          <td><span class="badge ${u.isActive ? 'badge-safe' : 'badge-danger'}">${u.isActive ? 'Active' : 'Inactive'}</span></td>
          <td style="font-size: 11.5px; color: var(--text-muted);">${u.lastLogin || 'Never'}</td>
        </tr>
      `).join('');
    } catch (e) {
      tbody.innerHTML = `<tr><td colspan="6" style="text-align: center; color: var(--text-muted); padding: 20px;">${e.message}</td></tr>`;
    }
  }

  openAddUserModal(container) {
    const bodyHtml = `
      <form id="form-add-user">
        <div class="form-grid-2">
          <div class="form-group">
            <label class="form-label">Username *</label>
            <input type="text" class="form-control" name="username" required placeholder="e.g. jdoe">
          </div>
          <div class="form-group">
            <label class="form-label">Password *</label>
            <input type="password" class="form-control" name="password" required placeholder="Min 6 characters">
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">Full Name & Credentials *</label>
          <input type="text" class="form-control" name="fullName" required placeholder="e.g. Dr. Jane Doe, PharmD">
        </div>

        <div class="form-grid-2">
          <div class="form-group">
            <label class="form-label">Email Address *</label>
            <input type="email" class="form-control" name="email" required placeholder="jane.doe@apexmedtrack.io">
          </div>
          <div class="form-group">
            <label class="form-label">System Role *</label>
            <select class="form-control" name="role" required>
              <option value="PHARMACIST">PHARMACIST (Dispensing, Batches, POS)</option>
              <option value="INVENTORY_MANAGER">INVENTORY_MANAGER (Stock, Suppliers, POs)</option>
              <option value="AUDITOR">AUDITOR (Audit trail, Compliance logs)</option>
              <option value="ADMIN">ADMIN (Full Superuser Privileges)</option>
            </select>
          </div>
        </div>
      </form>
    `;

    const footerHtml = `
      <button class="btn btn-secondary" onclick="window.modal.close()">Cancel</button>
      <button class="btn btn-primary" id="btn-submit-add-user">Create User Account</button>
    `;

    const modalEl = window.modal.open('Register Staff Account', bodyHtml, footerHtml);
    modalEl.querySelector('#btn-submit-add-user').addEventListener('click', async () => {
      const form = modalEl.querySelector('#form-add-user');
      if (!form.checkValidity()) {
        form.reportValidity();
        return;
      }

      const fd = new FormData(form);
      const payload = {
        username: fd.get('username'),
        password: fd.get('password'),
        fullName: fd.get('fullName'),
        email: fd.get('email'),
        role: fd.get('role')
      };

      try {
        await window.api.createUser(payload);
        window.toast.success('User Created', `Added staff user ${payload.username} (${payload.role})`);
        window.modal.close();
        this.loadUsers(container);
      } catch (e) {
        window.toast.error('Error', e.message);
      }
    });
  }
}

window.settingsView = new SettingsView();
