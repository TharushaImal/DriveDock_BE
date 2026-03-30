package com.climaxion.drivedock_be.util;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

import java.io.InputStream;

public class NotificationService {

    private static boolean isInitialized = false;

    private static synchronized void initializeFirebase() {
        if (isInitialized) return;
        try {
            InputStream serviceAccount = NotificationService.class
                    .getClassLoader()
                    .getResourceAsStream("service-account-file.json");

            if (serviceAccount == null) {
                System.err.println("Error: service-account-file.json not found in src/main/resources/");
                return;
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp.initializeApp(options);
            isInitialized = true;
            System.out.println("Firebase successfully initialized.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendToUser(String fcmToken, String title, String body) {
        if (fcmToken == null || fcmToken.isEmpty()) return;

        if (!isInitialized) {
            initializeFirebase();
        }

        if (!isInitialized) {
            System.err.println("Skipping notification: Firebase not initialized.");
            return;
        }

        try {
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            System.out.println("Successfully sent message: " + response);

        } catch (Exception e) {
            System.err.println("Error sending FCM message: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
