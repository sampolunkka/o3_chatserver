package com.sampolunkka.chatserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.stream.Collectors;


import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.net.httpserver.Headers;

public class ChatHandler implements HttpHandler {

    private String responseBody;
    private String operation = "";

    ArrayList<String> messages = new ArrayList<String>();

    //  Pyynnön käsittely
    //
    //  GET : kutsuaan GET -pyynnön käsittely
    //  POST : kutsutaan POST -pyynnön käsittely
    //  muutoin ei tuettu
    //
    //  Poikkeustilanteet käsitellään ja lähetetään vastaus
    //
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        
        responseBody = "";
        operation = "ChatHandler: handle";
        int code = 200;
        log(operation, "Starting to handle...");
        
        try {
            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                code = handleChatMessageFromClient(exchange);
            } else if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                code = handleGetRequestFromClient(exchange);
            } else {
                code = 400;
                responseBody = "Not supported";
            }
        } catch (IOException e) {
            code = 500;
            responseBody = "Error handling the request: " + e.getMessage();
            log(operation, responseBody);
        } catch (Exception e) {
            code = 500;
            responseBody = "Server error: " + e.getMessage();
            log(operation, responseBody);
        }
        if (code < 200 || code >= 400) {
            log(operation, Integer.toString(code) + ": " + responseBody);
        }

        byte bytes[] = responseBody.getBytes("UTF-8");
        int length = bytes.length;
        if (length > 0) {
            exchange.sendResponseHeaders(code, bytes.length);
            OutputStream oStream = exchange.getResponseBody();
            oStream.write(bytes);
            oStream.close();
        } else {
            exchange.sendResponseHeaders(code, -1);
        }
    }

    //  GET
    //
    //  Tarkistetaan ylätunnisteesta If-Modified-Since -tunniste
    //       ifModifiedSince on null = ei If-Modified-Since tunnistetta
    //       ifModifiedSince ei null = modified since tunniste
    //  
    //  ifModifiedSince välitetään funtkion parametrina viestin hakufunktiolle
    //  -> hakee viestit, jotka aikaleiman jälkeen
    //
    //  Haetaan viestit tietokannasta, jos tyhjä lista -> lähetetään 204
    //  Muutoin palautetaan viestit JSONArrayn merkkijonoesityksenä
    //

    private int handleGetRequestFromClient(HttpExchange exchange) throws IOException {
        operation = "ChatHandler: get";
        log(operation, "GET request from client");
        int code = 200;

        ZonedDateTime ifModifiedSince = null;

        Headers requestHeaders = exchange.getRequestHeaders();

        if (requestHeaders.containsKey("If-Modified-Since")) {
            try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
            ifModifiedSince = ZonedDateTime.parse(requestHeaders.get("If-Modified-Since").get(0), formatter);
            } catch (Exception e) {
                code = 400;
                responseBody = "Invalid datetime-format in If-Modified-Since header";
                return code;
            }
        }

        JSONArray messages = ChatServer.getDbManager().findAllMessages(ifModifiedSince);

        if (messages.isEmpty()) {
            code = 204;
            log(operation, "Messages is empty");
        } else {
            JSONObject lastModifiedMessage = messages.getJSONObject(messages.length()-1);
            String lastModified = lastModifiedMessage.getString("sent");
            exchange.getResponseHeaders().add("Last-Modified", lastModified);
            responseBody = messages.toString();
            log(operation, "Delivering messages to client");
        }
        return code;
    }


    //  POST
    //  
    //  Tarkistetaan ensin sisällön tyyppi ja pituus ylätunnisteesta
    //  Jos kaikki ok, tehdään tekstistä JSON olio ja parsitaan oliosta viesti ja käyttäjänimi
    //  Talletetaan parsitut merkkijonot tietokantaan
    //
    //  HOX! En lue POST:ista aikaleimaa, koska en luota käyttäjän kelloon. Aikaleima tehdään palvelimen ajasta
    //
    private int handleChatMessageFromClient(HttpExchange exchange) throws IOException {
        operation = "ChatHandler: post";
        log(operation, "POST request from client");

        int code = 200;
        Headers headers = exchange.getRequestHeaders();
        System.out.println(headers.toString());

        int contentLength = 0;
        String contentType = "";
        String logBody = "";

        if (headers.containsKey("Content-Length")) {
            contentLength = Integer.parseInt(headers.get("Content-Length").get(0));
            logBody += "content length: " + Integer.toString(contentLength) + " - ";

        } else {
            code = 411;
            log(operation, "No content length in header");
            return code;
        }

        if (headers.containsKey("Content-Type")) {
            contentType = headers.get("Content-Type").get(0);
        } else {
            code = 400;
            responseBody = "No content type in header";
            log(operation, responseBody);
            return code;
        }

        if (contentType.equalsIgnoreCase("application/json")) {
            log(operation, "Content-Type is: " + contentType
                            + " - Content-Length is: " + Integer.toString(contentLength)
                            );
            InputStream iStream = exchange.getRequestBody();
            String text = new BufferedReader(new InputStreamReader(iStream, StandardCharsets.UTF_8)).lines()
                    .collect(Collectors.joining("\n"));
            iStream.close();
            
            try {
                JSONObject messageObject = new JSONObject(text);
                String messageBody = messageObject.getString("message").trim();
                String username = messageObject.getString("user").trim();

                if (messageBody.length() > 0 && username.length() > 0) {
                    ChatServer.getDbManager().addMessage(username, messageBody);
                    responseBody = "";
                    code  = 200;
                } else {
                    code = 400;
                    responseBody = "Invalid content";
                    log(operation, responseBody);
                }

            } catch (JSONException e) {
                code = 400;
                responseBody = "Invalid message format";
                log(operation, responseBody + "\n" + e.toString());
            }

        } else {
            code = 400;
            responseBody = "Content-Type must be application/json, is" + contentType;
            log(operation, responseBody);
        }
        return code;
    }

    public void log(String operation, String logBody) {
        ChatServer.log(operation, logBody);
    }
}
