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

@WebServlet("/api/reservations/complete")
public class CompleteReservationServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String reservationId = request.getParameter("reservation_id");

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        Gson gson = new Gson();

        try {
            Connection con = DBConnection.getConnection();

            String sql = "UPDATE reservation SET status='COMPLETED', completed_at=NOW() WHERE id=?";
            PreparedStatement ps = con.prepareStatement(sql);

            ps.setInt(1, Integer.parseInt(reservationId));

            int rows = ps.executeUpdate();

            if (rows > 0) {
                out.print(gson.toJson("Reservation completed"));
            } else {
                out.print(gson.toJson("Failed to update reservation"));
            }

        } catch (Exception e) {
            e.printStackTrace();
            out.print(gson.toJson("Error: " + e.getMessage()));
        }
    }
}
