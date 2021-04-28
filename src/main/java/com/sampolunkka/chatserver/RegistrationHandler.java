package com.sampolunkka.chatserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
//import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.*;

public class RegistrationHandler implements HttpHandler {

    ChatAuthenticator auth = null;

    private String responseBody = "";

    RegistrationHandler(ChatAuthenticator authenticator) {
        auth = authenticator;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        int code = 200;
        ChatServer.log("REGISTER", "Starting to handle registration");
        try {
            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                handleRegistrationFromClient(exchange);
            } else {
                code = 400;
                responseBody = "Not supported";
                ChatServer.log("error", responseBody);
            }
        } catch (IOException e) {
            code = 500;
            responseBody = "Error handling the request: " + e.getMessage();
            ChatServer.log("error", responseBody);
        } catch (Exception e) {
            code = 500;
            responseBody = "Server error: " + e.getMessage();
            ChatServer.log("error", responseBody);
        }
        if (code < 200 || code >= 400) {
            ChatServer.log(Integer.toString(code), responseBody);
        }

        //send response
        byte bytes [] = responseBody.getBytes("UTF-8");
        int length = bytes.length;
        if (length == 0) {
            exchange.sendResponseHeaders(code, -1);
        } else {
            exchange.sendResponseHeaders(code, bytes.length);
            OutputStream oStream = exchange.getResponseBody();
            oStream.write(bytes);
            oStream.close();
        }
    }

    private void handleRegistrationFromClient(HttpExchange exchange) throws IOException {

        ChatServer.log("REGISTER", "Trying to recieve registration");

        int code = 200;
        Headers headers = exchange.getRequestHeaders();

        int contentLength = 0;
        String contentType = "";

        if (headers.containsKey("Content-Length")) {
            contentLength = Integer.parseInt(headers.get("Content-Length").get(0));
            ChatServer.log("REGISTER", "Register contains content length: " + Integer.toString(contentLength));

        } else {
            code = 411;
            responseBody = "No length in request header";
            ChatServer.log("REGISTER", responseBody);
        }

        if (headers.containsKey("Content-Type")) {
            contentType = headers.get("Content-Type").get(0);
        } else {
            code = 400;
            responseBody = "No content type in request header";
            ChatServer.log("REGISTER", responseBody);
        }

        if (contentType.equalsIgnoreCase("application/json")) {

            ChatServer.log("REGISTER", "Content type is " + contentType);

            InputStream iStream = exchange.getRequestBody();
            String text = new BufferedReader(new InputStreamReader(iStream, StandardCharsets.UTF_8)).lines()
                    .collect(Collectors.joining("\n"));

            iStream.close();

            // JSON
            try {
                
                JSONObject userdetails   = new JSONObject(text);

                String username = userdetails.getString("username").trim();
                String password = userdetails.getString("password").trim();
                String email    = userdetails.getString("email").trim();

                System.out.println(username + " " + password + " " + email + "\n");
                System.out.println(username.length() + " " + password.length() + " " + email.length());

                if ( username.length() > 0 && password.length() > 0 && email.length() > 0 ) {
                    if (auth.addUser(username, password, email)) {
                        ChatServer.log("REGISTER", username + " registered as user");
                        code = 200;
                    } else {
                        code = 400;
                        responseBody = "User exists. Try logging in.";
                        ChatServer.log("REGISTER", responseBody);
                    }
                } else {
                    code = 400;
                    responseBody = "Invalid user credentials";
                    ChatServer.log("REGISTER", "Trimmed length < 1");
                }

            } catch (JSONException e) {
                code = 400;
                responseBody = "Invalid user credentials";
                ChatServer.log("REGISTER", e.toString() + "\n" + text);
            }
        } else {
            code = 411;
            responseBody = "Content-Type must be application/json, is: " + contentType;
            ChatServer.log("REGISTER", responseBody);
        }
    }
}
