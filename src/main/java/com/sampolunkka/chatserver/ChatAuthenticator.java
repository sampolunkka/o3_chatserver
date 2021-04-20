package com.sampolunkka.chatserver;

import java.util.Map;
import java.util.HashMap;
import com.sun.net.httpserver.BasicAuthenticator;

public class ChatAuthenticator extends BasicAuthenticator{

    private Map<String, String> users = null;

    public ChatAuthenticator() {
        super("chat");
        users = new HashMap<String, String>();
        users.put("dummy", "passwd");
        //TODO Auto-generated constructor stub
    }

    @Override
    public boolean checkCredentials(String username, String password) {
        return ChatServer.getDbManager().compareCredentials(username, password);
    }

    public boolean addUser(String username, String password, String email) {

        ChatServer.dbManager.addUser(username, password, email);

        return false;
    }
    
}
