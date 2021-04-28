package com.sampolunkka.chatserver;

import java.util.Map;
import java.util.HashMap;
import com.sun.net.httpserver.BasicAuthenticator;

public class ChatAuthenticator extends BasicAuthenticator{

    public ChatAuthenticator() {
        super("chat");
    }

    @Override
    public boolean checkCredentials(String username, String password) {
        return ChatServer.getDbManager().compareCredentials(username, password);
    }

    public boolean addUser(String username, String password, String email) {
        return ChatServer.getDbManager().addUser(username, password, email);
    }
    
}
