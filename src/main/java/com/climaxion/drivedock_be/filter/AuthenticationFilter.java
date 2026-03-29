package com.climaxion.drivedock_be.filter;

import com.climaxion.drivedock_be.util.DBConnection;
import com.google.gson.Gson;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.*;
import java.sql.*;
import java.util.*;

@WebFilter("/api/admin/*")
public class AuthenticationFilter implements Filter {

    private static final Gson gson = new Gson();
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
            "/api/admin/login",
            "/api/admin/logout"  // Logout also shouldn't require authentication
    );

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestURI = httpRequest.getRequestURI();

        // Check if this is an excluded path (login, logout)
        for (String excludedPath : EXCLUDED_PATHS) {
            if (requestURI.contains(excludedPath)) {
                chain.doFilter(request, response);
                return;
            }
        }

        // Get session
        HttpSession session = httpRequest.getSession(false);

        // Check if session exists and contains admin info
        if (session == null || session.getAttribute("adminId") == null) {
            // Also check for session token in header (for API calls from frontend)
            String sessionToken = httpRequest.getHeader("X-Session-Token");

            if (sessionToken == null || !isValidSession(sessionToken)) {
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResponse.setContentType("application/json");
                PrintWriter out = httpResponse.getWriter();
                Map<String, String> error = new HashMap<>();
                error.put("error", "Unauthorized access. Please login.");
                out.print(gson.toJson(error));
                return;
            }

            // If token is valid, get admin info and create session
            try {
                AdminInfo adminInfo = getAdminInfoFromToken(sessionToken);
                if (adminInfo != null) {
                    session = httpRequest.getSession(true);
                    session.setAttribute("adminId", adminInfo.id);
                    session.setAttribute("adminUsername", adminInfo.username);
                    session.setAttribute("adminRole", adminInfo.role);
                    session.setAttribute("adminFullName", adminInfo.fullName);
                    session.setMaxInactiveInterval(30 * 60); // 30 minutes
                } else {
                    httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    PrintWriter out = httpResponse.getWriter();
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "Invalid session token");
                    out.print(gson.toJson(error));
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                httpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                PrintWriter out = httpResponse.getWriter();
                Map<String, String> error = new HashMap<>();
                error.put("error", "Authentication error: " + e.getMessage());
                out.print(gson.toJson(error));
                return;
            }
        }

        // Check role-based access for specific endpoints
        String role = (String) session.getAttribute("adminRole");

        if (!hasRequiredRole(role, requestURI, httpRequest.getMethod())) {
            httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            httpResponse.setContentType("application/json");
            PrintWriter out = httpResponse.getWriter();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Insufficient permissions");
            out.print(gson.toJson(error));
            return;
        }

        // Continue with the request
        chain.doFilter(request, response);
    }

    private boolean isValidSession(String sessionToken) {
        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT id FROM admin_session WHERE session_token = ? AND expires_at > NOW()";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, sessionToken);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private AdminInfo getAdminInfoFromToken(String sessionToken) {
        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT a.id, a.username, a.role, a.full_name FROM admin a " +
                    "JOIN admin_session s ON a.id = s.admin_id " +
                    "WHERE s.session_token = ? AND s.expires_at > NOW() AND a.is_active = 1";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, sessionToken);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        AdminInfo info = new AdminInfo();
                        info.id = rs.getInt("id");
                        info.username = rs.getString("username");
                        info.role = rs.getString("role");
                        info.fullName = rs.getString("full_name");
                        return info;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean hasRequiredRole(String role, String requestURI, String method) {
        // Define role-based access rules
        if (role == null) return false;

        // SUPER_ADMIN can access everything
        if ("SUPER_ADMIN".equals(role)) return true;

        // ADMIN can access most things but not user deletion or role changes
        if ("ADMIN".equals(role)) {
            // Block access to user deletion and admin management
            if (requestURI.contains("/api/admin/users") && "DELETE".equals(method)) {
                return false;
            }
            if (requestURI.contains("/api/admin/admins")) {
                return false;
            }
            return true;
        }

        // VIEWER can only read data, no modifications
        if ("VIEWER".equals(role)) {
            // Allow only GET requests
            return "GET".equals(method);
        }

        return false;
    }

    // Inner class to hold admin info
    private static class AdminInfo {
        int id;
        String username;
        String role;
        String fullName;
    }

    @Override
    public void destroy() {

    }
}
