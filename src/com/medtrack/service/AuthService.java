package com.medtrack.service;

import com.medtrack.dao.AuditLogDAO;
import com.medtrack.dao.UserDAO;
import com.medtrack.model.User;
import com.medtrack.util.PasswordUtils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthService {
    private final UserDAO userDAO = new UserDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    // In-memory token store: token -> SessionData
    public static class SessionData {
        private final User user;
        private final long expiryTime;

        public SessionData(User user, long ttlMillis) {
            this.user = user;
            this.expiryTime = System.currentTimeMillis() + ttlMillis;
        }

        public User getUser() { return user; }
        public boolean isExpired() { return System.currentTimeMillis() > expiryTime; }
    }

    private final Map<String, SessionData> sessions = new ConcurrentHashMap<>();
    private static final long SESSION_TTL = 24 * 60 * 60 * 1000L; // 24 hours

    public static class AuthResult {
        public boolean success;
        public String token;
        public User user;
        public String message;

        public AuthResult(boolean success, String token, User user, String message) {
            this.success = success;
            this.token = token;
            this.user = user;
            this.message = message;
        }
    }

    public AuthResult login(String username, String password, String ipAddress) {
        if (username == null || password == null) {
            return new AuthResult(false, null, null, "Username and password required.");
        }

        User user = userDAO.findByUsername(username);
        if (user == null) {
            return new AuthResult(false, null, null, "Invalid username or password.");
        }

        if (!user.isActive()) {
            return new AuthResult(false, null, null, "User account is deactivated. Contact Administrator.");
        }

        boolean valid = PasswordUtils.verifyPassword(password, user.getSalt(), user.getPasswordHash());
        if (!valid) {
            auditLogDAO.log(user.getId(), username, user.getRole(), "LOGIN_FAILED", "USER", String.valueOf(user.getId()), "Failed login attempt: bad credentials", ipAddress);
            return new AuthResult(false, null, null, "Invalid username or password.");
        }

        // Generate session token
        String token = UUID.randomUUID().toString();
        sessions.put(token, new SessionData(user, SESSION_TTL));
        userDAO.updateLastLogin(user.getId());

        auditLogDAO.log(user.getId(), username, user.getRole(), "LOGIN_SUCCESS", "USER", String.valueOf(user.getId()), "User logged in successfully.", ipAddress);

        return new AuthResult(true, token, user, "Login successful.");
    }

    public User validateToken(String token) {
        if (token == null || token.isBlank()) return null;
        SessionData data = sessions.get(token);
        if (data == null) return null;
        if (data.isExpired()) {
            sessions.remove(token);
            return null;
        }
        return data.getUser();
    }

    public void logout(String token, String ipAddress) {
        if (token != null) {
            SessionData data = sessions.remove(token);
            if (data != null) {
                auditLogDAO.log(data.getUser().getId(), data.getUser().getUsername(), data.getUser().getRole(), "LOGOUT", "USER", String.valueOf(data.getUser().getId()), "User logged out.", ipAddress);
            }
        }
    }

    public boolean hasRole(User user, String... allowedRoles) {
        if (user == null || user.getRole() == null) return false;
        if ("ADMIN".equalsIgnoreCase(user.getRole())) return true; // Superuser bypass
        for (String r : allowedRoles) {
            if (r.equalsIgnoreCase(user.getRole())) return true;
        }
        return false;
    }
}
