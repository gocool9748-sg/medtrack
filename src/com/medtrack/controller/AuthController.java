package com.medtrack.controller;

import com.google.gson.JsonObject;
import com.medtrack.dao.AuditLogDAO;
import com.medtrack.dao.UserDAO;
import com.medtrack.model.User;
import com.medtrack.server.JsonResponse;
import com.medtrack.server.RequestContext;
import com.medtrack.service.AuthService;
import com.medtrack.util.PasswordUtils;

import java.io.IOException;
import java.util.List;

public class AuthController {
    private final AuthService authService;
    private final UserDAO userDAO = new UserDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public void handleLogin(RequestContext ctx) throws IOException {
        JsonObject body = ctx.getBodyAsJsonObject();
        String username = body.has("username") ? body.get("username").getAsString() : null;
        String password = body.has("password") ? body.get("password").getAsString() : null;

        AuthService.AuthResult res = authService.login(username, password, ctx.getIpAddress());
        if (res.success) {
            JsonObject responseData = new JsonObject();
            responseData.addProperty("token", res.token);
            responseData.addProperty("id", res.user.getId());
            responseData.addProperty("username", res.user.getUsername());
            responseData.addProperty("fullName", res.user.getFullName());
            responseData.addProperty("email", res.user.getEmail());
            responseData.addProperty("role", res.user.getRole());
            JsonResponse.ok(ctx.getExchange(), res.message, responseData);
        } else {
            JsonResponse.badRequest(ctx.getExchange(), res.message);
        }
    }

    public void handleMe(RequestContext ctx) throws IOException {
        User user = authService.validateToken(ctx.getBearerToken());
        if (user == null) {
            JsonResponse.unauthorized(ctx.getExchange(), "Invalid or expired session token.");
            return;
        }
        JsonResponse.ok(ctx.getExchange(), user);
    }

    public void handleLogout(RequestContext ctx) throws IOException {
        authService.logout(ctx.getBearerToken(), ctx.getIpAddress());
        JsonResponse.ok(ctx.getExchange(), "Logged out successfully.", null);
    }

    public void handleGetUsers(RequestContext ctx) throws IOException {
        User currentUser = authService.validateToken(ctx.getBearerToken());
        if (currentUser == null) {
            JsonResponse.unauthorized(ctx.getExchange(), null);
            return;
        }
        if (!authService.hasRole(currentUser, "ADMIN", "AUDITOR")) {
            JsonResponse.forbidden(ctx.getExchange(), "Only administrators and auditors can view users.");
            return;
        }
        List<User> users = userDAO.findAll();
        JsonResponse.ok(ctx.getExchange(), users);
    }

    public void handleCreateUser(RequestContext ctx) throws IOException {
        User currentUser = authService.validateToken(ctx.getBearerToken());
        if (currentUser == null) {
            JsonResponse.unauthorized(ctx.getExchange(), null);
            return;
        }
        if (!authService.hasRole(currentUser, "ADMIN")) {
            JsonResponse.forbidden(ctx.getExchange(), "Only administrators can create user accounts.");
            return;
        }

        JsonObject body = ctx.getBodyAsJsonObject();
        String username = body.get("username").getAsString();
        String password = body.get("password").getAsString();
        String fullName = body.get("fullName").getAsString();
        String email = body.get("email").getAsString();
        String role = body.has("role") ? body.get("role").getAsString() : "PHARMACIST";

        if (userDAO.findByUsername(username) != null) {
            JsonResponse.badRequest(ctx.getExchange(), "Username '" + username + "' is already taken.");
            return;
        }

        String salt = PasswordUtils.generateSalt();
        String hash = PasswordUtils.hashPassword(password, salt);

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPasswordHash(hash);
        newUser.setSalt(salt);
        newUser.setFullName(fullName);
        newUser.setEmail(email);
        newUser.setRole(role.toUpperCase());
        newUser.setActive(true);

        boolean success = userDAO.create(newUser);
        if (success) {
            auditLogDAO.log(currentUser.getId(), currentUser.getUsername(), currentUser.getRole(), "CREATE_USER", "USER", String.valueOf(newUser.getId()), "Created user: " + username + " (" + role + ")", ctx.getIpAddress());
            JsonResponse.ok(ctx.getExchange(), "User account created successfully.", newUser);
        } else {
            JsonResponse.error(ctx.getExchange(), "Failed to create user.");
        }
    }
}
