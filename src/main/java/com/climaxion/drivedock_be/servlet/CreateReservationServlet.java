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
            PreparedStatement ps = con.prepareStatement(sql);

            ps.setInt(1, Integer.parseInt(userId));
            ps.setInt(2, Integer.parseInt(slotId));
            ps.setString(3, startTime);
            ps.setString(4, endTime);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                out.print(gson.toJson("Reservation created"));
            } else {
                out.print(gson.toJson("Failed to create reservation"));
            }

        } catch (Exception e) {
            e.printStackTrace();
            out.print(gson.toJson("Error: " + e.getMessage()));
        }
    }
}
