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

import org.json.JSONException;
import org.json.JSONObject;

import com.sun.net.httpserver.Headers;

public class ChatHandler implements HttpHandler {

    private String responseBody = "";
    private String operation = "ChatHandler";

    ArrayList<String> messages = new ArrayList<String>();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        int code = 200;
        ChatServer.log(operation, "Starting to handle...");
        try {
            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                code = handleChatMessageFromClient(exchange);
            } else if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                code = handleGetRequestFromClient(exchange);
            } else {
                code = 400;
                responseBody = "Not supported";
                ChatServer.log(operation, responseBody);
            }
        } catch (IOException e) {
            code = 500;
            responseBody = "Error handling the request: " + e.getMessage();
            ChatServer.log(operation, responseBody);
        } catch (Exception e) {
            code = 500;
            responseBody = "Server error: " + e.getMessage();
            ChatServer.log(operation, responseBody);
        }
        if (code < 200 || code >= 400) {
            ChatServer.log(operation, Integer.toString(code) + ": " + responseBody);
        }

        // send response
        byte bytes[] = responseBody.getBytes("UTF-8");
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

    private int handleGetRequestFromClient(HttpExchange exchange) throws IOException {
        int code = 200;

        messages = ChatServer.getDbManager().findAllMessages();

        if (messages.isEmpty()) {
            code = 204;
            ChatServer.log("INFO", "Messages is empty");
        } else {
            ChatServer.log("GET", "Delivering messages to client");
            String messageBody = "";
            for (String message : messages) {
                messageBody += message + "\n";
            }
            responseBody = messageBody;
        }
        return code;
    }

    private int handleChatMessageFromClient(HttpExchange exchange) throws IOException {
        operation = "POST Message";
        ChatServer.log(operation, "Trying to recieve chat message");

        int code = 200;
        Headers headers = exchange.getRequestHeaders();

        int contentLength = 0;
        String contentType = "";

        if (headers.containsKey("Content-Length")) {
            contentLength = Integer.parseInt(headers.get("Content-Length").get(0));
            ChatServer.log(operation, "Chat message contains content length: " + Integer.toString(contentLength));

        } else {
            code = 411;
            ChatServer.log(operation, "No content length in header");
            return code;
        }

        if (headers.containsKey("Content-Type")) {
            contentType = headers.get("Content-Type").get(0);
            ChatServer.log(operation, "Chat message contains key Content-Type");

        } else {
            code = 400;
            responseBody = "No content type in request";
            ChatServer.log(operation, responseBody);
            return code;
        }

        if (contentType.equalsIgnoreCase("application/json")) {

            ChatServer.log(operation, "Content-Type is: " + contentType);

            InputStream iStream = exchange.getRequestBody();
            String text = new BufferedReader(new InputStreamReader(iStream, StandardCharsets.UTF_8)).lines()
                    .collect(Collectors.joining("\n"));

            iStream.close();
            try {
                JSONObject messageObject = new JSONObject(text);
                String messageBody = messageObject.getString("message").trim();
                String username = messageObject.getString("user").trim();
                String timestamp = messageObject.getString("sent").trim();

                if (messageBody.length() > 0 && username.length() > 0 && timestamp.length() > 0) {
                    ChatServer.getDbManager().addMessage(username, messageBody, timestamp);
                } else {
                    responseBody = "Invalid content";
                    ChatServer.log(operation, responseBody);
                }

            } catch (JSONException e) {
                code = 400;
                responseBody = "Invalid message format";
                ChatServer.log(operation, e.toString() + "\n" + text);
            }

        } else {
            code = 411;
            responseBody = "Content-Type must be application/json, is" + contentType;
            ChatServer.log(operation, responseBody);
        }
        return code;
    }

    private byte[] bytefy(String string) throws UnsupportedEncodingException {
        byte bytes[] = string.getBytes("UTF-8");
        return bytes;
    }

    private void sendResponseMessageToClient(HttpExchange exchange, String messageBody, int code) throws IOException {
        byte bytes[] = bytefy(messageBody);
        exchange.sendResponseHeaders(code, bytes.length);
        OutputStream oStream = exchange.getResponseBody();
        oStream.write(bytes);
        oStream.close();
    }
}
