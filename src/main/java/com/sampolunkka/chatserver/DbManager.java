package com.sampolunkka.chatserver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;


public class DbManager {

    private String operation = "DbManager";
    private String logBody = "";
    private String url = "";


    public DbManager(String url) {
        this.url = url;
        createEventLog();
        createMessageLog();
        createUsersTable();
    }

    private void createTable(String sql) {

        operation = "createTable";

        try (Connection conn = DriverManager.getConnection(url)) {
            Statement statement = conn.createStatement();
            statement.execute(sql);
            logBody = "Table created with statement: (\n" + sql + "\n)";
        } catch (SQLException e) {
            logBody = e.toString();
        }

        log(operation, logBody);
    }

    private void createMessageLog() {

        String sql = "CREATE TABLE IF NOT EXISTS messages(\n"
                + "    time_created text,            \n"
                + "    username text NOT NULL,            \n"
                + "    message text NOT NULL              \n"
                + ");";

        try (Connection conn = DriverManager.getConnection(url)) {
			Statement statement = conn.createStatement();
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

        try (Connection conn = DriverManager.getConnection(url)) {
			Statement statement = conn.createStatement();
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

        try (Connection conn = DriverManager.getConnection(url)) {
			Statement statement = conn.createStatement();
			statement.execute(sql);
			
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
    }

    public boolean addUser(String username, String password, String email) {
        
        operation = "DB: addUser";
        String query = "INSERT INTO users (username,  password, email) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(url)) {
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, email);
            ps.executeUpdate();
            log(operation, "register success");
            return true;
        } catch (SQLException e) {
            log(operation, "register fail");
        }

        return false;
    }

    public boolean addMessage (String username, String messageBody, String timestamp) {

        operation = "DB: addMessage";
        System.out.println(username + " " + messageBody + " " + timestamp);
        String query = "INSERT INTO messages (time_created, username, message) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(url)) {
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, timestamp);
            ps.setString(2, username);
            ps.setString(3, messageBody);
            ps.executeUpdate();
            log(operation, "Message added!");
            return true;
        } catch (SQLException e) {
            log(operation, e.toString());
        }
        return false;
    }


    public ArrayList<String> findAllMessages() {
        
        operation = "DB: findAllMessages";

        ArrayList<String> messages  = new ArrayList<String>();
        String messageRow = "";
        String query = "SELECT time_created, username, message FROM messages";
        
        try (Connection conn = DriverManager.getConnection(url)) {
            PreparedStatement ps = conn.prepareStatement(query);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                messageRow += rs.getString(1);
                messageRow += rs.getString(2);
                messageRow += rs.getString(3);
                messageRow += "\n";
                messages.add(messageRow);
            }
        } catch (SQLException e) {
            log(operation, e.toString());
        }
        return messages;
    }


    public boolean compareCredentials(String username, String password) {

        operation = "compareCredentials";

        String query = "SELECT username, password FROM users WHERE username = ?";
        
        try (Connection conn = DriverManager.getConnection(url)) {
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            
            if (rs.getString(1).equals(username)) {
                if (rs.getString(2).equals(password)) {
                    logBody = "password and user match";
                    log("LOGIN", logBody);
                    return true;
                } else logBody = "Invalid password";
            } else logBody = "No match for user: " + username + " in db";
        } catch(Exception e) {
            logBody = e.toString();
        }
        log(operation, logBody);
        return false;
    }

    private void log(String operation, String logBody) {
        ChatServer.log(operation, logBody);
    }
}
