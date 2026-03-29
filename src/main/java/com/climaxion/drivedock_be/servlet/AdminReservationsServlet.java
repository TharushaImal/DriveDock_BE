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

@WebServlet("/api/admin/reservations")
public class AdminReservationsServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        Gson gson = new Gson();

        List<Map<String, Object>> list = new ArrayList<>();

        try {
            Connection con = DBConnection.getConnection();

            String sql = "SELECT r.id, u.fname, ps.slot_number, r.status, r.start_time, r.end_time " +
                    "FROM reservation r " +
                    "JOIN user u ON r.user_id = u.id " +
                    "JOIN parking_slot ps ON r.parking_slot_id = ps.id";

            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                Map<String, Object> r = new HashMap<>();
                r.put("id", rs.getInt("id"));
                r.put("user", rs.getString("fname"));
                r.put("slot", rs.getString("slot_number"));
                r.put("status", rs.getString("status"));
                r.put("start", rs.getString("start_time"));
                r.put("end", rs.getString("end_time"));

                list.add(r);
            }

            out.print(gson.toJson(list));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String id = request.getParameter("reservation_id");
        String status = request.getParameter("status");

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        try {
            Connection con = DBConnection.getConnection();

            String sql = "UPDATE reservation SET status=? WHERE id=?";
            PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1, status);
            ps.setInt(2, Integer.parseInt(id));

            ps.executeUpdate();

            out.print("Updated");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
