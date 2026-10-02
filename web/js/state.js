/**
 * Central Reactive Application State
 */
class AppState {
  constructor() {
    this.currentUser = null;
    this.theme = localStorage.getItem('medtrack_theme') || 'dark';
    this.unreadNotifications = 0;
    this.notifications = [];
    this.posCart = [];
    this.posCustomer = {
      name: '',
      phone: '',
      doctor: '',
      prescriptionNo: '',
      paymentMethod: 'CASH',
      discountPercent: 0
    };
    this.listeners = new Map();
  }

  on(event, callback) {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, []);
    }
    this.listeners.get(event).push(callback);
  }

  emit(event, data) {
    if (this.listeners.has(event)) {
      this.listeners.get(event).forEach(cb => cb(data));
    }
  }

  setCurrentUser(user) {
    this.currentUser = user;
    this.emit('userChanged', user);
  }

  setTheme(theme) {
    this.theme = theme;
    localStorage.setItem('medtrack_theme', theme);
    document.documentElement.setAttribute('data-theme', theme);
    this.emit('themeChanged', theme);
  }

  toggleTheme() {
    this.setTheme(this.theme === 'dark' ? 'light' : 'dark');
  }

  setNotifications(notifs, unreadCount) {
    this.notifications = notifs || [];
    this.unreadNotifications = unreadCount || 0;
    this.emit('notificationsChanged', { notifs: this.notifications, unreadCount: this.unreadNotifications });
  }

  // POS Cart Management
  addToCart(medicine, batch, quantity = 1) {
    const existingIndex = this.posCart.findIndex(item => item.batchId === batch.id);
    if (existingIndex >= 0) {
      const newQty = this.posCart[existingIndex].quantity + quantity;
      if (newQty > batch.quantity) {
        window.toast.warning('Stock Limit Reached', `Only ${batch.quantity} units available in batch ${batch.batchNumber}.`);
        return;
      }
      this.posCart[existingIndex].quantity = newQty;
      this.posCart[existingIndex].subtotal = newQty * this.posCart[existingIndex].unitPrice * (1 - (this.posCart[existingIndex].discountPercent / 100));
    } else {
      if (quantity > batch.quantity) {
        window.toast.warning('Stock Limit Reached', `Only ${batch.quantity} units available in batch ${batch.batchNumber}.`);
        return;
      }
      const unitPrice = batch.unitPrice || 0;
      const discount = batch.discountPercent || 0;
      const subtotal = quantity * unitPrice * (1 - (discount / 100));
      this.posCart.push({
        medicineId: medicine.id,
        medicineName: medicine.name,
        genericName: medicine.genericName,
        dosageForm: medicine.dosageForm,
        batchId: batch.id,
        batchNumber: batch.batchNumber,
        expiryDate: batch.expiryDate,
        quantity,
        maxStock: batch.quantity,
        unitPrice,
        discountPercent: discount,
        subtotal
      });
    }

    if (window.soundEffects) window.soundEffects.playBeep();
    this.emit('cartChanged', this.posCart);
  }

  updateCartQty(batchId, delta) {
    const item = this.posCart.find(i => i.batchId === batchId);
    if (!item) return;

    const newQty = item.quantity + delta;
    if (newQty <= 0) {
      this.removeFromCart(batchId);
      return;
    }
    if (newQty > item.maxStock) {
      window.toast.warning('Stock Limit', `Maximum available stock in this batch is ${item.maxStock}.`);
      return;
    }

    item.quantity = newQty;
    item.subtotal = newQty * item.unitPrice * (1 - (item.discountPercent / 100));
    this.emit('cartChanged', this.posCart);
  }

  removeFromCart(batchId) {
    this.posCart = this.posCart.filter(i => i.batchId !== batchId);
    this.emit('cartChanged', this.posCart);
  }

  clearCart() {
    this.posCart = [];
    this.posCustomer = {
      name: '',
      phone: '',
      doctor: '',
      prescriptionNo: '',
      paymentMethod: 'CASH',
      discountPercent: 0
    };
    this.emit('cartChanged', this.posCart);
  }

  getCartTotals() {
    const subtotal = this.posCart.reduce((sum, i) => sum + (i.quantity * i.unitPrice), 0);
    const itemDiscounts = this.posCart.reduce((sum, i) => sum + (i.quantity * i.unitPrice * (i.discountPercent / 100)), 0);
    const afterItemDiscount = subtotal - itemDiscounts;
    const additionalDiscount = afterItemDiscount * ((this.posCustomer.discountPercent || 0) / 100);
    const taxableAmount = afterItemDiscount - additionalDiscount;
    const tax = taxableAmount * 0.05; // 5% GST
    const finalAmount = taxableAmount + tax;

    return {
      subtotal,
      totalDiscount: itemDiscounts + additionalDiscount,
      taxableAmount,
      tax,
      finalAmount
    };
  }
}

window.state = new AppState();
