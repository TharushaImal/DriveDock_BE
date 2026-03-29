package com.climaxion.drivedock_be.servlet;

import com.climaxion.drivedock_be.util.DBConnection;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.*;
import java.sql.*;
import java.util.*;

@WebServlet("/api/service-bookings")
public class ServiceBookingServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // This could be used to fetch bookings for a user
        String userId = request.getParameter("user_id");
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        Gson gson = new Gson();

        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson(Collections.singletonMap("error", "User ID is required")));
            return;
        }

        List<Map<String, Object>> bookings = new ArrayList<>();
        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT sb.id, sb.booking_date, sb.status, sb.vehicle_number, s.name as service_name, s.price " +
                    "FROM service_booking sb " +
                    "JOIN service s ON sb.service_id = s.id " +
                    "WHERE sb.user_id = ? " +
                    "ORDER BY sb.booking_date DESC";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, Integer.parseInt(userId));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> booking = new HashMap<>();
                        booking.put("id", rs.getInt("id"));
                        booking.put("service_name", rs.getString("service_name"));
                        booking.put("price", rs.getDouble("price"));
                        booking.put("booking_date", rs.getString("booking_date"));
                        booking.put("status", rs.getString("status"));
                        booking.put("vehicle_number", rs.getString("vehicle_number"));
                        bookings.add(booking);
                    }
                }
            }
            out.print(gson.toJson(bookings));
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson(Collections.singletonMap("error", "Failed to fetch bookings: " + e.getMessage())));
        }

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // For creating a new service booking
        String userId = request.getParameter("user_id");
        String serviceId = request.getParameter("service_id");
        String vehicleNumber = request.getParameter("vehicle_number");
        String bookingDate = request.getParameter("booking_date"); // e.g., "2024-05-20 14:30:00"

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        Gson gson = new Gson();

        if (userId == null || serviceId == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson(Collections.singletonMap("error", "User ID and Service ID are required")));
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            String sql = "INSERT INTO service_booking (user_id, service_id, booking_date, status, vehicle_number, created_at) " +
                    "VALUES (?, ?, ?, 'PENDING', ?, NOW())";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, Integer.parseInt(userId));
                ps.setInt(2, Integer.parseInt(serviceId));
                ps.setString(3, bookingDate != null ? bookingDate : new Timestamp(System.currentTimeMillis()).toString());
                ps.setString(4, vehicleNumber);

                int rows = ps.executeUpdate();
                if (rows > 0) {
                    out.print(gson.toJson(Collections.singletonMap("message", "Service booking created successfully")));
                } else {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.print(gson.toJson(Collections.singletonMap("error", "Failed to create service booking")));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson(Collections.singletonMap("error", "Database error: " + e.getMessage())));
        }
    }

    // Optional: Add a method to update booking status (for admin)

}
