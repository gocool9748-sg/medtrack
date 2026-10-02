/**
 * Hash-Based Single Page Application (SPA) Router
 */
class AppRouter {
  constructor() {
    this.routes = {
      '#dashboard': window.dashboardView,
      '#inventory': window.inventoryView,
      '#batches': window.batchView,
      '#pos': window.posView,
      '#expiry': window.expiryView,
      '#orders': window.orderView,
      '#suppliers': window.supplierView,
      '#disposal': window.disposalView,
      '#analytics': window.analyticsView,
      '#audit': window.auditView,
      '#settings': window.settingsView,
      '#login': window.loginView
    };

    window.addEventListener('hashchange', () => this.handleRoute());
  }

  init() {
    this.handleRoute();
  }

  navigate(hash) {
    window.location.hash = hash;
  }

  async handleRoute() {
    const hash = window.location.hash || '#dashboard';
    const container = document.getElementById('content-viewport');
    if (!container) return;

    // Check Authentication
    const token = window.api.getToken();
    if (!token && hash !== '#login') {
      this.navigate('#login');
      return;
    }

    if (token && hash === '#login') {
      this.navigate('#dashboard');
      return;
    }

    // Toggle Navigation Sidebar visibility for login screen
    const sidebar = document.getElementById('app-sidebar');
    const header = document.getElementById('app-header');
    if (hash === '#login') {
      if (sidebar) sidebar.style.display = 'none';
      if (header) header.style.display = 'none';
    } else {
      if (sidebar) sidebar.style.display = 'flex';
      if (header) header.style.display = 'flex';
    }

    // Update active state on sidebar nav items
    document.querySelectorAll('.nav-item').forEach(item => {
      const linkHash = item.getAttribute('href');
      if (linkHash === hash) {
        item.classList.add('active');
      } else {
        item.classList.remove('active');
      }
    });

    // Resolve view
    const view = this.routes[hash] || this.routes['#dashboard'];
    if (view && typeof view.render === 'function') {
      try {
        await view.render(container);
      } catch (err) {
        console.error('Error rendering route:', err);
        container.innerHTML = `
          <div class="glass-card" style="text-align: center; padding: 40px; color: var(--status-danger);">
            <h2>Route Error</h2>
            <p style="margin-top: 8px;">${err.message}</p>
            <button class="btn btn-secondary" onclick="window.router.navigate('#dashboard')" style="margin-top: 16px;">
              Return to Dashboard
            </button>
          </div>
        `;
      }
    }
  }
}

window.router = new AppRouter();
