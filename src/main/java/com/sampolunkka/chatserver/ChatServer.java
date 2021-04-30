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
import java.security.spec.InvalidKeySpecException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;

/**
 * Hello world!
 *
 */
public class ChatServer {

    public static DbManager dbManager = new DbManager();

    public static void main(String[] args) throws IOException {
        try {
            
            log("ChatServer", "Entering main loop");
            
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
            log("ChatServer" ,"Starting ChatServer");
            server.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static SSLContext chatServerSSLContext() throws NoSuchAlgorithmException, CertificateException,
            FileNotFoundException, IOException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
        char[] passphrase = "password".toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream("keystore.jks"), passphrase);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        SSLContext ssl = SSLContext.getInstance("TLS");
        ssl.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return ssl;

    }

    public static void log(String operation, String text) {
        dbManager.log(operation, text);
    }

    public static DbManager getDbManager() {
        return dbManager;
    }
}
