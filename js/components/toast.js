/**
 * Toast Notification Manager
 */
class ToastManager {
  constructor() {
    this.container = document.getElementById('toast-container');
    if (!this.container) {
      this.container = document.createElement('div');
      this.container.id = 'toast-container';
      document.body.appendChild(this.container);
    }
  }

  show(type, title, message, duration = 4000) {
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;

    let icon = 'ℹ️';
    if (type === 'success') {
      icon = '✅';
      if (window.soundEffects) window.soundEffects.playSuccess();
    } else if (type === 'error') {
      icon = '⚠️';
      if (window.soundEffects) window.soundEffects.playWarning();
    } else if (type === 'warning') {
      icon = '🔔';
      if (window.soundEffects) window.soundEffects.playWarning();
    }

    toast.innerHTML = `
      <div style="font-size: 18px;">${icon}</div>
      <div class="toast-content">
        <div class="toast-title">${title}</div>
        <div class="toast-message">${message}</div>
      </div>
      <div class="toast-close" onclick="this.parentElement.remove()">✕</div>
    `;

    this.container.appendChild(toast);

    // Trigger animation
    requestAnimationFrame(() => {
      toast.classList.add('show');
    });

    setTimeout(() => {
      toast.classList.remove('show');
      setTimeout(() => toast.remove(), 300);
    }, duration);
  }

  success(title, message) { this.show('success', title, message); }
  error(title, message) { this.show('error', title, message, 5000); }
  warning(title, message) { this.show('warning', title, message, 4500); }
  info(title, message) { this.show('info', title, message); }
}

window.toast = new ToastManager();
