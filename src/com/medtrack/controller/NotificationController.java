package com.medtrack.controller;

import com.google.gson.JsonObject;
import com.medtrack.dao.NotificationDAO;
import com.medtrack.model.Notification;
import com.medtrack.server.JsonResponse;
import com.medtrack.server.RequestContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationController {
    private final NotificationDAO notificationDAO = new NotificationDAO();

    public void handleGetAll(RequestContext ctx) throws IOException {
        String unreadOnly = ctx.getQueryParam("unread");
        Integer limit = ctx.getQueryParamAsInt("limit");

        List<Notification> list = notificationDAO.findAll("true".equalsIgnoreCase(unreadOnly), limit != null ? limit : 50);
        int unreadCount = notificationDAO.getUnreadCount();

        Map<String, Object> res = new HashMap<>();
        res.put("notifications", list);
        res.put("unreadCount", unreadCount);

        JsonResponse.ok(ctx.getExchange(), res);
    }

    public void handleMarkRead(RequestContext ctx, int id) throws IOException {
        boolean ok = notificationDAO.markAsRead(id);
        if (ok) {
            JsonResponse.ok(ctx.getExchange(), "Notification marked as read.", null);
        } else {
            JsonResponse.error(ctx.getExchange(), "Failed to update notification.");
        }
    }

    public void handleMarkAllRead(RequestContext ctx) throws IOException {
        notificationDAO.markAllAsRead();
        JsonResponse.ok(ctx.getExchange(), "All notifications marked as read.", null);
    }
}
