package org.itmo.testing.lab2;

import org.itmo.testing.lab2.controller.UserAnalyticsController;

public class Main {
    public static void main(String[] args) {
        var app = UserAnalyticsController.createApp();
        app.start(8080);
        System.out.println("Server started...");
    }
}