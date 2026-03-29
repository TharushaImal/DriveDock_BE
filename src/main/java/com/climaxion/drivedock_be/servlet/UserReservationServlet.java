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

@WebServlet("/api/user-reservations")
public class UserReservationServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String userId = request.getParameter("user_id");

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        Gson gson = new Gson();

        List<Map<String, Object>> reservations = new ArrayList<>();

        try {
            Connection con = DBConnection.getConnection();

            String sql = "SELECT r.*, ps.slot_number, pl.name AS location_name " +
                    "FROM reservation r " +
                    "JOIN parking_slot ps ON r.parking_slot_id = ps.id " +
                    "JOIN parking_location pl ON ps.parking_location_id = pl.id " +
                    "WHERE r.user_id = ?";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, Integer.parseInt(userId));

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Map<String, Object> res = new HashMap<>();
                res.put("id", rs.getInt("id"));
                res.put("slot_number", rs.getString("slot_number"));
                res.put("location_name", rs.getString("location_name"));
                res.put("start_time", rs.getString("start_time"));
                res.put("end_time", rs.getString("end_time"));
                res.put("status", rs.getString("status"));

                reservations.add(res);
            }

            out.print(gson.toJson(reservations));

        } catch (Exception e) {
            e.printStackTrace();
            out.print(gson.toJson("Error: " + e.getMessage()));
        }
    }
}
