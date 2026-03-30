package com.climaxion.drivedock_be.servlet;

import com.climaxion.drivedock_be.util.DBConnection;
import com.climaxion.drivedock_be.util.NotificationService;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.*;
import java.sql.*;
import java.util.*;

@WebServlet("/api/reservations")
public class CreateReservationServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String userId = request.getParameter("user_id");
        String slotId = request.getParameter("parking_slot_id");
        String startTime = request.getParameter("start_time");
        String endTime = request.getParameter("end_time");

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        Gson gson = new Gson();

        try {
            Connection con = DBConnection.getConnection();

            String checkSql = "SELECT COUNT(*) FROM reservation " +
                    "WHERE parking_slot_id=? " +
                    "AND status IN ('PENDING','CONFIRMED') " +
                    "AND (start_time < ? AND end_time > ?)";

            PreparedStatement checkPs = con.prepareStatement(checkSql);
            checkPs.setInt(1, Integer.parseInt(slotId));
            checkPs.setString(2, endTime);
            checkPs.setString(3, startTime);

            ResultSet rs = checkPs.executeQuery();
            rs.next();

            if (rs.getInt(1) > 0) {
                out.print(gson.toJson("Slot already booked for this time"));
                return;
            }

            String sql = "INSERT INTO reservation (user_id, parking_slot_id, start_time, end_time, status) VALUES (?, ?, ?, ?, 'PENDING')";
            PreparedStatement ps = con.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);

            ps.setInt(1, Integer.parseInt(userId));
            ps.setInt(2, Integer.parseInt(slotId));
            ps.setString(3, startTime);
            ps.setString(4, endTime);

            int rows = ps.executeUpdate();

            if (rows > 0) {

                ResultSet generatedKeys = ps.getGeneratedKeys();
                int newReservationId = 0;
                if (generatedKeys.next()) {
                    newReservationId = generatedKeys.getInt(1);
                }

                Map<String, Object> result = new HashMap<>();
                result.put("message", "Reservation created");
                result.put("reservationId", newReservationId);
                out.print(gson.toJson(result));

                try (PreparedStatement tokenPs = con.prepareStatement("SELECT fcm_token FROM user WHERE id = ?")) {
                    tokenPs.setInt(1, Integer.parseInt(userId));
                    ResultSet tokenRs = tokenPs.executeQuery();
                    if (tokenRs.next() && tokenRs.getString("fcm_token") != null) {
                        String fcmToken = tokenRs.getString("fcm_token");
                        // Send in background thread to not block response
                        new Thread(() -> NotificationService.sendToUser(
                                fcmToken,
                                "Reservation Confirmed!",
                                "Your parking slot has been reserved successfully."
                        )).start();
                    }
                }
            } else {
                Map<String, Object> result = new HashMap<>();
                result.put("error", "Failed to create reservation");
                out.print(gson.toJson(result));
            }

        } catch (Exception e) {
            e.printStackTrace();
            out.print(gson.toJson("Error: " + e.getMessage()));
        }
    }
}
