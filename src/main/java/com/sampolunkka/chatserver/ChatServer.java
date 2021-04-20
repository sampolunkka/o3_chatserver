package com.sampolunkka.chatserver;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;

/**
 * Hello world!
 *
 */
public class ChatServer {

    private static String dbUrl = "jdbc:sqlite:chatserver.db";
    private static boolean https = false;


    public static ServerLog serverLog = new ServerLog();
    public static DbManager dbManager = new DbManager(dbUrl);

    public static void main(String[] args) throws IOException {

        try {
            
            log("INFO", "Launching ChatServer...");
            
                HttpsServer server = HttpsServer.create(new InetSocketAddress(8001), 0);
                SSLContext sslContext = chatServerSSLContext();
                
                
                server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                    public void configure(HttpsParameters params) {
                        InetSocketAddress remote = params.getClientAddress();
                        SSLContext c = getSSLContext();
                        SSLParameters sslparams = c.getDefaultSSLParameters();
                        params.setSSLParameters(sslparams);
                    }
                });
            
            ChatAuthenticator auth = new ChatAuthenticator();
            HttpContext chatContext = server.createContext("/chat", new ChatHandler());
            chatContext.setAuthenticator(auth);

            server.createContext("/registration", new RegistrationHandler(auth));

            server.setExecutor(null);
            log("INFO" ,"Starting ChatServer");
            server.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    private static SSLContext chatServerSSLContext() throws NoSuchAlgorithmException, CertificateException,
            FileNotFoundException, IOException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
        char[] passphrase = "password".toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream("C:/Users/sampp/Documents/Koulu/ohj3/o3_chatserver/keystore.jks"), passphrase);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        SSLContext ssl = SSLContext.getInstance("TLS");
        ssl.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return ssl;

    }

    /*
    PBKDF2
    Source: https://medium.com/@kasunpdh/how-to-store-passwords-securely-with-pbkdf2-204487f14e84
    */  
    public static byte[] hashPassword(String password) {

        String saltString = "1234";

        int iterations = 10000;
        int keyLength = 512;

        byte[] saltBytes = saltString.getBytes();
        char[] passwordChars = password.toCharArray();


        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance( "PBKDF2WithHmacSHA512" );
            PBEKeySpec spec = new PBEKeySpec( passwordChars, saltBytes, iterations, keyLength );
            SecretKey key = skf.generateSecret( spec );
            byte[] res = key.getEncoded( );
            return res;
        } catch ( NoSuchAlgorithmException | InvalidKeySpecException e ) {
            throw new RuntimeException( e );
        }
    }

    public static void log(String operation, String text) {
        serverLog.log(operation, text);
    }

    public static void log(User user, String message) {
        serverLog.log(user, message);
    }

    public static DbManager getDbManager() {
        return dbManager;
    }
}
