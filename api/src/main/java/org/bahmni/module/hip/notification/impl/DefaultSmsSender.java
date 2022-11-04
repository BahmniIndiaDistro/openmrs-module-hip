package org.bahmni.module.hip.notification.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bahmni.module.hip.notification.SMSSender;
import org.openmrs.api.context.Context;
import org.springframework.stereotype.Repository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@Repository
public class DefaultSmsSender implements SMSSender {

    private static Logger logger = LogManager.getLogger(DefaultSmsSender.class);


    @Override
    public String send(String senderInfo, String phoneNumber, String messageText) {
        try {
            // Construct data
            String apiKey = "apikey=" + Context.getAdministrationService().getGlobalProperty("bahmni.smsKey");
            String message = "&message=" + messageText;
            String sender = "&sender=" + senderInfo;
            String numbers = "&numbers=" + phoneNumber;

            // Send data
            HttpURLConnection conn = (HttpURLConnection) new URL("https://api.textlocal.in/send/?").openConnection();
            String data = apiKey + numbers + message + sender;
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Length", Integer.toString(data.length()));
            conn.getOutputStream().write(data.getBytes("UTF-8"));
            logger.warn("Request 1 " + conn.getRequestMethod() );
            logger.warn("Request 2 " + conn.getInputStream() );
            logger.warn("Request 3 " + conn );


            final BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            final StringBuffer stringBuffer = new StringBuffer();
            String line;
            while ((line = rd.readLine()) != null) {
                stringBuffer.append(line);
            }

            logger.warn("String " + stringBuffer );
            rd.close();

            return stringBuffer.toString();
        } catch (Exception e) {
            System.out.println("Error SMS "+e);
            return "Error "+e;
        }
    }
}
