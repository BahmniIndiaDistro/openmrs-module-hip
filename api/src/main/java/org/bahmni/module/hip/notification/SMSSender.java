package org.bahmni.module.hip.notification;

public interface SMSSender {
    String send(String sender, String phoneNumber, String message);
}
