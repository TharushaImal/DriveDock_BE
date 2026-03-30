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


@WebServlet("/api/parking-locations")
public class ParkingLocationsApiServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        Gson gson = new Gson();
        response.setContentType("application/json");
        response.setHeader("Access-Control-Allow-Origin", "*");
        PrintWriter out = response.getWriter();

        List<Map<String, Object>> locations = new ArrayList<>();

        try (Connection con = DBConnection.getConnection()) {
            String sql = "SELECT id, name, address, latitude, longitude, opening_time, closing_time " +
                    "FROM parking_location ORDER BY id";

            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {

                while (rs.next()) {
                    int locationId = rs.getInt("id");
                    Map<String, Object> loc = new HashMap<>();
                    loc.put("id", locationId);
                    loc.put("name", rs.getString("name"));
                    loc.put("address", rs.getString("address"));
                    loc.put("latitude", rs.getDouble("latitude"));
                    loc.put("longitude", rs.getDouble("longitude"));
                    loc.put("opening_time", rs.getString("opening_time"));
                    loc.put("closing_time", rs.getString("closing_time"));

                    // Count total and available slots
                    String countSql = "SELECT " +
                            "COUNT(*) AS total, " +
                            "SUM(CASE WHEN status = 'AVAILABLE' THEN 1 ELSE 0 END) AS available " +
                            "FROM parking_slot WHERE parking_location_id = ?";

                    try (PreparedStatement ps = con.prepareStatement(countSql)) {
                        ps.setInt(1, locationId);
                        try (ResultSet rs2 = ps.executeQuery()) {
                            if (rs2.next()) {
                                loc.put("total_slots", rs2.getInt("total"));
                                loc.put("available_slots", rs2.getInt("available"));
                            }
                        }
                    }

                    locations.add(loc);
                }
            }

            out.print(gson.toJson(locations));

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson(Collections.singletonMap("error", "Failed to fetch parking locations: " + e.getMessage())));
        }
    }
}
