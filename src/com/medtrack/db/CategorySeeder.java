package com.medtrack.db;

import com.medtrack.util.DateUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

/**
 * Repairs/initializes required default medicine categories without
 * re-seeding the rest of the database. This is safe to run on every startup.
 */
public final class CategorySeeder {

    private CategorySeeder() {}

    public static void ensureDefaults() {
        String sql = "INSERT OR IGNORE INTO categories (name, description, icon, created_at) VALUES (?, ?, ?, ?);";

        String[][] categories = {
            {"Antibiotics & Antimicrobials", "Broad-spectrum and targeted antibacterial drugs", "pill"},
            {"Analgesics & Pain Relief", "NSAIDs, antipyretics, and pain management medications", "shield-plus"},
            {"Cardiovascular & Hypertension", "Heart health, blood pressure, and cholesterol management", "heart-pulse"},
            {"Antidiabetic & Endocrinology", "Insulin, GLP-1, and oral hypoglycemic agents", "activity"},
            {"Respiratory & Pulmonology", "Inhalers, bronchodilators, and allergy relief", "wind"},
            {"Gastrointestinal & Digestive", "Antacids, proton-pump inhibitors, and anti-emetics", "droplet"},
            {"Central Nervous System", "Sedatives, antidepressants, antiepileptics", "brain"},
            {"Dermatologicals & Topicals", "Ointments, antifungals, and corticosteroid creams", "sparkles"},
            {"Ophthalmic & ENT", "Eye drops, ear sprays, and nasal decongestants", "eye"},
            {"Vitamins & Nutritional", "Essential multivitamins, minerals, and supplements", "sun"}
        };

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String now = DateUtils.now();

            for (String[] category : categories) {
                ps.setString(1, category[0]);
                ps.setString(2, category[1]);
                ps.setString(3, category[2]);
                ps.setString(4, now);
                ps.addBatch();
            }

            ps.executeBatch();
            System.out.println("Default medicine categories verified.");
        } catch (Exception e) {
            System.err.println("Category initialization error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
