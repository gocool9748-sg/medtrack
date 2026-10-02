/**
 * Modal Dialog Manager
 */
class ModalManager {
  constructor() {
    this.activeModal = null;
  }

  open(title, bodyHtml, footerHtml = '', maxWidth = '620px') {
    this.close(); // Close existing if open

    const overlay = document.createElement('div');
    overlay.className = 'modal-overlay';
    overlay.id = 'active-modal-overlay';

    overlay.innerHTML = `
      <div class="modal-dialog" style="max-width: ${maxWidth};">
        <div class="modal-header">
          <div class="modal-title">${title}</div>
          <button class="modal-close-btn" id="modal-btn-close">✕</button>
        </div>
        <div class="modal-body">
          ${bodyHtml}
        </div>
        ${footerHtml ? `<div class="modal-footer">${footerHtml}</div>` : ''}
      </div>
    `;

    document.body.appendChild(overlay);

    const closeBtn = overlay.querySelector('#modal-btn-close');
    closeBtn.addEventListener('click', () => this.close());

    overlay.addEventListener('click', (e) => {
      if (e.target === overlay) this.close();
    });

    // Animate in
    requestAnimationFrame(() => {
      overlay.classList.add('active');
    });

    this.activeModal = overlay;
    return overlay;
  }

  close() {
    const existing = document.getElementById('active-modal-overlay');
    if (existing) {
      existing.classList.remove('active');
      setTimeout(() => existing.remove(), 250);
    }
    this.activeModal = null;
  }
}

window.modal = new ModalManager();
