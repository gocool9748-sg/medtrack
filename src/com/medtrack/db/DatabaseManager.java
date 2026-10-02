package com.medtrack.db;

import com.medtrack.config.AppConfig;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static DatabaseManager instance;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC Driver not found: " + e.getMessage());
        }
    }

    private DatabaseManager() {
        initDatabase();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(AppConfig.DB_URL);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
            stmt.execute("PRAGMA journal_mode = WAL;");
        }
        return conn;
    }

    private void initDatabase() {
        File dataDir = new File(AppConfig.DB_DIR);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Users table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL,
                    password_hash TEXT NOT NULL,
                    salt TEXT NOT NULL,
                    full_name TEXT NOT NULL,
                    email TEXT UNIQUE NOT NULL,
                    role TEXT NOT NULL DEFAULT 'PHARMACIST',
                    is_active INTEGER NOT NULL DEFAULT 1,
                    created_at TEXT NOT NULL,
                    last_login TEXT
                );
            """);

            // Categories table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS categories (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT UNIQUE NOT NULL,
                    description TEXT,
                    icon TEXT,
                    created_at TEXT NOT NULL
                );
            """);

            // Suppliers table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS suppliers (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT UNIQUE NOT NULL,
                    contact_person TEXT,
                    email TEXT,
                    phone TEXT,
                    address TEXT,
                    tax_id TEXT,
                    rating REAL DEFAULT 5.0,
                    created_at TEXT NOT NULL
                );
            """);

            // Medicines table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS medicines (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    generic_name TEXT NOT NULL,
                    category_id INTEGER NOT NULL,
                    dosage_form TEXT NOT NULL,
                    strength TEXT NOT NULL,
                    unit TEXT NOT NULL,
                    reorder_level INTEGER NOT NULL DEFAULT 20,
                    min_alert_days INTEGER NOT NULL DEFAULT 60,
                    requires_prescription INTEGER NOT NULL DEFAULT 0,
                    storage_condition TEXT NOT NULL DEFAULT 'Room Temperature (15-25°C)',
                    side_effects TEXT,
                    barcode TEXT UNIQUE,
                    is_active INTEGER NOT NULL DEFAULT 1,
                    created_at TEXT NOT NULL,
                    FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE RESTRICT
                );
            """);

            // Batches table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS batches (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    medicine_id INTEGER NOT NULL,
                    batch_number TEXT UNIQUE NOT NULL,
                    mfg_date TEXT NOT NULL,
                    expiry_date TEXT NOT NULL,
                    quantity INTEGER NOT NULL DEFAULT 0,
                    original_quantity INTEGER NOT NULL DEFAULT 0,
                    unit_cost REAL NOT NULL DEFAULT 0.0,
                    unit_price REAL NOT NULL DEFAULT 0.0,
                    discount_percent REAL NOT NULL DEFAULT 0.0,
                    shelf_location TEXT NOT NULL DEFAULT 'Main Shelf',
                    supplier_id INTEGER,
                    status TEXT NOT NULL DEFAULT 'ACTIVE',
                    notes TEXT,
                    created_at TEXT NOT NULL,
                    FOREIGN KEY (medicine_id) REFERENCES medicines (id) ON DELETE CASCADE,
                    FOREIGN KEY (supplier_id) REFERENCES suppliers (id) ON DELETE SET NULL
                );
            """);

            // Sales table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS sales (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    invoice_number TEXT UNIQUE NOT NULL,
                    customer_name TEXT NOT NULL,
                    customer_phone TEXT,
                    doctor_name TEXT,
                    prescription_no TEXT,
                    total_amount REAL NOT NULL,
                    discount_amount REAL NOT NULL DEFAULT 0.0,
                    tax_amount REAL NOT NULL DEFAULT 0.0,
                    final_amount REAL NOT NULL,
                    payment_method TEXT NOT NULL DEFAULT 'CASH',
                    cashier_id INTEGER,
                    sale_date TEXT NOT NULL,
                    FOREIGN KEY (cashier_id) REFERENCES users (id) ON DELETE SET NULL
                );
            """);

            // Sale Items table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS sale_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    sale_id INTEGER NOT NULL,
                    medicine_id INTEGER NOT NULL,
                    batch_id INTEGER NOT NULL,
                    quantity INTEGER NOT NULL,
                    unit_price REAL NOT NULL,
                    discount_percent REAL NOT NULL DEFAULT 0.0,
                    subtotal REAL NOT NULL,
                    FOREIGN KEY (sale_id) REFERENCES sales (id) ON DELETE CASCADE,
                    FOREIGN KEY (medicine_id) REFERENCES medicines (id) ON DELETE RESTRICT,
                    FOREIGN KEY (batch_id) REFERENCES batches (id) ON DELETE RESTRICT
                );
            """);

            // Purchase Orders table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS purchase_orders (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    po_number TEXT UNIQUE NOT NULL,
                    supplier_id INTEGER NOT NULL,
                    order_date TEXT NOT NULL,
                    expected_date TEXT,
                    received_date TEXT,
                    total_amount REAL NOT NULL DEFAULT 0.0,
                    status TEXT NOT NULL DEFAULT 'DRAFT',
                    created_by INTEGER,
                    notes TEXT,
                    FOREIGN KEY (supplier_id) REFERENCES suppliers (id) ON DELETE RESTRICT,
                    FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL
                );
            """);

            // Purchase Order Items table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS purchase_order_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    po_id INTEGER NOT NULL,
                    medicine_id INTEGER NOT NULL,
                    quantity INTEGER NOT NULL,
                    unit_cost REAL NOT NULL,
                    subtotal REAL NOT NULL,
                    FOREIGN KEY (po_id) REFERENCES purchase_orders (id) ON DELETE CASCADE,
                    FOREIGN KEY (medicine_id) REFERENCES medicines (id) ON DELETE RESTRICT
                );
            """);

            // Disposals table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS disposals (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    batch_id INTEGER,
                    medicine_id INTEGER NOT NULL,
                    batch_number TEXT NOT NULL,
                    quantity INTEGER NOT NULL,
                    unit_cost REAL NOT NULL DEFAULT 0.0,
                    total_loss REAL NOT NULL DEFAULT 0.0,
                    reason TEXT NOT NULL,
                    destruction_method TEXT NOT NULL,
                    authorized_by INTEGER,
                    disposal_date TEXT NOT NULL,
                    certificate_number TEXT UNIQUE NOT NULL,
                    notes TEXT,
                    FOREIGN KEY (medicine_id) REFERENCES medicines (id) ON DELETE RESTRICT,
                    FOREIGN KEY (authorized_by) REFERENCES users (id) ON DELETE SET NULL
                );
            """);

            // Audit Logs table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS audit_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER,
                    username TEXT NOT NULL,
                    role TEXT,
                    action TEXT NOT NULL,
                    entity_type TEXT NOT NULL,
                    entity_id TEXT,
                    details TEXT,
                    ip_address TEXT,
                    created_at TEXT NOT NULL
                );
            """);

            // Notifications table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS notifications (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    message TEXT NOT NULL,
                    type TEXT NOT NULL DEFAULT 'INFO',
                    severity TEXT NOT NULL DEFAULT 'INFO',
                    is_read INTEGER NOT NULL DEFAULT 0,
                    link_type TEXT,
                    link_id TEXT,
                    created_at TEXT NOT NULL
                );
            """);

            // Settings table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS settings (
                    key TEXT PRIMARY KEY,
                    value TEXT NOT NULL,
                    description TEXT
                );
            """);

            // Indexes for high performance querying
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_batches_expiry ON batches(expiry_date);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_batches_medicine ON batches(medicine_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_medicines_category ON medicines(category_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_medicines_barcode ON medicines(barcode);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_sales_date ON sales(sale_date);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_audit_time ON audit_logs(created_at);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_notif_unread ON notifications(is_read);");

            System.out.println("Database schema initialized successfully.");
        } catch (SQLException e) {
            System.err.println("Database initialization error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
