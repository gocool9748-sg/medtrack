/**
 * Authentication & Login View
 */
class LoginView {
  async render(container) {
    container.innerHTML = `
      <div style="min-height: 80vh; display: flex; align-items: center; justify-content: center; padding: 20px;">
        <div class="glass-card" style="width: 100%; max-width: 440px; padding: 36px;">
          <div style="text-align: center; margin-bottom: 28px;">
            <div style="width: 56px; height: 56px; border-radius: var(--radius-lg); background: var(--brand-gradient); margin: 0 auto 16px auto; display: flex; align-items: center; justify-content: center; color: #fff; box-shadow: var(--glow-cyan);">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m10.5 20.5 10-10a4.95 4.95 0 1 0-7-7l-10 10a4.95 4.95 0 1 0 7 7Z"/><path d="m8.5 8.5 7 7"/></svg>
            </div>
            <h1 style="font-size: 22px; font-weight: 800; color: var(--text-primary);">MEDTRACK</h1>
            <p style="font-size: 13px; color: var(--text-muted); margin-top: 4px;">Medicine Stock & Expiry Management System</p>
          </div>

          <form id="form-login">
            <div class="form-group">
              <label class="form-label">Username</label>
              <input type="text" class="form-control" name="username" id="login-username" required placeholder="Enter your username" value="admin">
            </div>

            <div class="form-group">
              <label class="form-label">Password</label>
              <input type="password" class="form-control" name="password" id="login-password" required placeholder="Enter your password" value="Admin@123">
            </div>

            <button type="submit" class="btn btn-gradient" style="width: 100%; height: 44px; margin-top: 10px; font-size: 14px;">
              Sign In to MedTrack
            </button>
          </form>

          <div style="margin-top: 24px; padding-top: 20px; border-top: 1px solid var(--bg-card-border);">
            <div style="font-size: 11px; font-weight: 700; text-transform: uppercase; color: var(--text-muted); margin-bottom: 10px; text-align: center;">
              Demo Accounts (Click to Autofill)
            </div>
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px;">
              <button class="btn btn-secondary btn-sm btn-fill-login" data-user="admin" data-pass="Admin@123">
                👑 Admin
              </button>
              <button class="btn btn-secondary btn-sm btn-fill-login" data-user="pharmacist" data-pass="Pharm@123">
                💊 Pharmacist
              </button>
              <button class="btn btn-secondary btn-sm btn-fill-login" data-user="manager" data-pass="Manager@123">
                📦 Manager
              </button>
              <button class="btn btn-secondary btn-sm btn-fill-login" data-user="auditor" data-pass="Audit@123">
                🔍 Auditor
              </button>
            </div>
          </div>
        </div>
      </div>
    `;

    // Demo Autofill buttons
    container.querySelectorAll('.btn-fill-login').forEach(btn => {
      btn.addEventListener('click', () => {
        container.querySelector('#login-username').value = btn.dataset.user;
        container.querySelector('#login-password').value = btn.dataset.pass;
      });
    });

    // Handle Form Submit
    container.querySelector('#form-login').addEventListener('submit', async (e) => {
      e.preventDefault();
      const user = container.querySelector('#login-username').value.trim();
      const pass = container.querySelector('#login-password').value.trim();

      try {
        const res = await window.api.login(user, pass);
        window.api.setToken(res.token);
        window.state.setCurrentUser(res);
        window.toast.success('Welcome Back', `Logged in as ${res.fullName}`);
        if (window.app) window.app.updateUserHeader();
        window.router.navigate('#dashboard');
      } catch (err) {
        window.toast.error('Login Failed', err.message);
      }
    });
  }
}

window.loginView = new LoginView();
