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

    private String operation = "RegistrationHandler";
    private String responseBody = "";

    RegistrationHandler(ChatAuthenticator authenticator) {
        auth = authenticator;
    }


    //  Pyynnön käsittely
    //
    //  Jos POST, koitetaan rekisteröidä. Muutoin ei tuettu.
    //  Ehtolausekkeilla käsitellään erilaiset poikkeustilanteet.
    // 
    //  Response lähetetään ehtojen jälkeen
    //  Jos ei ole responseBodyä, lähetetään -1
    //  muutoin lähetetään responseBody byteinä
    //  Statuskoodi määräytyy sen mukaan, mihin ehtoon pyyntö sopii
    //
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        operation = "RegistrationHandler: handle";
        int code = 200;
        log(operation, "Starting to handle registration");
        try {
            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                code = handleRegistrationFromClient(exchange);
            } else {
                code = 400;
                responseBody = "Not supported";
                log(operation, responseBody);
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
            log(Integer.toString(code), responseBody);
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


    //  Rekisteröinti
    //
    //  Tarkistetaan ensin sisällön tyyppi ja pituus ylätunnisteesta
    //  Jos kaikki ok, mennään rekisteröintiin.
    //      Koitetaan muuttaa pyyntö JSON-olioksi, josta parsitaan käyttäjänimi, salasana ja sposti.
    //      Käytetään trim funktiota, koska en hyväksy tyhjiä merkkejä alkuun tai loppuun
    //          Jos kaikki ok, yritetään lisätä tietokantaan
    //          Jos käyttäjä on jo, ei onnistu lisätä
    //  Koodi määräytyy sen mukaan, mikä ei onnistunut ja log
    //
    //  Poikkeustilanteet:
    //  Sisällön pituudesta läähetetään virhe 411, muutoin 400
    //  400 virheistä ainoastaan käyttäjän olemassaolo "kerrotaan" kehottamalla käyttäjää koettamaan toista käyttäjänimeä
    //  Muutoin "invalid user credentials" -> ei ylimääräistä tietoa
    //  Lokiin talletetaan kuitenkin, mitä meni pieleen
    //
    private int handleRegistrationFromClient(HttpExchange exchange) throws IOException {
        operation = "RegistrationHandler: register";
        log(operation, "Trying to recieve registration");

        int code = 200;
        Headers headers = exchange.getRequestHeaders();

        int contentLength = 0;
        String contentType = "";

        if (headers.containsKey("Content-Length")) {
            contentLength = Integer.parseInt(headers.get("Content-Length").get(0));
            log(operation, "Register contains content length: " + Integer.toString(contentLength));

        } else {
            code = 411;
            responseBody = "No length in request header";
            log(operation, responseBody);
        }

        if (headers.containsKey("Content-Type")) {
            contentType = headers.get("Content-Type").get(0);
        } else {
            code = 400;
            responseBody = "No content type in request header";
            log(operation, responseBody);
        }

        if (contentType.equalsIgnoreCase("application/json")) {

            log(operation, "Content type is " + contentType);

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

                if ( username.length() > 0 && password.length() > 0 && email.length() > 0 ) {
                    if (auth.addUser(username, password, email)) {
                        log(operation, username + " registered as user");
                        code = 200;
                    } else {
                        code = 400;
                        responseBody = "Try different username.";
                        log(operation, responseBody);
                    }
                } else {
                    code = 400;
                    responseBody = "Invalid user credentials";
                    log(operation, "Trimmed length < 1");
                }

            } catch (JSONException e) {
                code = 400;
                responseBody = "Invalid user credentials";
                e.printStackTrace();
                log(operation, e.toString());
            }
        } else {
            code = 400;
            responseBody =  "Invalid user credentials";
            log(operation, "invalid content type: " + contentType);
        }

        return code;
    }


    private void log(String operation, String logBody) {
        ChatServer.log(operation, logBody);
    }
}
