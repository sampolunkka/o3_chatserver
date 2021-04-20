package com.sampolunkka.chatserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class RegistrationHandler implements HttpHandler{

    ChatAuthenticator auth = null;

    private String responseBody = "";

    RegistrationHandler(ChatAuthenticator authenticator) {
        auth = authenticator;
    }


    @Override
    public void handle(HttpExchange exchange) throws IOException {
        int code = 200;
        ChatServer.log("<RegistrationHandler>", "Starting to handle...");
        try {
            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {

                handleRegistrationFromClient(exchange);
            } else {
                code = 400;
                responseBody = "Not supported";
                ChatServer.log("error",responseBody);
            }
        } catch (IOException e) {
            code = 500;
            responseBody = "Error handling the request: " + e.getMessage();
            ChatServer.log("error",responseBody);
        } catch (Exception e) {
            code = 500;
            responseBody = "Server error: " + e.getMessage();
            ChatServer.log("error",responseBody);
        }
        if (code < 200 || code >= 400) {
            ChatServer.log(Integer.toString(code), responseBody);
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
            ChatServer.log("REGISTER", "Register contains content length: "
                    + Integer.toString(contentLength));
        
        } else {
            code = 411;
            ChatServer.log("REGISTER", "No content length in header");;
        }

        if (headers.containsKey("Content-Type")) {
            contentType = headers.get("Content-Type").get(0);
            ChatServer.log("REGISTER", "Register contains key Content-Type");
        
        } else {
            code = 400;
            responseBody = "No content type in header";
            ChatServer.log("REGISTER", responseBody);;
        }
        
        if (contentType.equalsIgnoreCase("text/plain")) {

            ChatServer.log("REGISTER", "Content is text/plain");

            InputStream iStream = exchange.getRequestBody();
            String text = new BufferedReader(
                new InputStreamReader(
                        iStream,
                        StandardCharsets.UTF_8
                )
            ).lines().collect(Collectors.joining("\n"));
            
            iStream.close();

            if (text.trim().length() > 0) {
                String[] items = text.split(":");
                if (items.length == 2) {

                    String username = items[0].trim();
                    System.out.println(items[0]);

                    String password = items[1].trim();
                    System.out.println(items[1]);
                    //String email = items[2].trim();
                    //System.out.println(items[2]);
                    
                    if (username.length() > 0 && password.length() > 0) {
                        //luokauyttaja
                        if (auth.addUser(username, password, "email")) {
                            ChatServer.log("REGISTER", username + " registered as user");
                            code = 200;
                            //exchange.sendResponseHeaders(code, -1);
                        } else {
                            code = 400;
                            responseBody = "Invalid user credentials";
                        }
                    } else {
                        code = 400;
                        responseBody = "Invalid user credentials";
                    }
                } else {
                    code = 400;
                    responseBody = "Invalid user credentials";
                }

            } else {
                code = 400;
                responseBody = "No content in request";
            }
        } else {
            code = 411;
            responseBody = "Content-Type must be text/plain";
            ChatServer.log("REGISTER", responseBody);
        }

        exchange.sendResponseHeaders(code, -1);
    }
}
