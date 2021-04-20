package com.sampolunkka.chatserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.logging.ErrorManager;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.Headers;


public class ChatHandler implements HttpHandler{
    
    private String responseBody = "";

    ArrayList<String> messages = new ArrayList<String>();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        int code = 200;
        ChatServer.log("<CHATHANDLER>", "Starting to handle...");
        try {
            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                code = handleChatMessageFromClient(exchange);
                ChatServer.log("INFO","Reached end of POST");
                exchange.close();
            } else if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                code = handleGetRequestFromClient(exchange);
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
            sendResponseMessageToClient(exchange, responseBody, code);
        }
    }

    private int handleGetRequestFromClient(HttpExchange exchange) throws IOException {
        int code = 200;

        if (messages.isEmpty()) {
            code = 204;
            exchange.sendResponseHeaders(code, -1);
            ChatServer.log("INFO", "Messages is empty");
            return code;
        }

        String messageBody = "";
        for (String message : messages) {
            messageBody += message + "\n";
        }
        
        sendResponseMessageToClient(exchange, messageBody, code);
        
        return code;
    }

    private int handleChatMessageFromClient(HttpExchange exchange) throws IOException {

        ChatServer.log("POST", "Trying to recieve chat message");
        
        int code = 200;
        Headers headers = exchange.getRequestHeaders();

        int contentLength = 0;
        String contentType = "";
        
        
        if (headers.containsKey("Content-Length")) {
            contentLength = Integer.parseInt(headers.get("Content-Length").get(0));
            ChatServer.log("POST", "Chat message contains content length: "
                    + Integer.toString(contentLength));
        
        } else {
            code = 411;
            ChatServer.log("POST", "No content length in header");
            return code;
        }

        if (headers.containsKey("Content-Type")) {
            contentType = headers.get("Content-Type").get(0);
            ChatServer.log("POST", "Chat message contains key Content-Type");
        
        } else {
            code = 400;
            responseBody = "No content type in request";
            ChatServer.log("POST", responseBody);
            return code;
        }
        
        if (contentType.equalsIgnoreCase("text/plain")) {

            ChatServer.log("POST", "Content is text/plain");

            InputStream iStream = exchange.getRequestBody();
            String text = new BufferedReader(new InputStreamReader(iStream, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
            iStream.close();

            if (text.trim().length() > 0) {
                processMessage(text);
                exchange.sendResponseHeaders(code, -1);
                ChatServer.log("POST", "Content is: " + text);
            } else {
                code = 400;
                responseBody = "No content in request";
            }
            

        } else {
            code = 411;
            responseBody = "Content-Type must be text/plain";
            ChatServer.log("POST", responseBody);
        }
        
        //sendResponseMessageToClient(exchange, responseBody, code);
        return code;
    }

    private byte [] bytefy(String string) throws UnsupportedEncodingException {
        byte bytes [] = string.getBytes("UTF-8");
        return bytes;
    }

    private void sendResponseMessageToClient(HttpExchange exchange, String messageBody, int code) throws IOException {
        byte bytes [] = bytefy(messageBody);
        exchange.sendResponseHeaders(code, bytes.length);
        OutputStream oStream = exchange.getResponseBody();
        oStream.write(bytes);
        oStream.close();
    }

    private void processMessage(String text) {
        messages.add(text);
        ChatServer.log("POST", "Message processed");
    }
}
