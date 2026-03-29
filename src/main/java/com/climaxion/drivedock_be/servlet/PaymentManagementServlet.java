package com.climaxion.drivedock_be.servlet;

import com.climaxion.drivedock_be.util.DBConnection;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.*;
import java.sql.*;
import java.util.*;

@WebServlet("/api/admin/payments")
public class PaymentManagementServlet extends HttpServlet {
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("adminId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(gson.toJson(Collections.singletonMap("error", "Unauthorized")));
            return;
        }

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String paymentId = request.getParameter("id");
        String status = request.getParameter("status");

        try (Connection con = DBConnection.getConnection()) {
            if (paymentId != null && !paymentId.isEmpty()) {
                // Get single payment details
                Map<String, Object> payment = getPaymentDetails(con, Integer.parseInt(paymentId));
                out.print(gson.toJson(payment));
            } else {
                // Get all payments with filters
                List<Map<String, Object>> payments = getAllPayments(con, status);
                out.print(gson.toJson(payments));
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson(Collections.singletonMap("error", "Failed to fetch payments: " + e.getMessage())));
        }
    }

    private List<Map<String, Object>> getAllPayments(Connection con, String statusFilter) throws SQLException {
        List<Map<String, Object>> payments = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
                "SELECT p.id, p.amount, p.payment_method, p.payment_status, p.payment_date, " +
                        "r.id as reservation_id, r.start_time, r.end_time, r.status as reservation_status, " +
                        "u.id as user_id, u.fname, u.lname, u.email, " +
                        "ps.slot_number, pl.name as location_name " +
                        "FROM payment p " +
                        "JOIN reservation r ON p.reservation_id = r.id " +
                        "JOIN user u ON r.user_id = u.id " +
                        "JOIN parking_slot ps ON r.parking_slot_id = ps.id " +
                        "JOIN parking_location pl ON ps.parking_location_id = pl.id " +
                        "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        if (statusFilter != null && !statusFilter.isEmpty()) {
            sql.append("AND p.payment_status = ? ");
            params.add(statusFilter);
        }

        sql.append("ORDER BY p.payment_date DESC");

        try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> payment = new HashMap<>();
                    payment.put("id", rs.getInt("id"));
                    payment.put("amount", rs.getDouble("amount"));
                    payment.put("payment_method", rs.getString("payment_method"));
                    payment.put("payment_status", rs.getString("payment_status"));
                    payment.put("payment_date", rs.getTimestamp("payment_date"));

                    Map<String, Object> reservation = new HashMap<>();
                    reservation.put("id", rs.getInt("reservation_id"));
                    reservation.put("start_time", rs.getTimestamp("start_time"));
                    reservation.put("end_time", rs.getTimestamp("end_time"));
                    reservation.put("status", rs.getString("reservation_status"));
                    reservation.put("slot_number", rs.getString("slot_number"));
                    reservation.put("location_name", rs.getString("location_name"));
                    payment.put("reservation", reservation);

                    Map<String, Object> user = new HashMap<>();
                    user.put("id", rs.getInt("user_id"));
                    user.put("name", rs.getString("fname") + " " + rs.getString("lname"));
                    user.put("email", rs.getString("email"));
                    payment.put("user", user);

                    payments.add(payment);
                }
            }
        }

        return payments;
    }

    private Map<String, Object> getPaymentDetails(Connection con, int paymentId) throws SQLException {
        Map<String, Object> payment = new HashMap<>();

        String sql = "SELECT p.*, r.*, u.*, ps.slot_number, pl.name as location_name " +
                "FROM payment p " +
                "JOIN reservation r ON p.reservation_id = r.id " +
                "JOIN user u ON r.user_id = u.id " +
                "JOIN parking_slot ps ON r.parking_slot_id = ps.id " +
                "JOIN parking_location pl ON ps.parking_location_id = pl.id " +
                "WHERE p.id = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, paymentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    payment.put("id", rs.getInt("id"));
                    payment.put("amount", rs.getDouble("amount"));
                    payment.put("payment_method", rs.getString("payment_method"));
                    payment.put("payment_status", rs.getString("payment_status"));
                    payment.put("payment_date", rs.getTimestamp("payment_date"));
                    payment.put("created_at", rs.getTimestamp("created_at"));

                    Map<String, Object> reservation = new HashMap<>();
                    reservation.put("id", rs.getInt("reservation_id"));
                    reservation.put("start_time", rs.getTimestamp("start_time"));
                    reservation.put("end_time", rs.getTimestamp("end_time"));
                    reservation.put("status", rs.getString("status"));
                    reservation.put("total_amount", rs.getDouble("total_amount"));
                    reservation.put("slot_number", rs.getString("slot_number"));
                    reservation.put("location_name", rs.getString("location_name"));
                    payment.put("reservation", reservation);

                    Map<String, Object> user = new HashMap<>();
                    user.put("id", rs.getInt("user_id"));
                    user.put("fname", rs.getString("fname"));
                    user.put("lname", rs.getString("lname"));
                    user.put("email", rs.getString("email"));
                    user.put("phone", rs.getString("phone_number"));
                    payment.put("user", user);
                }
            }
        }

        return payment;
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || !hasAdminAccess(session)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write(gson.toJson(Collections.singletonMap("error", "Insufficient permissions")));
            return;
        }

        String paymentId = request.getParameter("id");
        String status = request.getParameter("status");

        PrintWriter out = response.getWriter();

        if (paymentId == null || status == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson(Collections.singletonMap("error", "Payment ID and status are required")));
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            // Update payment status
            String updateSql = "UPDATE payment SET payment_status = ? WHERE id = ?";
            try (PreparedStatement ps = con.prepareStatement(updateSql)) {
                ps.setString(1, status);
                ps.setInt(2, Integer.parseInt(paymentId));
                ps.executeUpdate();
            }

            // If payment is refunded, update reservation status
            if ("REFUNDED".equals(status)) {
                String updateResSql = "UPDATE reservation r JOIN payment p ON r.id = p.reservation_id " +
                        "SET r.status = 'CANCELLED' WHERE p.id = ?";
                try (PreparedStatement ps = con.prepareStatement(updateResSql)) {
                    ps.setInt(1, Integer.parseInt(paymentId));
                    ps.executeUpdate();
                }
            }

            out.print(gson.toJson(Collections.singletonMap("message", "Payment status updated successfully")));
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson(Collections.singletonMap("error", "Database error: " + e.getMessage())));
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || !"SUPER_ADMIN".equals(session.getAttribute("adminRole"))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write(gson.toJson(Collections.singletonMap("error", "Only Super Admin can delete payments")));
            return;
        }

        String paymentId = request.getParameter("id");

        PrintWriter out = response.getWriter();

        if (paymentId == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson(Collections.singletonMap("error", "Payment ID is required")));
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            String sql = "DELETE FROM payment WHERE id = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, Integer.parseInt(paymentId));
                ps.executeUpdate();
                out.print(gson.toJson(Collections.singletonMap("message", "Payment deleted successfully")));
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson(Collections.singletonMap("error", "Database error: " + e.getMessage())));
        }
    }

    private boolean hasAdminAccess(HttpSession session) {
        String role = (String) session.getAttribute("adminRole");
        return "SUPER_ADMIN".equals(role) || "ADMIN".equals(role);
    }
}
