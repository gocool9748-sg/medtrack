package com.medtrack;

import com.medtrack.config.AppConfig;
import com.medtrack.db.DatabaseManager;
import com.medtrack.db.DatabaseSeeder;
import com.medtrack.db.CategorySeeder;
import com.medtrack.server.AppHttpServer;

public class Main {
    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   __  __ _____ ____ _____ ____      _    ____ _  __");
        System.out.println("  |  \\/  | ____|  _ \\_   _|  _ \\    / \\  / ___| |/ /");
        System.out.println("  | |\\/| |  _| | | | || | | |_) |  / _ \\| |   | ' / ");
        System.out.println("  | |  | | |___| |_| || | |  _ <  / ___ \\ |___| . \\ ");
        System.out.println("  |_|  |_|_____|____/ |_| |_| \\_\\/_/   \\_\\____|_|\\_\\");
        System.out.println("   Medicine Stock & Expiry Management System (v2.5 Enterprise)");
        System.out.println("================================================================================");

        try {
            // 1. Initialize SQLite Database Schema
            System.out.println("[1/3] Initializing Database Connection & Schema...");
            DatabaseManager.getInstance();

            // 2. Seed Realistic Clinical Data if fresh database
            System.out.println("[2/3] Checking & Seeding Healthcare Records...");
            DatabaseSeeder.seedIfNeeded();
            // Ensure default categories also exist when the database was created by an older build.
            CategorySeeder.ensureDefaults();

            // 3. Start Multi-threaded HTTP Server
            System.out.println("[3/3] Starting MedTrack Server...");
            AppHttpServer server = new AppHttpServer(AppConfig.PORT);
            server.start();

            System.out.println("================================================================================");
            System.out.println(" MedTrack is LIVE and accessible at:");
            System.out.println(" >>  http://localhost:" + AppConfig.PORT + "  <<");
            System.out.println("--------------------------------------------------------------------------------");
            System.out.println(" DEMO LOGIN CREDENTIALS:");
            System.out.println("  - Administrator:     admin      / Admin@123      (Role: ADMIN)");
            System.out.println("  - Lead Pharmacist:   pharmacist / Pharm@123      (Role: PHARMACIST)");
            System.out.println("  - Inventory Manager: manager    / Manager@123    (Role: INVENTORY_MANAGER)");
            System.out.println("  - Compliance Auditor:auditor    / Audit@123      (Role: AUDITOR)");
            System.out.println("================================================================================");

            // Add JVM Shutdown Hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutting down MedTrack server gracefully...");
                server.stop();
            }));

        } catch (Exception e) {
            System.err.println("FATAL: Failed to start MedTrack server:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
