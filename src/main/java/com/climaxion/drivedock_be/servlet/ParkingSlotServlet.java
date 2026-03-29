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

@WebServlet("/api/parking-slots")
public class ParkingSlotServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String locationId = request.getParameter("location_id");

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        Gson gson = new Gson();

        List<Map<String, Object>> slots = new ArrayList<>();

        try {
            Connection con = DBConnection.getConnection();

            String sql = "SELECT * FROM parking_slot WHERE parking_location_id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, Integer.parseInt(locationId));

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Map<String, Object> slot = new HashMap<>();
                slot.put("id", rs.getInt("id"));
                slot.put("slot_number", rs.getString("slot_number"));
                slot.put("status", rs.getString("status"));
                slot.put("price_per_hour", rs.getDouble("price_per_hour"));

                slots.add(slot);
            }

            out.print(gson.toJson(slots));

        } catch (Exception e) {
            e.printStackTrace();
            out.print(gson.toJson("Error: " + e.getMessage()));
        }
    }
}
