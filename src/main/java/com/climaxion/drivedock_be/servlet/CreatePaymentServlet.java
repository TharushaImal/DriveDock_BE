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

@WebServlet("/api/payments")
public class CreatePaymentServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String reservationId = request.getParameter("reservation_id");
        String amount = request.getParameter("amount");
        String method = request.getParameter("payment_method");

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        Gson gson = new Gson();

        try {
            Connection con = DBConnection.getConnection();

            // 1. Insert payment
            String sql = "INSERT INTO payment (reservation_id, amount, payment_method, payment_status) VALUES (?, ?, ?, 'SUCCESS')";
            PreparedStatement ps = con.prepareStatement(sql);

            ps.setInt(1, Integer.parseInt(reservationId));
            ps.setDouble(2, Double.parseDouble(amount));
            ps.setString(3, method);

            int rows = ps.executeUpdate();

            // 2. Update reservation status
            if (rows > 0) {
                String updateSql = "UPDATE reservation SET status='CONFIRMED' WHERE id=?";
                PreparedStatement ups = con.prepareStatement(updateSql);
                ups.setInt(1, Integer.parseInt(reservationId));
                ups.executeUpdate();

                out.print(gson.toJson("Payment successful & reservation confirmed"));
            } else {
                out.print(gson.toJson("Payment failed"));
            }

        } catch (Exception e) {
            e.printStackTrace();
            out.print(gson.toJson("Error: " + e.getMessage()));
        }
    }
}
