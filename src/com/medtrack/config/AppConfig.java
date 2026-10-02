package com.medtrack.config;

import java.io.File;

public class AppConfig {
    public static final int PORT = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
    public static final String DB_DIR = "data";
    public static final String DB_FILE = DB_DIR + File.separator + "medtrack.db";
    public static final String DB_URL = "jdbc:sqlite:" + DB_FILE;

    // Pharmacy Metadata
    public static String PHARMACY_NAME = "Apex Care Pharmacy & Healthcare";
    public static String PHARMACY_LICENSE = "DL-MED-2026-98741X";
    public static String PHARMACY_GST = "36AAACH7412K1Z9";
    public static String PHARMACY_PHONE = "+91 98765 43210";
    public static String PHARMACY_EMAIL = "dispensary@apexmedtrack.io";
    public static String PHARMACY_ADDRESS = "Plot 42, Healthcare Boulevard, Cyber Hills, Tech City - 500081";
    public static String CURRENCY_SYMBOL = "$";
    public static double TAX_RATE_PERCENT = 5.0; // 5% GST / VAT on medicines
    public static int CRITICAL_ALERT_DAYS = 30;
    public static int WARNING_ALERT_DAYS = 90;
}
