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

@WebServlet("/api/admin/parking-locations")
public class ParkingLocationServlet extends HttpServlet {
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Check authentication
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("adminId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(gson.toJson(Collections.singletonMap("error", "Unauthorized")));
            return;
        }

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String locationId = request.getParameter("location_id");

        try (Connection con = DBConnection.getConnection()) {
            if (locationId != null && !locationId.isEmpty()) {
                // Get single location with its slots
                Map<String, Object> location = getLocationWithSlots(con, Integer.parseInt(locationId));
                out.print(gson.toJson(location));
            } else {
                // Get all locations
                List<Map<String, Object>> locations = getAllLocations(con);
                out.print(gson.toJson(locations));
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson(Collections.singletonMap("error", "Failed to fetch data: " + e.getMessage())));
        }
    }

    private List<Map<String, Object>> getAllLocations(Connection con) throws SQLException {
        List<Map<String, Object>> locations = new ArrayList<>();
        String sql = "SELECT * FROM parking_location ORDER BY id";

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> loc = new HashMap<>();
                loc.put("id", rs.getInt("id"));
                loc.put("name", rs.getString("name"));
                loc.put("address", rs.getString("address"));
                loc.put("latitude", rs.getDouble("latitude"));
                loc.put("longitude", rs.getDouble("longitude"));
                loc.put("opening_time", rs.getString("opening_time"));
                loc.put("closing_time", rs.getString("closing_time"));

                // Get slot count and available slots
                String countSql = "SELECT COUNT(*) as total, SUM(CASE WHEN status='AVAILABLE' THEN 1 ELSE 0 END) as available FROM parking_slot WHERE parking_location_id = ?";
                try (PreparedStatement ps = con.prepareStatement(countSql)) {
                    ps.setInt(1, rs.getInt("id"));
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
        return locations;
    }

    private Map<String, Object> getLocationWithSlots(Connection con, int locationId) throws SQLException {
        Map<String, Object> location = new HashMap<>();

        // Get location details
        String locSql = "SELECT * FROM parking_location WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(locSql)) {
            ps.setInt(1, locationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    location.put("id", rs.getInt("id"));
                    location.put("name", rs.getString("name"));
                    location.put("address", rs.getString("address"));
                    location.put("latitude", rs.getDouble("latitude"));
                    location.put("longitude", rs.getDouble("longitude"));
                    location.put("opening_time", rs.getString("opening_time"));
                    location.put("closing_time", rs.getString("closing_time"));
                }
            }
        }

        // Get slots for this location
        List<Map<String, Object>> slots = new ArrayList<>();
        String slotSql = "SELECT * FROM parking_slot WHERE parking_location_id = ? ORDER BY slot_number";
        try (PreparedStatement ps = con.prepareStatement(slotSql)) {
            ps.setInt(1, locationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> slot = new HashMap<>();
                    slot.put("id", rs.getInt("id"));
                    slot.put("slot_number", rs.getString("slot_number"));
                    slot.put("status", rs.getString("status"));
                    slot.put("price_per_hour", rs.getDouble("price_per_hour"));
                    slots.add(slot);
                }
            }
        }
        location.put("slots", slots);

        return location;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Check authentication and role
        HttpSession session = request.getSession(false);
        if (session == null || !hasAdminAccess(session)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write(gson.toJson(Collections.singletonMap("error", "Insufficient permissions")));
            return;
        }

        String action = request.getParameter("action");

        if ("location".equals(action)) {
            handleLocationOperation(request, response);
        } else if ("slot".equals(action)) {
            handleSlotOperation(request, response);
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(Collections.singletonMap("error", "Invalid action")));
        }
    }

    private void handleLocationOperation(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String id = request.getParameter("id");
        String name = request.getParameter("name");
        String address = request.getParameter("address");
        String latitude = request.getParameter("latitude");
        String longitude = request.getParameter("longitude");
        String openingTime = request.getParameter("opening_time");
        String closingTime = request.getParameter("closing_time");

        PrintWriter out = response.getWriter();

        // Validate inputs
        if (name == null || name.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson(Collections.singletonMap("error", "Location name is required")));
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            if (id != null && !id.isEmpty()) {
                // Update existing location
                String sql = "UPDATE parking_location SET name=?, address=?, latitude=?, longitude=?, opening_time=?, closing_time=? WHERE id=?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, name);
                    ps.setString(2, address);
                    ps.setDouble(3, Double.parseDouble(latitude));
                    ps.setDouble(4, Double.parseDouble(longitude));
                    ps.setString(5, openingTime);
                    ps.setString(6, closingTime);
                    ps.setInt(7, Integer.parseInt(id));
                    ps.executeUpdate();
                }
                out.print(gson.toJson(Collections.singletonMap("message", "Location updated successfully")));
            } else {
                // Insert new location
                String sql = "INSERT INTO parking_location (name, address, latitude, longitude, opening_time, closing_time) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, name);
                    ps.setString(2, address);
                    ps.setDouble(3, Double.parseDouble(latitude));
                    ps.setDouble(4, Double.parseDouble(longitude));
                    ps.setString(5, openingTime);
                    ps.setString(6, closingTime);
                    ps.executeUpdate();

                    ResultSet rs = ps.getGeneratedKeys();
                    if (rs.next()) {
                        int newId = rs.getInt(1);
                        out.print(gson.toJson(Map.of("message", "Location created successfully", "id", newId)));
                    } else {
                        out.print(gson.toJson(Collections.singletonMap("message", "Location created successfully")));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson(Collections.singletonMap("error", "Database error: " + e.getMessage())));
        }
    }

    private void handleSlotOperation(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String id = request.getParameter("id");
        String locationId = request.getParameter("location_id");
        String slotNumber = request.getParameter("slot_number");
        String pricePerHour = request.getParameter("price_per_hour");
        String status = request.getParameter("status");

        PrintWriter out = response.getWriter();

        // Validate inputs
        if (locationId == null || locationId.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson(Collections.singletonMap("error", "Location ID is required")));
            return;
        }

        if (slotNumber == null || slotNumber.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson(Collections.singletonMap("error", "Slot number is required")));
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            if (id != null && !id.isEmpty()) {
                // Update existing slot
                StringBuilder sql = new StringBuilder("UPDATE parking_slot SET slot_number=?, price_per_hour=?");
                List<Object> params = new ArrayList<>();
                params.add(slotNumber);
                params.add(Double.parseDouble(pricePerHour));

                if (status != null && !status.isEmpty()) {
                    sql.append(", status=?");
                    params.add(status);
                }

                sql.append(" WHERE id=?");
                params.add(Integer.parseInt(id));

                try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
                    for (int i = 0; i < params.size(); i++) {
                        ps.setObject(i + 1, params.get(i));
                    }
                    ps.executeUpdate();
                }
                out.print(gson.toJson(Collections.singletonMap("message", "Slot updated successfully")));
            } else {
                // Insert new slot
                String sql = "INSERT INTO parking_slot (parking_location_id, slot_number, status, price_per_hour) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, Integer.parseInt(locationId));
                    ps.setString(2, slotNumber);
                    ps.setString(3, status != null ? status : "AVAILABLE");
                    ps.setDouble(4, Double.parseDouble(pricePerHour));
                    ps.executeUpdate();
                }
                out.print(gson.toJson(Collections.singletonMap("message", "Slot created successfully")));
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson(Collections.singletonMap("error", "Database error: " + e.getMessage())));
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Check authentication and role
        HttpSession session = request.getSession(false);
        if (session == null || !hasAdminAccess(session)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write(gson.toJson(Collections.singletonMap("error", "Insufficient permissions")));
            return;
        }

        String type = request.getParameter("type");
        String id = request.getParameter("id");

        PrintWriter out = response.getWriter();

        if (id == null || id.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson(Collections.singletonMap("error", "ID is required")));
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            if ("location".equals(type)) {
                // Check if location has reservations
                String checkSql = "SELECT COUNT(*) FROM reservation r JOIN parking_slot ps ON r.parking_slot_id = ps.id WHERE ps.parking_location_id = ?";
                try (PreparedStatement ps = con.prepareStatement(checkSql)) {
                    ps.setInt(1, Integer.parseInt(id));
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                            out.print(gson.toJson(Collections.singletonMap("error", "Cannot delete location with existing reservations")));
                            return;
                        }
                    }
                }

                // Delete all slots first
                String deleteSlotsSql = "DELETE FROM parking_slot WHERE parking_location_id = ?";
                try (PreparedStatement ps = con.prepareStatement(deleteSlotsSql)) {
                    ps.setInt(1, Integer.parseInt(id));
                    ps.executeUpdate();
                }

                // Delete location
                String deleteSql = "DELETE FROM parking_location WHERE id = ?";
                try (PreparedStatement ps = con.prepareStatement(deleteSql)) {
                    ps.setInt(1, Integer.parseInt(id));
                    ps.executeUpdate();
                }
                out.print(gson.toJson(Collections.singletonMap("message", "Location deleted successfully")));

            } else if ("slot".equals(type)) {
                // Check if slot has reservations
                String checkSql = "SELECT COUNT(*) FROM reservation WHERE parking_slot_id = ? AND status IN ('PENDING', 'CONFIRMED')";
                try (PreparedStatement ps = con.prepareStatement(checkSql)) {
                    ps.setInt(1, Integer.parseInt(id));
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                            out.print(gson.toJson(Collections.singletonMap("error", "Cannot delete slot with active reservations")));
                            return;
                        }
                    }
                }

                String deleteSql = "DELETE FROM parking_slot WHERE id = ?";
                try (PreparedStatement ps = con.prepareStatement(deleteSql)) {
                    ps.setInt(1, Integer.parseInt(id));
                    ps.executeUpdate();
                }
                out.print(gson.toJson(Collections.singletonMap("message", "Slot deleted successfully")));
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(gson.toJson(Collections.singletonMap("error", "Invalid delete type")));
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
