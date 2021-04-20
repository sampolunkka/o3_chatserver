package com.sampolunkka.chatserver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


public class DbManager {

    private String url = "";


    public DbManager(String url) {
        this.url = url;
        createEventLog();
        createMessageLog();
        createUsersTable();
    }

    private Connection connect() {

        String operation = "dbConnect";
        String logBody = "";

        Connection connection = null;

        try {
            connection = DriverManager.getConnection(url);
            
            if (connection != null) {
                logBody = "new db created";
            
            } else {
                logBody = "connect success";

            }

        } catch (SQLException e) {
            logBody = e.toString();
        }

        ChatServer.log(operation, logBody);
        
        return connection;
    }

    private void createTable(String sql) {

        String operation = "createTable";
        String logBody = "";

        try {
            Statement statement = connect().createStatement();
            statement.execute(sql);
            logBody = "Table created with statement: (\n" + sql + "\n)";
        } catch (SQLException e) {
            logBody = e.toString();
        }

        ChatServer.log(operation, logBody);
    }

    private void createMessageLog() {

        String sql = "CREATE TABLE IF NOT EXISTS messages(\n"
                + "    id integer PRIMARY KEY,            \n"
                + "    time_created TIMESTAMP,            \n"
                + "    username text NOT NULL,            \n"
                + "    message text NOT NULL              \n"
                + ");";

        try {
			Statement statement = this.connect().createStatement();
			statement.execute(sql);
			
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
    }

    private void createEventLog() {

        String sql = "CREATE TABLE IF NOT EXISTS events(\n"
                + "    id integer PRIMARY KEY,          \n"
                + "    time_created TIMESTAMP NOT NULL, \n"
                + "    operation text NOT NULL,         \n"
                + "    username text NOT NULL,          \n"
                + "    message text NOT NULL            \n"
                + ");";

        try {
			Statement statement = this.connect().createStatement();
			statement.execute(sql);
			
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
    }

    private void createUsersTable() {

        String sql = "CREATE TABLE IF NOT EXISTS users(\n"
                + "username text PRIMARY KEY,   \n"
                + "password text NOT NULL,       \n"
                + "email text NOT NULL           \n"
                + ");";

        try {
			Statement statement = this.connect().createStatement();
			statement.execute(sql);
			
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
    }

    public boolean addUser(String username, String password, String email) {
        
        String query = "INSERT INTO users (username,  password, email) VALUES (?, ?, ?)";

        try {
            PreparedStatement ps = connect().prepareStatement(query);
            ps.setString(1, username);
            ps.setString(2, ChatServer.hashPassword(password).toString());
            ps.setString(3, email);
            ps.executeUpdate();
            ChatServer.log("REGISTER", "register success");
        } catch (SQLException e) {
            ChatServer.log("REGISTER", "register fail");
        }

        return false;
    }

    public boolean compareCredentials(String username, String password) {

        String query = "SELECT username, password FROM users WHERE username = ?";
        
        try {
            PreparedStatement ps = connect().prepareStatement(query);
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery(query);
            if (rs.getString(1).equals(username)) {
                if (rs.getString(2).equals(ChatServer.hashPassword(password).toString())) {
                    //login success
                    return true;
                }
            }

        } catch(Exception e) {

        }

        return false;
    }
}
