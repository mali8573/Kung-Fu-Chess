package server;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Username+password accounts with an ELO rating, persisted in a local SQLite database file.
 * A username that hasn't been seen before is registered automatically on its first login -
 * there's no separate "sign up" step yet, that's fine for this stage.
 *
 * Passwords are never stored in plain text: SHA-256 over (salt + password), where salt is a
 * random value generated once per account and stored alongside the hash.
 */
public class AccountStore {
    private static final int STARTING_ELO = 1200;
    private static final int K_FACTOR = 32;

    private final String jdbcUrl;

    public AccountStore(String dbFilePath) {
        this.jdbcUrl = "jdbc:sqlite:" + dbFilePath;
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("SQLite JDBC driver not on classpath", e);
        }
        createTableIfMissing();
    }

    private void createTableIfMissing() {
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS accounts (" +
                    "username TEXT PRIMARY KEY, " +
                    "salt TEXT NOT NULL, " +
                    "password_hash TEXT NOT NULL, " +
                    "elo INTEGER NOT NULL)");
        } catch (SQLException e) {
            throw new IllegalStateException("Could not initialize accounts database", e);
        }
    }

    public static final class LoginResult {
        public final boolean success;
        public final String reason; // null on success
        public final int elo;

        private LoginResult(boolean success, String reason, int elo) {
            this.success = success;
            this.reason = reason;
            this.elo = elo;
        }
    }

    public static final class EloUpdate {
        public final int winnerElo;
        public final int loserElo;

        private EloUpdate(int winnerElo, int loserElo) {
            this.winnerElo = winnerElo;
            this.loserElo = loserElo;
        }
    }

    /** New username -> registers it with this password and starting ELO. Existing username ->
     *  succeeds only if the password matches what was stored at registration. */
    public synchronized LoginResult login(String username, String password) {
        try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
            try (PreparedStatement select = conn.prepareStatement(
                    "SELECT salt, password_hash, elo FROM accounts WHERE username = ?")) {
                select.setString(1, username);
                try (ResultSet rs = select.executeQuery()) {
                    if (rs.next()) {
                        String salt = rs.getString("salt");
                        String expectedHash = rs.getString("password_hash");
                        int elo = rs.getInt("elo");
                        if (hash(salt, password).equals(expectedHash)) {
                            return new LoginResult(true, null, elo);
                        }
                        return new LoginResult(false, "wrong_password", 0);
                    }
                }
            }

            String salt = randomSalt();
            String passwordHash = hash(salt, password);
            try (PreparedStatement insert = conn.prepareStatement(
                    "INSERT INTO accounts (username, salt, password_hash, elo) VALUES (?, ?, ?, ?)")) {
                insert.setString(1, username);
                insert.setString(2, salt);
                insert.setString(3, passwordHash);
                insert.setInt(4, STARTING_ELO);
                insert.executeUpdate();
            }
            return new LoginResult(true, null, STARTING_ELO);
        } catch (SQLException e) {
            return new LoginResult(false, "server_error", 0);
        }
    }

    /** Standard ELO update (K=32) for a finished game - this game has no draws. */
    public synchronized EloUpdate applyGameResult(String winnerUsername, String loserUsername) {
        try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
            int winnerElo = getElo(conn, winnerUsername);
            int loserElo = getElo(conn, loserUsername);

            double expectedWinner = 1.0 / (1.0 + Math.pow(10, (loserElo - winnerElo) / 400.0));
            int newWinnerElo = (int) Math.round(winnerElo + K_FACTOR * (1 - expectedWinner));
            int newLoserElo = (int) Math.round(loserElo - K_FACTOR * (1 - expectedWinner));

            setElo(conn, winnerUsername, newWinnerElo);
            setElo(conn, loserUsername, newLoserElo);
            return new EloUpdate(newWinnerElo, newLoserElo);
        } catch (SQLException e) {
            System.out.println("Could not update ELO: " + e.getMessage());
            return new EloUpdate(STARTING_ELO, STARTING_ELO);
        }
    }

    private int getElo(Connection conn, String username) throws SQLException {
        try (PreparedStatement st = conn.prepareStatement("SELECT elo FROM accounts WHERE username = ?")) {
            st.setString(1, username);
            try (ResultSet rs = st.executeQuery()) {
                return rs.next() ? rs.getInt("elo") : STARTING_ELO;
            }
        }
    }

    private void setElo(Connection conn, String username, int elo) throws SQLException {
        try (PreparedStatement st = conn.prepareStatement("UPDATE accounts SET elo = ? WHERE username = ?")) {
            st.setInt(1, elo);
            st.setString(2, username);
            st.executeUpdate();
        }
    }

    private static String randomSalt() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return toHex(bytes);
    }

    private static String hash(String salt, String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] hashed = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return toHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e); // SHA-256 is always available on a real JVM
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
