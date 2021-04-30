package com.sampolunkka.chatserver;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import org.apache.commons.codec.digest.Crypt;
import org.json.JSONArray;
import org.json.JSONObject;


public class DbManager {

    private String operation = "DbManager";
    private String logBody = "";
    private String url = "";
    private SecureRandom secureRandom = new SecureRandom();

    public DbManager() {
        this.url = "jdbc:sqlite:chatserver.db";
        createEventLog();
        createMessageLog();
        createUsersTable();
    }

    private void createMessageLog() {

        String sql = "CREATE TABLE IF NOT EXISTS messages(\n"
                + "    id integer PRIMARY KEY,            \n"
                + "    time_created integer NOT NULL,            \n"
                + "    username text NOT NULL,            \n"
                + "    message text NOT NULL              \n"
                + ");";

        try (Connection conn = DriverManager.getConnection(url)) {
			Statement statement = conn.createStatement();
			statement.execute(sql);
            conn.close();
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
    }

    private void createEventLog() {

        String sql = "CREATE TABLE IF NOT EXISTS events(\n"
                + "    id integer PRIMARY KEY,          \n"
                + "    time_created TIMESTAMP NOT NULL, \n"
                + "    operation text NOT NULL,         \n"
                + "    description text NOT NULL            \n"
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
                + "email text NOT NULL,           \n"
                + "salt text NOT NULL              \n"
                + ");";

        try (Connection conn = DriverManager.getConnection(url)) {
			Statement statement = conn.createStatement();
			statement.execute(sql);
			
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
    }


    //  Lisää käyttäjän tietokantaan
    //  username, hashedPassword, email, salt
    //
    //  Jos käyttäjänimi on jo tietokannassa tulee virhe SQLException 
    //
    //  Onnistunut lisäys palautta true;
    //  epäonnistunut false;
    //
    public boolean addUser(String username, String password, String email) {
        
        operation = "DB: addUser";
        String query = "INSERT INTO users (username,  password, email, salt) VALUES (?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(url)) {
            PreparedStatement ps = conn.prepareStatement(query);
            
            ps.setString(1, username);

            byte bytes[] = new byte[13];
            secureRandom.nextBytes(bytes);
            String saltBytes = new String(Base64.getEncoder().encode(bytes));
            String salt = "$6$" + saltBytes;
            String hashedPassword = Crypt.crypt(password, salt);

            ps.setString(2, hashedPassword);
            ps.setString(3, email);
            ps.setString(4, salt);
            ps.executeUpdate();
            
            conn.close();
            
            log(operation, "register success");
            
            return true;
        } catch (SQLException e) {
            log(operation, "register fail");
        }
        return false;
    }


    //  Lisätään viesti tietokantaan
    //  
    //  Viestistä otetaan käyttäjänimi ja viestin sisältö merkkijonona
    //  Aikaleimaksi talletetaan palvelimen aikaleima (UTC)
    //  En näe mitään järkeä tallettaa clientin lähettämää aikaleimaa tietokantaani, vaikka se sellaisen minulle lähettäisikin
    //
    public boolean addMessage (String username, String messageBody) {

        operation = "DB: addMessage";
        
        long timestamp = getCurrentServerUnixTime();
        String query = "INSERT INTO messages (time_created, username, message) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(url)) {
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setLong(1, timestamp);
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


    //  Etsitään kaikki viestit ennen tiettyä ajankohtaa
    //
    //  Valitaan SELECT:llä tietokannasta viestit, joiden aikaleima on isompi kuin parametrin time
    //  Jos parametri time = null, cutoff aika on 0 -> haetaan kaikki viestit
    //  
    //  Palautetaan JSONArray messages, johon kaikki viestit on talletettu JSONObject -muodossa
    //
    public JSONArray findAllMessages(ZonedDateTime time) {

        long cutoffTime = 0;
        if(time != null) {
            cutoffTime = time.toInstant().toEpochMilli();
        }

        JSONArray messages = new JSONArray();
        operation = "DB: findAllMessages(cutoff)";

        String query = "SELECT username, message, time_created FROM messages WHERE time_created > ? ORDER BY time_created ASC";
        
        try (Connection conn = DriverManager.getConnection(url)) {
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setLong(1, cutoffTime);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                JSONObject message = new JSONObject();
                message.put("user", rs.getString(1));
                message.put("message", rs.getString(2));
                message.put("sent", convertUnixTimeToString(rs.getLong(3)));
                messages.put(message);
            }
            conn.close();
        } catch (SQLException e) {
            log(operation, e.toString());
        }
        return messages;
    }


    //  Vertaillaan käyttämän antamia credejä tietokannasta läytyviin
    // 
    //  Tarkistetaan onko käyttäjää ollenkaan
    //  Jos on, tarkistetaan täsmäävätkö hashatut salasanat
    //  
    //  Palauttaa true jos täsmäävät
    //  False, jos jokin steppi ei onnistunut
    //
    public boolean compareCredentials(String username, String password) {

        operation = "compareCredentials";

        String query = "SELECT username, password FROM users WHERE username = ?";
        
        try (Connection conn = DriverManager.getConnection(url)) {
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()){ log(operation, "No match for user: " + username + " in db"); return false;}
            if (rs.getString(1).equals(username)) {
                String hashedPassword = rs.getString(2);
                if (hashedPassword.equals(Crypt.crypt(password, hashedPassword))) {
                    conn.close();
                    logBody = username + " login";
                    log(operation, logBody);
                    return true;
                } else logBody = "Invalid password";
            }
        } catch(Exception e) {
            logBody = e.toString();
        }
        log(operation, logBody);
        return false;
    }


    //  Lisätään palvelimen tapahtuma tietokantaan
    //
    //  Tietokantaan ylläpidetään palvelimen tapahtumista debugausta ja virheenmääritystä varten
    //
    private boolean addEvent(String op, String description) {
        long timestamp = getCurrentServerUnixTime();
        String query = "INSERT INTO events (time_created, operation, description) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(url)) {
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setLong(1, timestamp);
            ps.setString(2, op);
            ps.setString(3, description);
            ps.executeUpdate();
            conn.close();
            return true;
        } catch (SQLException e) {
            System.out.println("addEvent: ");
            e.printStackTrace();
        }
        return false;
    }
    

    //  Aika long muodossa aikojen tallettamista ja vertailua varten
    //
    private long getCurrentServerUnixTime() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        long unixTime = now.toInstant().toEpochMilli();
        return unixTime;
    }


    //  Long aikojen muuntamista merkkijonoksi (esim GET:iä varten)
    //
    private String convertUnixTimeToString(long unixTime) {
        ZonedDateTime time = ZonedDateTime.ofInstant(Instant.ofEpochMilli(unixTime), ZoneId.of("UTC"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        String dateText = time.format(formatter);
        return dateText;
    }

    public void log(String operation, String logBody) {
        System.out.println("<"+operation+"> " + logBody);
        addEvent(operation, logBody);
    }
}
