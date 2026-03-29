package com.climaxion.drivedock_be.util;

public class HashPassword {
    public static void main(String[] args) {
        String defaultPassword = "Admin@123";
        String hashedPassword = PasswordUtil.hashPassword(defaultPassword);
        System.out.println("Password: " + defaultPassword);
        System.out.println("Hashed Password: " + hashedPassword);
    }
}
