/**
 * MedTrack REST API Client
 */
class ApiClient {
  constructor() {
    this.baseUrl = '/api';
  }

  getToken() {
    return localStorage.getItem('medtrack_token');
  }

  setToken(token) {
    if (token) localStorage.setItem('medtrack_token', token);
    else localStorage.removeItem('medtrack_token');
  }

  async request(endpoint, options = {}) {
    const url = `${this.baseUrl}${endpoint}`;
    const headers = {
      'Content-Type': 'application/json',
      ...(options.headers || {})
    };

    const token = this.getToken();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    try {
      const response = await fetch(url, {
        ...options,
        headers
      });

      // If unauthorized, trigger login modal
      if (response.status === 401) {
        this.setToken(null);
        if (window.state) window.state.setCurrentUser(null);
        if (window.router) window.router.navigate('#login');
        throw new Error('Session expired or unauthorized. Please log in.');
      }

      const json = await response.json();
      if (!response.ok || !json.success) {
        throw new Error(json.error || json.message || 'Request failed.');
      }

      return json.data !== undefined ? json.data : json;
    } catch (err) {
      console.error(`API Error [${endpoint}]:`, err);
      throw err;
    }
  }

  // Auth Endpoints
  login(username, password) {
    return this.request('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password })
    });
  }

  getMe() {
    return this.request('/auth/me');
  }

  logout() {
    return this.request('/auth/logout', { method: 'POST' });
  }

  getUsers() {
    return this.request('/auth/users');
  }

  createUser(user) {
    return this.request('/auth/users', {
      method: 'POST',
      body: JSON.stringify(user)
    });
  }

  // Medicines Endpoints
  getMedicines(params = {}) {
    const q = new URLSearchParams(params).toString();
    return this.request(`/medicines${q ? '?' + q : ''}`);
  }

  getMedicineById(id) {
    return this.request(`/medicines/${id}`);
  }

  getMedicineByBarcode(barcode) {
    return this.request(`/medicines/barcode/${barcode}`);
  }

  createMedicine(data) {
    return this.request('/medicines', {
      method: 'POST',
      body: JSON.stringify(data)
    });
  }

  updateMedicine(id, data) {
    return this.request(`/medicines/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data)
    });
  }

  deleteMedicine(id) {
    return this.request(`/medicines/${id}`, { method: 'DELETE' });
  }

  // Batches Endpoints
  getBatches(params = {}) {
    const q = new URLSearchParams(params).toString();
    return this.request(`/batches${q ? '?' + q : ''}`);
  }

  getFEFOBatches(medicineId) {
    return this.request(`/batches/fefo/${medicineId}`);
  }

  getBatchById(id) {
    return this.request(`/batches/${id}`);
  }

  createBatch(data) {
    return this.request('/batches', {
      method: 'POST',
      body: JSON.stringify(data)
    });
  }

  updateBatch(id, data) {
    return this.request(`/batches/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data)
    });
  }

  quarantineBatch(id, reason) {
    return this.request(`/batches/${id}/quarantine`, {
      method: 'POST',
      body: JSON.stringify({ reason })
    });
  }

  discountBatch(id, discountPercent) {
    return this.request(`/batches/${id}/discount`, {
      method: 'PUT',
      body: JSON.stringify({ discountPercent })
    });
  }

  adjustBatchStock(id, quantity, reason) {
    return this.request(`/batches/${id}/adjust`, {
      method: 'PUT',
      body: JSON.stringify({ quantity, reason })
    });
  }

  deleteBatch(id) {
    return this.request(`/batches/${id}`, { method: 'DELETE' });
  }

  // Categories Endpoints
  getCategories() {
    return this.request('/categories');
  }

  createCategory(data) {
    return this.request('/categories', {
      method: 'POST',
      body: JSON.stringify(data)
    });
  }

  updateCategory(id, data) {
    return this.request(`/categories/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data)
    });
  }

  deleteCategory(id) {
    return this.request(`/categories/${id}`, { method: 'DELETE' });
  }

  // Suppliers Endpoints
  getSuppliers() {
    return this.request('/suppliers');
  }

  createSupplier(data) {
    return this.request('/suppliers', {
      method: 'POST',
      body: JSON.stringify(data)
    });
  }

  updateSupplier(id, data) {
    return this.request(`/suppliers/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data)
    });
  }

  deleteSupplier(id) {
    return this.request(`/suppliers/${id}`, { method: 'DELETE' });
  }

  // Sales / POS Endpoints
  getSales(params = {}) {
    const q = new URLSearchParams(params).toString();
    return this.request(`/sales${q ? '?' + q : ''}`);
  }

  getSaleById(id) {
    return this.request(`/sales/${id}`);
  }

  getSaleByInvoice(invoiceNo) {
    return this.request(`/sales/invoice/${invoiceNo}`);
  }

  createSale(saleData) {
    return this.request('/sales', {
      method: 'POST',
      body: JSON.stringify(saleData)
    });
  }

  // Purchase Orders Endpoints
  getOrders(status) {
    const q = status ? `?status=${status}` : '';
    return this.request(`/orders${q}`);
  }

  getOrderById(id) {
    return this.request(`/orders/${id}`);
  }

  createOrder(data) {
    return this.request('/orders', {
      method: 'POST',
      body: JSON.stringify(data)
    });
  }

  updateOrderStatus(id, status) {
    return this.request(`/orders/${id}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status })
    });
  }

  // Disposals Endpoints
  getDisposals(params = {}) {
    const q = new URLSearchParams(params).toString();
    return this.request(`/disposals${q ? '?' + q : ''}`);
  }

  createDisposal(data) {
    return this.request('/disposals', {
      method: 'POST',
      body: JSON.stringify(data)
    });
  }

  // Expiry Intelligence Endpoints
  getExpiryRecommendations() {
    return this.request('/expiry/recommendations');
  }

  runExpiryScan() {
    return this.request('/expiry/scan', { method: 'POST' });
  }

  applyMarkdown(batchId, discountPercent) {
    return this.request('/expiry/markdown', {
      method: 'POST',
      body: JSON.stringify({ batchId, discountPercent })
    });
  }

  // Analytics Endpoints
  getDashboardStats() {
    return this.request('/analytics/dashboard');
  }

  getCategoryAnalytics() {
    return this.request('/analytics/categories');
  }

  getTopMedicines(limit = 10) {
    return this.request(`/analytics/top-medicines?limit=${limit}`);
  }

  getFinancialTrends() {
    return this.request('/analytics/financials');
  }

  // Audit Logs Endpoints
  getAuditLogs(params = {}) {
    const q = new URLSearchParams(params).toString();
    return this.request(`/audit${q ? '?' + q : ''}`);
  }

  // Notifications Endpoints
  getNotifications(unreadOnly = false) {
    return this.request(`/notifications?unread=${unreadOnly}`);
  }

  markNotificationRead(id) {
    return this.request(`/notifications/${id}/read`, { method: 'PUT' });
  }

  markAllNotificationsRead() {
    return this.request('/notifications/read-all', { method: 'PUT' });
  }

  // Reset / Backup
  resetDatabase() {
    return this.request('/backup/reset', { method: 'POST' });
  }
}

window.api = new ApiClient();
