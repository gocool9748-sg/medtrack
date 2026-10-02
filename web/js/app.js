/**
 * MedTrack Main Application Entry Point & Orchestrator
 */
class MedTrackApp {
  constructor() {
    this.clockInterval = null;
  }

  async init() {
    console.log('[MedTrack] Initializing Enterprise Healthcare Suite...');

    // 1. Initialize Theme
    document.documentElement.setAttribute('data-theme', window.state.theme);

    // 2. Start Live Clock
    this.startClock();

    // 3. Setup Global Keyboard Shortcuts
    this.setupShortcuts();

    // 4. Setup Notification Panel Dropdown
    this.setupNotificationDropdown();

    // 5. Setup Quick Header Actions
    this.setupHeaderActions();

    // 6. Verify Active Session & User Info
    await this.checkSession();

    // 7. Initialize SPA Router
    window.router.init();

    // 8. Initial Notification Fetch
    if (window.api.getToken()) {
      this.fetchNotifications();
    }
  }

  startClock() {
    const clockEl = document.getElementById('header-live-time');
    const update = () => {
      if (clockEl) {
        const now = new Date();
        clockEl.textContent = now.toLocaleTimeString('en-US', { hour12: false });
      }
    };
    update();
    this.clockInterval = setInterval(update, 1000);
  }

  setupShortcuts() {
    window.addEventListener('keydown', (e) => {
      // Ignore in input/textarea unless Escape
      const isInput = ['INPUT', 'TEXTAREA', 'SELECT'].includes(document.activeElement.tagName);

      if (e.key === 'Escape') {
        if (window.modal) window.modal.close();
        const notifPanel = document.getElementById('notifications-dropdown');
        if (notifPanel) notifPanel.classList.remove('active');
      }

      if (isInput) return;

      if (e.key === '/' || (e.ctrlKey && e.key === 'k')) {
        e.preventDefault();
        const searchInput = document.getElementById('global-search-input') || document.getElementById('pos-med-search') || document.getElementById('inv-search-input');
        if (searchInput) searchInput.focus();
      } else if (e.key === 'F2') {
        e.preventDefault();
        window.router.navigate('#pos');
      } else if (e.key === 'F1') {
        e.preventDefault();
        window.router.navigate('#dashboard');
      }
    });

    // Global Search Enter Handler
    const globalSearch = document.getElementById('global-search-input');
    if (globalSearch) {
      globalSearch.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
          const query = globalSearch.value.trim();
          if (query) {
            window.inventoryView.searchQuery = query;
            window.router.navigate('#inventory');
          }
        }
      });
    }
  }

  setupHeaderActions() {
    // Theme toggle button
    const themeBtn = document.getElementById('btn-header-theme-toggle');
    if (themeBtn) {
      themeBtn.addEventListener('click', () => {
        window.state.toggleTheme();
        window.toast.info('Theme Changed', `Switched to ${window.state.theme} mode.`);
      });
    }

    // Launch POS button
    const posBtn = document.getElementById('btn-header-launch-pos');
    if (posBtn) {
      posBtn.addEventListener('click', () => {
        window.router.navigate('#pos');
      });
    }

    // User Logout Action
    const logoutBtn = document.getElementById('btn-sidebar-logout');
    if (logoutBtn) {
      logoutBtn.addEventListener('click', async () => {
        if (confirm('Are you sure you want to sign out of MedTrack?')) {
          try {
            await window.api.logout();
          } catch (e) {}
          window.api.setToken(null);
          window.state.setCurrentUser(null);
          window.toast.info('Signed Out', 'You have been logged out.');
          window.router.navigate('#login');
        }
      });
    }
  }

  setupNotificationDropdown() {
    const bellBtn = document.getElementById('btn-header-notifications');
    const panel = document.getElementById('notifications-dropdown');
    const markAllBtn = document.getElementById('btn-notif-mark-all');

    if (bellBtn && panel) {
      bellBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        panel.classList.toggle('active');
        if (panel.classList.contains('active')) {
          this.fetchNotifications();
        }
      });

      document.addEventListener('click', (e) => {
        if (!panel.contains(e.target) && e.target !== bellBtn) {
          panel.classList.remove('active');
        }
      });
    }

    if (markAllBtn) {
      markAllBtn.addEventListener('click', async () => {
        try {
          await window.api.markAllNotificationsRead();
          window.toast.success('Alerts Cleared', 'All notifications marked as read.');
          this.fetchNotifications();
        } catch (e) {
          window.toast.error('Error', e.message);
        }
      });
    }
  }

  async fetchNotifications() {
    try {
      const res = await window.api.getNotifications();
      const notifs = res.notifications || [];
      const unreadCount = res.unreadCount || 0;

      window.state.setNotifications(notifs, unreadCount);

      // Update Header Bell Badge
      const badge = document.getElementById('header-notif-count');
      if (badge) {
        if (unreadCount > 0) {
          badge.textContent = unreadCount > 9 ? '9+' : unreadCount;
          badge.style.display = 'flex';
        } else {
          badge.style.display = 'none';
        }
      }

      // Populate Dropdown Body
      const body = document.getElementById('notifications-dropdown-body');
      if (body) {
        if (notifs.length === 0) {
          body.innerHTML = '<div style="text-align: center; padding: 24px; color: var(--text-muted);">No new notifications.</div>';
        } else {
          body.innerHTML = notifs.map(n => `
            <div class="notification-item ${!n.isRead ? 'unread' : ''}" data-id="${n.id}" data-link="${n.linkType}" data-target="${n.linkId}">
              <div class="notification-dot ${n.severity}"></div>
              <div class="notif-text-wrap">
                <div class="notif-title">${n.title}</div>
                <div class="notif-msg">${n.message}</div>
                <div class="notif-time">${n.createdAt}</div>
              </div>
            </div>
          `).join('');

          body.querySelectorAll('.notification-item').forEach(item => {
            item.addEventListener('click', async () => {
              const id = item.dataset.id;
              await window.api.markNotificationRead(id);
              item.classList.remove('unread');
              const link = item.dataset.link;
              if (link === 'BATCH') window.router.navigate('#batches');
              else if (link === 'MEDICINE') window.router.navigate('#inventory');
              else if (link === 'PO') window.router.navigate('#orders');
            });
          });
        }
      }

    } catch (e) {}
  }

  async checkSession() {
    const token = window.api.getToken();
    if (!token) return;

    try {
      const user = await window.api.getMe();
      window.state.setCurrentUser(user);
      this.updateUserHeader();
    } catch (e) {
      console.warn('Session check failed:', e.message);
      window.api.setToken(null);
      window.state.setCurrentUser(null);
    }
  }

  updateUserHeader() {
    const user = window.state.currentUser;
    if (!user) return;

    const nameEl = document.getElementById('sidebar-user-name');
    const roleEl = document.getElementById('sidebar-user-role');
    const avatarEl = document.getElementById('sidebar-user-avatar');

    if (nameEl) nameEl.textContent = user.fullName || user.username;
    if (roleEl) roleEl.textContent = user.role;
    if (avatarEl) {
      const initials = (user.fullName || user.username).substring(0, 2).toUpperCase();
      avatarEl.textContent = initials;
    }
  }
}

window.app = new MedTrackApp();
document.addEventListener('DOMContentLoaded', () => {
  window.app.init();
});
