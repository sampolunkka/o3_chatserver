package com.sampolunkka.chatserver;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.sql.*;

public class ServerLog {

    private String url = "";

    private ArrayList<String> serverLog;
    
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    public ServerLog() {
        initServerLog();
    }

    private void initServerLog() {
        serverLog = new ArrayList<String>();
    }

    public String log(String operation, String text) {
        String entry = "<" + operation + "> " + text;
        serverLog.add(entry);
        System.out.println(entry);
        return text;
    }

    public void log(User user, String text) {

    }

    public void logEvent(String operation, String text) {

    }

    public void logMessage(User user, String message) {

    }

    @Override
    public String toString() {
        String output = "";
        for(String entry : serverLog) {
            output += entry + "\n";
        }
        return output;
    }

    
}
