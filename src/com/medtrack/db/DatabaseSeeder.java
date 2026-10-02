package com.medtrack.db;

import com.medtrack.util.DateUtils;
import com.medtrack.util.PasswordUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;

public class DatabaseSeeder {

    public static void seedIfNeeded() {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            // Check if users already exist
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users;")) {
                if (rs.next() && rs.getInt(1) > 0) {
                    System.out.println("Database already seeded. Skipping initial seeding.");
                    return;
                }
            }

            System.out.println("Seeding database with comprehensive healthcare records...");

            // 1. Seed Users
            String insertUserSql = "INSERT INTO users (username, password_hash, salt, full_name, email, role, is_active, created_at, last_login) VALUES (?, ?, ?, ?, ?, ?, 1, ?, ?);";
            try (PreparedStatement ps = conn.prepareStatement(insertUserSql)) {
                String now = DateUtils.now();
                
                // Admin: admin / Admin@123
                String salt1 = PasswordUtils.generateSalt();
                ps.setString(1, "admin");
                ps.setString(2, PasswordUtils.hashPassword("Admin@123", salt1));
                ps.setString(3, salt1);
                ps.setString(4, "Dr. Sarah Jenkins (Chief Administrator)");
                ps.setString(5, "admin@apexmedtrack.io");
                ps.setString(6, "ADMIN");
                ps.setString(7, now);
                ps.setString(8, now);
                ps.addBatch();

                // Pharmacist: pharmacist / Pharm@123
                String salt2 = PasswordUtils.generateSalt();
                ps.setString(1, "pharmacist");
                ps.setString(2, PasswordUtils.hashPassword("Pharm@123", salt2));
                ps.setString(3, salt2);
                ps.setString(4, "Alex Vance, PharmD");
                ps.setString(5, "alex.vance@apexmedtrack.io");
                ps.setString(6, "PHARMACIST");
                ps.setString(7, now);
                ps.setString(8, now);
                ps.addBatch();

                // Inventory Manager: manager / Manager@123
                String salt3 = PasswordUtils.generateSalt();
                ps.setString(1, "manager");
                ps.setString(2, PasswordUtils.hashPassword("Manager@123", salt3));
                ps.setString(3, salt3);
                ps.setString(4, "Marcus Brody (Logistics Lead)");
                ps.setString(5, "marcus.brody@apexmedtrack.io");
                ps.setString(6, "INVENTORY_MANAGER");
                ps.setString(7, now);
                ps.setString(8, null);
                ps.addBatch();

                // Auditor: auditor / Audit@123
                String salt4 = PasswordUtils.generateSalt();
                ps.setString(1, "auditor");
                ps.setString(2, PasswordUtils.hashPassword("Audit@123", salt4));
                ps.setString(3, salt4);
                ps.setString(4, "Elena Rostova (Compliance Inspector)");
                ps.setString(5, "elena.rostova@apexmedtrack.io");
                ps.setString(6, "AUDITOR");
                ps.setString(7, now);
                ps.setString(8, null);
                ps.addBatch();

                ps.executeBatch();
            }

            // 2. Seed Categories
            String insertCatSql = "INSERT INTO categories (name, description, icon, created_at) VALUES (?, ?, ?, ?);";
            try (PreparedStatement ps = conn.prepareStatement(insertCatSql)) {
                String now = DateUtils.now();
                String[][] categories = {
                    {"Antibiotics & Antimicrobials", "Broad-spectrum and targeted antibacterial drugs", "pill", now},
                    {"Analgesics & Pain Relief", "NSAIDs, antipyretics, and pain management medications", "shield-plus", now},
                    {"Cardiovascular & Hypertension", "Heart health, blood pressure, and cholesterol management", "heart-pulse", now},
                    {"Antidiabetic & Endocrinology", "Insulin, GLP-1, and oral hypoglycemic agents", "activity", now},
                    {"Respiratory & Pulmonology", "Inhalers, bronchodilators, and allergy relief", "wind", now},
                    {"Gastrointestinal & Digestive", "Antacids, proton-pump inhibitors, and anti-emetics", "droplet", now},
                    {"Central Nervous System", "Sedatives, antidepressants, antiepileptics", "brain", now},
                    {"Dermatologicals & Topicals", "Ointments, antifungals, and corticosteroid creams", "sparkles", now},
                    {"Ophthalmic & ENT", "Eye drops, ear sprays, and nasal decongestants", "eye", now},
                    {"Vitamins & Nutritional", "Essential multivitamins, minerals, and supplements", "sun", now}
                };
                for (String[] cat : categories) {
                    ps.setString(1, cat[0]);
                    ps.setString(2, cat[1]);
                    ps.setString(3, cat[2]);
                    ps.setString(4, cat[3]);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            // 3. Seed Suppliers
            String insertSuppSql = "INSERT INTO suppliers (name, contact_person, email, phone, address, tax_id, rating, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
            try (PreparedStatement ps = conn.prepareStatement(insertSuppSql)) {
                String now = DateUtils.now();
                Object[][] suppliers = {
                    {"Novartis Health Global", "Robert Langdon", "orders@novartis-supply.com", "+1-800-555-0199", "100 Technology Square, Cambridge, MA", "US-EIN-9482710", 4.9, now},
                    {"Pfizer Distribution Corp", "David Kim", "pharma.logistics@pfizer-corp.com", "+1-800-555-0245", "66 Hudson Blvd East, New York, NY", "US-EIN-1324567", 4.8, now},
                    {"GSK Biologicals Supply", "Sophie Chen", "direct@gsk-healthlink.com", "+44-20-7981-4321", "980 Great West Road, Brentford, UK", "GB-VAT-982147", 4.7, now},
                    {"Sun Pharma Lifecare", "Ramesh Mehta", "b2b@sunpharma-care.com", "+91-22-6789-0123", "Goregaon East, Mumbai, Maharashtra", "36AABCS1423M1Z2", 4.6, now},
                    {"Sanofi Aventis Logistics", "Jean-Luc Picard", "orders@sanofi-supply.fr", "+33-1-5377-4000", "54 Rue La Boétie, Paris, France", "FR-VAT-3301928", 4.9, now},
                    {"Cipla Quality Generics", "Ananya Deshmukh", "institutional@cipla-dist.com", "+91-22-2482-6000", "Peninsula Business Park, Lower Parel, Mumbai", "27AAACC1234N1ZT", 4.8, now}
                };
                for (Object[] supp : suppliers) {
                    ps.setString(1, (String) supp[0]);
                    ps.setString(2, (String) supp[1]);
                    ps.setString(3, (String) supp[2]);
                    ps.setString(4, (String) supp[3]);
                    ps.setString(5, (String) supp[4]);
                    ps.setString(6, (String) supp[5]);
                    ps.setDouble(7, (Double) supp[6]);
                    ps.setString(8, (String) supp[7]);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            // 4. Seed Medicines
            String insertMedSql = "INSERT INTO medicines (name, generic_name, category_id, dosage_form, strength, unit, reorder_level, min_alert_days, requires_prescription, storage_condition, side_effects, barcode, is_active, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?);";
            try (PreparedStatement ps = conn.prepareStatement(insertMedSql)) {
                String now = DateUtils.now();
                Object[][] medicines = {
                    // Antibiotics (cat 1)
                    {"Amoxil 500", "Amoxicillin Trihydrate", 1, "Capsule", "500mg", "Strip (10 Caps)", 30, 60, 1, "Room Temperature (15-25°C)", "Nausea, mild rash, stomach upset", "890103001001", now},
                    {"Augmentin 625 Duo", "Amoxicillin + Clavulanic Acid", 1, "Tablet", "625mg", "Strip (10 Tabs)", 40, 60, 1, "Cool & Dry Place (<25°C)", "Diarrhea, skin rash", "890103001002", now},
                    {"Zithromax 500", "Azithromycin", 1, "Tablet", "500mg", "Strip (3 Tabs)", 25, 45, 1, "Room Temperature (15-25°C)", "Headache, dizziness", "890103001003", now},
                    {"Cipro 500", "Ciprofloxacin", 1, "Tablet", "500mg", "Strip (10 Tabs)", 20, 60, 1, "Room Temperature (15-25°C)", "Tendonitis risk, insomnia", "890103001004", now},

                    // Analgesics (cat 2)
                    {"Tylenol Extra Strength", "Paracetamol / Acetaminophen", 2, "Tablet", "650mg", "Strip (15 Tabs)", 50, 90, 0, "Room Temperature (15-25°C)", "Rare liver toxicity on overdose", "890103002001", now},
                    {"Brufen 400", "Ibuprofen", 2, "Tablet", "400mg", "Strip (15 Tabs)", 35, 60, 0, "Room Temperature (15-25°C)", "Heartburn, dyspepsia", "890103002002", now},
                    {"Voveran SR 100", "Diclofenac Sodium Sustained Release", 2, "Tablet", "100mg", "Strip (10 Tabs)", 20, 60, 1, "Room Temperature (15-25°C)", "Gastric irritation", "890103002003", now},
                    {"Tramacet", "Tramadol + Paracetamol", 2, "Tablet", "37.5mg/325mg", "Strip (10 Tabs)", 15, 60, 1, "Room Temperature (15-25°C)", "Drowsiness, constipation", "890103002004", now},

                    // Cardiovascular (cat 3)
                    {"Lipitor 20", "Atorvastatin Calcium", 3, "Tablet", "20mg", "Strip (15 Tabs)", 40, 90, 1, "Room Temperature (15-25°C)", "Muscle fatigue, mild headache", "890103003001", now},
                    {"Norvasc 5", "Amlodipine Besylate", 3, "Tablet", "5mg", "Strip (14 Tabs)", 30, 90, 1, "Room Temperature (15-25°C)", "Peripheral edema, flushing", "890103003002", now},
                    {"Telma 40", "Telmisartan", 3, "Tablet", "40mg", "Strip (15 Tabs)", 35, 90, 1, "Room Temperature (15-25°C)", "Back pain, sinus congestion", "890103003003", now},
                    {"Ecosprin 75", "Aspirin Enteric Coated", 3, "Tablet", "75mg", "Strip (14 Tabs)", 60, 90, 0, "Cool & Dry Place (<25°C)", "Bleeding tendency", "890103003004", now},

                    // Antidiabetic (cat 4)
                    {"Glucophage XR 500", "Metformin Hydrochloride Extended Release", 4, "Tablet", "500mg", "Strip (15 Tabs)", 50, 90, 1, "Room Temperature (15-25°C)", "Metallic taste, GI flatulence", "890103004001", now},
                    {"Januvia 100", "Sitagliptin Phosphate", 4, "Tablet", "100mg", "Strip (14 Tabs)", 25, 60, 1, "Room Temperature (15-25°C)", "Upper respiratory tract infection", "890103004002", now},
                    {"Lantus Solostar", "Insulin Glargine rDNA", 4, "Injection", "100 IU/ml", "Box (5 Pens)", 10, 30, 1, "Refrigerated (2-8°C)", "Hypoglycemia, injection site redness", "890103004003", now},
                    {"Rybelsus 7", "Semaglutide", 4, "Tablet", "7mg", "Strip (10 Tabs)", 15, 45, 1, "Room Temperature (15-25°C)", "Nausea, reduced appetite", "890103004004", now},

                    // Respiratory (cat 5)
                    {"Ventolin HFA", "Salbutamol / Albuterol Inhaler", 5, "Inhaler", "100mcg/puff", "Canister (200 Puffs)", 20, 60, 1, "Cool & Dry Place (<25°C)", "Tremor, palpitations", "890103005001", now},
                    {"Seretide Accuhaler", "Fluticasone + Salmeterol", 5, "Inhaler", "50mcg/250mcg", "Diskus Device (60 Doses)", 15, 60, 1, "Room Temperature (15-25°C)", "Hoarseness, oral candidiasis", "890103005002", now},
                    {"Allegra 120", "Fexofenadine HCl", 5, "Tablet", "120mg", "Strip (10 Tabs)", 40, 60, 0, "Room Temperature (15-25°C)", "Drowsiness (rare), dry mouth", "890103005003", now},
                    {"Ascoril D Plus", "Dextromethorphan + Phenylephrine + Chlorpheniramine", 5, "Syrup", "100ml", "Bottle (100ml)", 30, 60, 0, "Room Temperature (15-25°C)", "Somnolence", "890103005004", now},

                    // Gastrointestinal (cat 6)
                    {"Nexium 40", "Esomeprazole Magnesium", 6, "Tablet", "40mg", "Strip (15 Tabs)", 45, 90, 1, "Room Temperature (15-25°C)", "Headache, abdominal pain", "890103006001", now},
                    {"Pan 40", "Pantoprazole Sodium", 6, "Tablet", "40mg", "Strip (15 Tabs)", 50, 90, 1, "Room Temperature (15-25°C)", "Flatulence, dizziness", "890103006002", now},
                    {"Duphalac Syrup", "Lactulose Solution", 6, "Syrup", "3.335g/5ml", "Bottle (200ml)", 20, 60, 0, "Room Temperature (15-25°C)", "Abdominal cramps", "890103006003", now},

                    // Central Nervous System (cat 7)
                    {"Lexapro 10", "Escitalopram Oxalate", 7, "Tablet", "10mg", "Strip (14 Tabs)", 25, 90, 1, "Room Temperature (15-25°C)", "Insomnia, nausea, fatigue", "890103007001", now},
                    {"Alprax 0.5", "Alprazolam", 7, "Tablet", "0.5mg", "Strip (15 Tabs)", 15, 45, 1, "Room Temperature (15-25°C)", "Drowsiness, dependence with prolonged use", "890103007002", now},

                    // Topicals (cat 8)
                    {"Betnovate-N Cream", "Betamethasone + Neomycin", 8, "Ointment", "20g", "Tube (20g)", 30, 60, 1, "Cool & Dry Place (<25°C)", "Skin thinning on overuse", "890103008001", now},
                    {"Candid-B Cream", "Clotrimazole + Beclomethasone", 8, "Ointment", "30g", "Tube (30g)", 25, 60, 0, "Room Temperature (15-25°C)", "Local burning sensation", "890103008002", now},

                    // Ophthalmic (cat 9)
                    {"Ciplox Eye Drops", "Ciprofloxacin Ophthalmic 0.3%", 9, "Drops", "10ml", "Vial (10ml)", 35, 45, 1, "Room Temperature (15-25°C)", "Mild stinging on application", "890103009001", now},
                    {"Refresh Tears", "Carboxymethylcellulose Sodium 0.5%", 9, "Drops", "10ml", "Vial (10ml)", 40, 60, 0, "Room Temperature (15-25°C)", "Temporary blurriness", "890103009002", now},

                    // Vitamins (cat 10)
                    {"Becadexamin Softgels", "Multivitamin & Multimineral", 10, "Capsule", "High Potency", "Bottle (30 Softgels)", 50, 90, 0, "Cool & Dry Place (<25°C)", "Mild nausea if taken without food", "890103010001", now},
                    {"Limcee 500 Chewable", "Vitamin C (Ascorbic Acid)", 10, "Tablet", "500mg", "Strip (15 Tabs)", 60, 90, 0, "Room Temperature (15-25°C)", "None reported at recommended dose", "890103010002", now},
                    {"Shelcal 500", "Calcium Carbonate + Vitamin D3", 10, "Tablet", "500mg/250IU", "Strip (15 Tabs)", 45, 90, 0, "Room Temperature (15-25°C)", "Constipation", "890103010003", now}
                };

                for (Object[] med : medicines) {
                    ps.setString(1, (String) med[0]);
                    ps.setString(2, (String) med[1]);
                    ps.setInt(3, (Integer) med[2]);
                    ps.setString(4, (String) med[3]);
                    ps.setString(5, (String) med[4]);
                    ps.setString(6, (String) med[5]);
                    ps.setInt(7, (Integer) med[6]);
                    ps.setInt(8, (Integer) med[7]);
                    ps.setInt(9, (Integer) med[8]);
                    ps.setString(10, (String) med[9]);
                    ps.setString(11, (String) med[10]);
                    ps.setString(12, (String) med[11]);
                    ps.setString(13, (String) med[12]);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            // 5. Seed Batches (with dynamic dates based on today to guarantee realistic SAFE, WARNING, CRITICAL, and EXPIRED items)
            String insertBatchSql = "INSERT INTO batches (medicine_id, batch_number, mfg_date, expiry_date, quantity, original_quantity, unit_cost, unit_price, discount_percent, shelf_location, supplier_id, status, notes, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
            try (PreparedStatement ps = conn.prepareStatement(insertBatchSql)) {
                LocalDate today = LocalDate.now();
                String now = DateUtils.now();

                // Format: medicine_id, batch_num, mfg_offset_months, expiry_offset_days, qty, orig_qty, cost, price, discount, shelf, supp_id, notes
                Object[][] batchSpecs = {
                    // Safe long expiry (6-18 months)
                    {1, "AMX-2026-001", -6, 420, 120, 150, 4.50, 8.50, 0.0, "Shelf A-1", 1, "Primary production run"},
                    {1, "AMX-2026-002", -1, 580, 80, 80, 4.60, 8.50, 0.0, "Shelf A-1", 1, "Fresh restock"},
                    {2, "AUG-2026-101", -4, 360, 95, 100, 12.00, 22.00, 0.0, "Shelf A-2", 3, "High-demand antibiotic"},
                    {3, "ZTH-2026-201", -3, 300, 60, 60, 8.50, 16.00, 0.0, "Shelf A-3", 2, "Standard stock"},
                    {4, "CPR-2026-301", -5, 280, 45, 50, 5.20, 10.50, 0.0, "Shelf A-4", 6, "Standard batch"},
                    {5, "TYL-2026-401", -2, 700, 300, 300, 2.10, 4.99, 0.0, "Shelf B-1", 2, "Fast moving bulk"},
                    {6, "BRU-2026-501", -4, 450, 110, 120, 3.00, 6.50, 0.0, "Shelf B-2", 4, "Anti-inflammatory run"},
                    {7, "VOV-2026-601", -6, 320, 70, 80, 4.80, 9.75, 0.0, "Shelf B-3", 1, "SR release stock"},
                    {8, "TRM-2026-701", -3, 380, 40, 40, 7.50, 15.00, 0.0, "Vault Sec-1", 5, "Controlled schedule drug"},
                    {9, "LIP-2026-801", -2, 500, 140, 150, 9.00, 18.50, 0.0, "Shelf C-1", 2, "Cardiovascular staple"},
                    {10, "NOR-2026-901", -4, 460, 85, 100, 4.20, 8.90, 0.0, "Shelf C-2", 2, "Standard hypertension pack"},
                    {11, "TEL-2026-011", -3, 490, 90, 100, 6.50, 13.00, 0.0, "Shelf C-3", 4, "High turnover"},
                    {12, "ECO-2026-012", -5, 600, 220, 250, 1.20, 3.25, 0.0, "Shelf C-4", 4, "Low dose cardioprotective"},
                    {13, "GLU-2026-013", -2, 550, 180, 200, 3.80, 8.00, 0.0, "Shelf D-1", 5, "Type-2 diabetes mainstay"},
                    {14, "JAN-2026-014", -4, 400, 50, 50, 18.00, 34.00, 0.0, "Shelf D-2", 1, "DPP-4 inhibitor"},
                    {15, "LAN-2026-015", -1, 240, 25, 30, 45.00, 85.00, 0.0, "Cold Unit Alpha (4°C)", 5, "Cold-chain insulin pens"},
                    {16, "RYB-2026-016", -2, 350, 35, 40, 60.00, 110.00, 0.0, "Shelf D-3", 1, "Oral GLP-1 peptide"},
                    {17, "VEN-2026-017", -4, 480, 65, 75, 7.00, 14.50, 0.0, "Shelf E-1", 3, "Metered inhaler"},
                    {18, "SER-2026-018", -3, 420, 30, 35, 28.00, 52.00, 0.0, "Shelf E-2", 3, "Accuhaler combo"},
                    {19, "ALL-2026-019", -5, 520, 130, 150, 5.00, 11.25, 0.0, "Shelf E-3", 5, "Non-sedating antihistamine"},
                    {20, "ASC-2026-020", -3, 380, 75, 90, 3.50, 7.80, 0.0, "Shelf E-4", 4, "Cough formula"},
                    {21, "NEX-2026-021", -2, 450, 120, 130, 11.00, 24.00, 0.0, "Shelf F-1", 2, "PPI therapy"},
                    {22, "PAN-2026-022", -4, 480, 160, 180, 4.00, 9.00, 0.0, "Shelf F-2", 6, "Standard gastro"},
                    {23, "DUP-2026-023", -3, 360, 45, 50, 6.00, 12.50, 0.0, "Shelf F-3", 4, "Lactulose syrup"},
                    {24, "LEX-2026-024", -5, 400, 55, 60, 14.00, 28.00, 0.0, "Vault Sec-2", 1, "SSRI antidepressant"},
                    {25, "ALP-2026-025", -2, 320, 40, 45, 3.50, 8.00, 0.0, "Vault Sec-1", 4, "Controlled benzodiazepine"},
                    {26, "BET-2026-026", -4, 410, 80, 90, 2.50, 5.75, 0.0, "Shelf G-1", 3, "Topical corticosteroid"},
                    {27, "CAN-2026-027", -3, 430, 70, 80, 3.20, 7.20, 0.0, "Shelf G-2", 3, "Antifungal topical"},
                    {28, "CIP-2026-028", -2, 280, 85, 100, 2.00, 4.50, 0.0, "Shelf H-1", 6, "Ophthalmic sterile drops"},
                    {29, "REF-2026-029", -4, 340, 90, 100, 4.50, 9.90, 0.0, "Shelf H-2", 2, "Lubricating eye drops"},
                    {30, "BEC-2026-030", -5, 500, 140, 150, 3.50, 7.99, 0.0, "Shelf I-1", 3, "Multivitamin bottle"},
                    {31, "LIM-2026-031", -2, 600, 200, 200, 1.80, 4.20, 0.0, "Shelf I-2", 6, "Vitamin C chewables"},
                    {32, "SHE-2026-032", -4, 480, 150, 160, 4.50, 9.50, 0.0, "Shelf I-3", 6, "Calcium supplement"},

                    // WARNING Near-Expiry Batches (Expiring in 35-85 days -> Yellow alert)
                    {1, "AMX-WARN-098", -18, 55, 35, 120, 4.50, 8.50, 15.0, "Shelf A-1 (Promo)", 1, "Suggested early discount applied (15%)"},
                    {2, "AUG-WARN-099", -16, 42, 22, 100, 12.00, 22.00, 20.0, "Shelf A-2 (Promo)", 3, "FEFO Priority alert: Expiring in 6 weeks"},
                    {9, "LIP-WARN-102", -20, 70, 40, 150, 9.00, 18.50, 10.0, "Shelf C-1 (Promo)", 2, "Active promotional markdown"},
                    {13, "GLU-WARN-105", -19, 65, 50, 200, 3.80, 8.00, 15.0, "Shelf D-1 (Promo)", 5, "Promotional stock markdown"},
                    {15, "LAN-WARN-108", -10, 45, 8, 30, 45.00, 85.00, 25.0, "Cold Unit Alpha (4°C)", 5, "Cold-chain near-expiry, urgent dispensing recommended"},
                    {19, "ALL-WARN-110", -18, 78, 30, 150, 5.00, 11.25, 10.0, "Shelf E-3 (Promo)", 5, "Early seasonal clearance"},

                    // CRITICAL Near-Expiry Batches (Expiring in 5-25 days -> Orange alert)
                    {3, "ZTH-CRIT-201", -22, 12, 18, 80, 8.50, 16.00, 35.0, "Dispensary Counter 1", 2, "CRITICAL: 12 days left! 35% clearance markdown"},
                    {5, "TYL-CRIT-202", -23, 18, 45, 200, 2.10, 4.99, 40.0, "Dispensary Counter 2", 2, "CRITICAL: 18 days left! 40% clearance discount"},
                    {17, "VEN-CRIT-203", -21, 8, 12, 50, 7.00, 14.50, 50.0, "Dispensary Counter 1", 3, "CRITICAL: 8 days remaining! Urgent flash sale / supplier return"},
                    {21, "NEX-CRIT-204", -22, 22, 20, 100, 11.00, 24.00, 30.0, "Dispensary Counter 3", 2, "CRITICAL: 22 days left. Alert sent to pharmacist"},

                    // EXPIRED Batches (Past expiry date -> Red alert, ready for Quarantine & Disposal demonstration)
                    {2, "AUG-EXP-088", -26, -14, 15, 100, 12.00, 22.00, 0.0, "Quarantine Bay Q-1", 3, "EXPIRED 14 days ago. Quarantined for destruction."},
                    {7, "VOV-EXP-089", -28, -35, 25, 80, 4.80, 9.75, 0.0, "Quarantine Bay Q-1", 1, "EXPIRED 35 days ago. Pending bio-waste incineration."},
                    {20, "ASC-EXP-090", -24, -8, 10, 100, 3.50, 7.80, 0.0, "Quarantine Bay Q-2", 4, "EXPIRED 8 days ago. Cold chain violation."},
                    {28, "CIP-EXP-091", -20, -45, 30, 100, 2.00, 4.50, 0.0, "Quarantine Bay Q-2", 6, "EXPIRED 45 days ago. Ready for safe chemical disposal."}
                };

                for (Object[] b : batchSpecs) {
                    int medId = (Integer) b[0];
                    String batchNum = (String) b[1];
                    int mfgOffset = (Integer) b[2];
                    int expOffset = (Integer) b[3];
                    int qty = (Integer) b[4];
                    int origQty = (Integer) b[5];
                    double cost = (Double) b[6];
                    double price = (Double) b[7];
                    double discount = (Double) b[8];
                    String shelf = (String) b[9];
                    int suppId = (Integer) b[10];
                    String notes = (String) b[11];

                    LocalDate mfg = today.plusMonths(mfgOffset);
                    LocalDate exp = today.plusDays(expOffset);

                    String status = "ACTIVE";
                    if (expOffset < 0) {
                        status = "EXPIRED";
                    } else if (expOffset <= 30) {
                        status = "EXPIRING_SOON";
                    }

                    ps.setInt(1, medId);
                    ps.setString(2, batchNum);
                    ps.setString(3, mfg.format(DateUtils.DATE_FORMATTER));
                    ps.setString(4, exp.format(DateUtils.DATE_FORMATTER));
                    ps.setInt(5, qty);
                    ps.setInt(6, origQty);
                    ps.setDouble(7, cost);
                    ps.setDouble(8, price);
                    ps.setDouble(9, discount);
                    ps.setString(10, shelf);
                    ps.setInt(11, suppId);
                    ps.setString(12, status);
                    ps.setString(13, notes);
                    ps.setString(14, now);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            // 6. Seed Past Sales & Transactions
            String insertSaleSql = "INSERT INTO sales (invoice_number, customer_name, customer_phone, doctor_name, prescription_no, total_amount, discount_amount, tax_amount, final_amount, payment_method, cashier_id, sale_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 2, ?);";
            String insertSaleItemSql = "INSERT INTO sale_items (sale_id, medicine_id, batch_id, quantity, unit_price, discount_percent, subtotal) VALUES (?, ?, ?, ?, ?, ?, ?);";

            LocalDate today = LocalDate.now();
            Object[][] salesData = {
                {"INV-2026-1001", "Jonathan Miller", "+1-555-0142", "Dr. Emily Roberts", "RX-78921", 52.50, 5.00, 2.38, 49.88, "CARD", today.minusDays(5).format(DateUtils.DATE_FORMATTER) + " 09:42:15"},
                {"INV-2026-1002", "Priya Sharma", "+91-98765-11223", "Dr. R. K. Gupta", "RX-78922", 34.00, 0.00, 1.70, 35.70, "UPI", today.minusDays(4).format(DateUtils.DATE_FORMATTER) + " 11:15:30"},
                {"INV-2026-1003", "Michael Thorne", "+1-555-0189", "Dr. Emily Roberts", "RX-78923", 112.50, 10.00, 5.13, 107.63, "CASH", today.minusDays(3).format(DateUtils.DATE_FORMATTER) + " 14:20:00"},
                {"INV-2026-1004", "Sophia Chen", "+1-555-0211", "Dr. Alan Grant", "RX-78924", 78.00, 8.00, 3.50, 73.50, "INSURANCE", today.minusDays(2).format(DateUtils.DATE_FORMATTER) + " 16:05:45"},
                {"INV-2026-1005", "David Kumar", "+91-98444-55667", "Dr. R. K. Gupta", "RX-78925", 42.00, 0.00, 2.10, 44.10, "UPI", today.minusDays(1).format(DateUtils.DATE_FORMATTER) + " 10:30:10"},
                {"INV-2026-1006", "Emma Watson", "+1-555-0333", "Dr. Gregory House", "RX-78926", 95.00, 12.00, 4.15, 87.15, "CARD", today.format(DateUtils.DATE_FORMATTER) + " 09:12:00"},
                {"INV-2026-1007", "Liam O'Connor", "+1-555-0444", "Dr. Gregory House", "RX-78927", 26.50, 0.00, 1.33, 27.83, "CASH", today.format(DateUtils.DATE_FORMATTER) + " 11:45:22"}
            };

            for (Object[] s : salesData) {
                try (PreparedStatement ps = conn.prepareStatement(insertSaleSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, (String) s[0]);
                    ps.setString(2, (String) s[1]);
                    ps.setString(3, (String) s[2]);
                    ps.setString(4, (String) s[3]);
                    ps.setString(5, (String) s[4]);
                    ps.setDouble(6, (Double) s[5]);
                    ps.setDouble(7, (Double) s[6]);
                    ps.setDouble(8, (Double) s[7]);
                    ps.setDouble(9, (Double) s[8]);
                    ps.setString(10, (String) s[9]);
                    ps.setString(11, (String) s[10]);
                    ps.executeUpdate();

                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            int saleId = rs.getInt(1);
                            // Add 2 items per sale
                            try (PreparedStatement psItem = conn.prepareStatement(insertSaleItemSql)) {
                                psItem.setInt(1, saleId);
                                psItem.setInt(2, 1); // Amoxil
                                psItem.setInt(3, 1); // Batch 1
                                psItem.setInt(4, 2);
                                psItem.setDouble(5, 8.50);
                                psItem.setDouble(6, 0.0);
                                psItem.setDouble(7, 17.00);
                                psItem.addBatch();

                                psItem.setInt(1, saleId);
                                psItem.setInt(2, 5); // Tylenol
                                psItem.setInt(3, 6); // Batch 6
                                psItem.setInt(4, 3);
                                psItem.setDouble(5, 4.99);
                                psItem.setDouble(6, 0.0);
                                psItem.setDouble(7, 14.97);
                                psItem.addBatch();

                                psItem.executeBatch();
                            }
                        }
                    }
                }
            }

            // 7. Seed Purchase Orders
            String insertPOSql = "INSERT INTO purchase_orders (po_number, supplier_id, order_date, expected_date, received_date, total_amount, status, created_by, notes) VALUES (?, ?, ?, ?, ?, ?, ?, 3, ?);";
            try (PreparedStatement ps = conn.prepareStatement(insertPOSql)) {
                ps.setString(1, "PO-2026-0891");
                ps.setInt(2, 1); // Novartis
                ps.setString(3, today.minusDays(15).format(DateUtils.DATE_FORMATTER));
                ps.setString(4, today.minusDays(8).format(DateUtils.DATE_FORMATTER));
                ps.setString(5, today.minusDays(7).format(DateUtils.DATE_FORMATTER));
                ps.setDouble(6, 1850.00);
                ps.setString(7, "RECEIVED");
                ps.setString(8, "Quarterly antibiotic and cardiovascular replenishment received in full.");
                ps.addBatch();

                ps.setString(1, "PO-2026-0892");
                ps.setInt(2, 5); // Sanofi
                ps.setString(3, today.minusDays(5).format(DateUtils.DATE_FORMATTER));
                ps.setString(4, today.plusDays(3).format(DateUtils.DATE_FORMATTER));
                ps.setString(5, null);
                ps.setDouble(6, 2400.00);
                ps.setString(7, "SENT");
                ps.setString(8, "Cold chain insulin and gastrointestinal stock order in transit.");
                ps.addBatch();

                ps.executeBatch();
            }

            // 8. Seed Disposals
            String insertDispSql = "INSERT INTO disposals (batch_id, medicine_id, batch_number, quantity, unit_cost, total_loss, reason, destruction_method, authorized_by, disposal_date, certificate_number, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?, ?);";
            try (PreparedStatement ps = conn.prepareStatement(insertDispSql)) {
                ps.setInt(1, 41);
                ps.setInt(2, 2);
                ps.setString(3, "AUG-EXP-088");
                ps.setInt(4, 15);
                ps.setDouble(5, 12.00);
                ps.setDouble(6, 180.00);
                ps.setString(7, "EXPIRED");
                ps.setString(8, "INCINERATION");
                ps.setString(9, today.minusDays(2).format(DateUtils.DATE_FORMATTER));
                ps.setString(10, "DISP-CERT-2026-0041");
                ps.setString(11, "Hazardous biomedical waste certified incinerator disposal under EPA Guideline Section 4.");
                ps.addBatch();

                ps.executeBatch();
            }

            // 9. Seed Audit Logs
            String insertAuditSql = "INSERT INTO audit_logs (user_id, username, role, action, entity_type, entity_id, details, ip_address, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, '127.0.0.1', ?);";
            try (PreparedStatement ps = conn.prepareStatement(insertAuditSql)) {
                String now = DateUtils.now();
                ps.setInt(1, 1);
                ps.setString(2, "admin");
                ps.setString(3, "ADMIN");
                ps.setString(4, "SYSTEM_INIT");
                ps.setString(5, "SYSTEM");
                ps.setString(6, "SYS_01");
                ps.setString(7, "MedTrack system initialized with secure database schema.");
                ps.setString(8, now);
                ps.addBatch();

                ps.setInt(1, 1);
                ps.setString(2, "admin");
                ps.setString(3, "ADMIN");
                ps.setString(4, "LOGIN");
                ps.setString(5, "USER");
                ps.setString(6, "1");
                ps.setString(7, "Administrator logged in successfully.");
                ps.setString(8, now);
                ps.addBatch();

                ps.setInt(1, 2);
                ps.setString(2, "pharmacist");
                ps.setString(3, "PHARMACIST");
                ps.setString(4, "DISPENSE_SALE");
                ps.setString(5, "SALE");
                ps.setString(6, "INV-2026-1007");
                ps.setString(7, "Dispensed prescription RX-78927 to Liam O'Connor (Total: $27.83).");
                ps.setString(8, now);
                ps.addBatch();

                ps.executeBatch();
            }

            // 10. Seed Notifications
            String insertNotifSql = "INSERT INTO notifications (title, message, type, severity, is_read, link_type, link_id, created_at) VALUES (?, ?, ?, ?, 0, ?, ?, ?);";
            try (PreparedStatement ps = conn.prepareStatement(insertNotifSql)) {
                String now = DateUtils.now();
                ps.setString(1, "Critical Expiry Alert: Zithromax 500");
                ps.setString(2, "Batch ZTH-CRIT-201 expires in 12 days. Markdown applied (35%). Please prioritize dispensing.");
                ps.setString(3, "EXPIRY");
                ps.setString(4, "CRITICAL");
                ps.setString(5, "BATCH");
                ps.setString(6, "37");
                ps.setString(7, now);
                ps.addBatch();

                ps.setString(1, "Cold Chain Warning: Lantus Solostar");
                ps.setString(2, "Batch LAN-WARN-108 in Cold Unit Alpha (4°C) has 45 days remaining.");
                ps.setString(3, "EXPIRY");
                ps.setString(4, "WARNING");
                ps.setString(5, "BATCH");
                ps.setString(6, "35");
                ps.setString(7, now);
                ps.addBatch();

                ps.setString(1, "Low Stock Reorder Recommended");
                ps.setString(2, "Lantus Solostar stock is at 33 units (Reorder threshold: 30 units).");
                ps.setString(3, "LOW_STOCK");
                ps.setString(4, "INFO");
                ps.setString(5, "MEDICINE");
                ps.setString(6, "15");
                ps.setString(7, now);
                ps.addBatch();

                ps.executeBatch();
            }

            System.out.println("Database seeding completed successfully with full clinical data.");

        } catch (Exception e) {
            System.err.println("Seeding error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
