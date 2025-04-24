package net.hywave.snake.db;

import net.hywave.snake.Engine;
import net.hywave.snake.db.player.PlayerScore;

import org.bukkit.entity.Player;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {
    private final Engine plugin;
    private Connection connection;
    private final String dbPath;
    
    public DatabaseManager(Engine plugin) {
        this.plugin = plugin;
        this.dbPath = plugin.getDataFolder() + File.separator + "playerdata.db";
        
        setupDatabase();
    }
    
    private void setupDatabase() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdir();
        }
        
        try {
            if (connection != null && !connection.isClosed()) {
                return;
            }
            
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            
            createTables();
            
        } catch (SQLException | ClassNotFoundException e) {
        }
    }
    
    private void createTables() {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS players (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "name VARCHAR(16) NOT NULL, " +
                    "coins INTEGER DEFAULT 0, " +
                    "last_seen BIGINT NOT NULL)");
            
            statement.execute("CREATE TABLE IF NOT EXISTS game_history (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "player_uuid VARCHAR(36) NOT NULL, " +
                    "score INTEGER NOT NULL, " +
                    "length INTEGER NOT NULL, " +
                    "duration_seconds INTEGER NOT NULL, " +
                    "timestamp BIGINT NOT NULL, " +
                    "FOREIGN KEY (player_uuid) REFERENCES players(uuid))");
            
            statement.execute("CREATE INDEX IF NOT EXISTS idx_game_history_player_uuid ON game_history(player_uuid)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_game_history_score ON game_history(score)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_game_history_timestamp ON game_history(timestamp)");
        } catch (SQLException e) {
        }
    }
    
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Database -> CLOSED");
            }
        } catch (SQLException e) {
        }
    }
    
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            }
        } catch (SQLException | ClassNotFoundException e) {
        }
        return connection;
    }
    
    public void createOrUpdatePlayer(Player player) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT OR REPLACE INTO players (uuid, name, coins, last_seen) " +
                    "VALUES (?, ?, COALESCE((SELECT coins FROM players WHERE uuid = ?), 0), ?)");
            
            ps.setString(1, player.getUniqueId().toString());
            ps.setString(2, player.getName());
            ps.setString(3, player.getUniqueId().toString());
            ps.setLong(4, System.currentTimeMillis());
            
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
        }
    }
    
    public void recordGameResult(UUID playerUUID, int score, int length, int durationSeconds) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO game_history (player_uuid, score, length, duration_seconds, timestamp) " +
                    "VALUES (?, ?, ?, ?, ?)");
            
            ps.setString(1, playerUUID.toString());
            ps.setInt(2, score);
            ps.setInt(3, length);
            ps.setInt(4, durationSeconds);
            ps.setLong(5, System.currentTimeMillis());
            
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
        }
    }
    
    public int getPlayerCoins(UUID playerUUID) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT coins FROM players WHERE uuid = ?");
            
            ps.setString(1, playerUUID.toString());
            ResultSet rs = ps.executeQuery();
            
            int coins = 0;
            if (rs.next()) {
                coins = rs.getInt("coins");
            }
            
            rs.close();
            ps.close();
            
            return coins;
        } catch (SQLException e) {
            return 0;
        }
    }
    
    public void addPlayerCoins(UUID playerUUID, int amount) {
        if (amount <= 0) return;
        
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "UPDATE players SET coins = coins + ? WHERE uuid = ?");
            
            ps.setInt(1, amount);
            ps.setString(2, playerUUID.toString());
            
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
        }
    }
    
    public boolean removePlayerCoins(UUID playerUUID, int amount) {
        if (amount <= 0) return true;
        
        try {
            int currentCoins = getPlayerCoins(playerUUID);
            if (currentCoins < amount) {
                return false;
            }
            
            PreparedStatement ps = connection.prepareStatement(
                    "UPDATE players SET coins = coins - ? WHERE uuid = ?");
            
            ps.setInt(1, amount);
            ps.setString(2, playerUUID.toString());
            
            ps.executeUpdate();
            ps.close();
            
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
    
    public boolean playerHasEnoughCoins(UUID playerUUID, int amount) {
        return getPlayerCoins(playerUUID) >= amount;
    }
    
    public void setPlayerCoins(UUID playerUUID, int amount) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "UPDATE players SET coins = ? WHERE uuid = ?");
            
            ps.setInt(1, amount);
            ps.setString(2, playerUUID.toString());
            
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
        }
    }
    
    public List<PlayerScore> getTopScores(int limit) {
        List<PlayerScore> topScores = new ArrayList<>();
        
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT p.name, g.score, g.length, g.duration_seconds, g.timestamp " +
                    "FROM game_history g " +
                    "JOIN players p ON g.player_uuid = p.uuid " +
                    "ORDER BY g.score DESC " +
                    "LIMIT ?");
            
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                PlayerScore score = new PlayerScore(
                        rs.getString("name"),
                        rs.getInt("score"),
                        rs.getInt("length"),
                        rs.getInt("duration_seconds"),
                        rs.getLong("timestamp")
                );
                topScores.add(score);
            }
            
            rs.close();
            ps.close();
        } catch (SQLException e) {
        }
        
        return topScores;
    }
    
    public List<PlayerScore> getTopLengths(int limit) {
        List<PlayerScore> topLengths = new ArrayList<>();
        
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT p.name, g.score, g.length, g.duration_seconds, g.timestamp " +
                    "FROM game_history g " +
                    "JOIN players p ON g.player_uuid = p.uuid " +
                    "ORDER BY g.length DESC " +
                    "LIMIT ?");
            
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                PlayerScore score = new PlayerScore(
                        rs.getString("name"),
                        rs.getInt("score"),
                        rs.getInt("length"),
                        rs.getInt("duration_seconds"),
                        rs.getLong("timestamp")
                );
                topLengths.add(score);
            }
            
            rs.close();
            ps.close();
        } catch (SQLException e) {
        }
        
        return topLengths;
    }
    
    public List<PlayerScore> getFastestGames(int minScore, int limit) {
        List<PlayerScore> fastestGames = new ArrayList<>();
        
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT p.name, g.score, g.length, g.duration_seconds, g.timestamp " +
                    "FROM game_history g " +
                    "JOIN players p ON g.player_uuid = p.uuid " +
                    "WHERE g.score >= ? " +
                    "ORDER BY g.duration_seconds ASC " +
                    "LIMIT ?");
            
            ps.setInt(1, minScore);
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                PlayerScore score = new PlayerScore(
                        rs.getString("name"),
                        rs.getInt("score"),
                        rs.getInt("length"),
                        rs.getInt("duration_seconds"),
                        rs.getLong("timestamp")
                );
                fastestGames.add(score);
            }
            
            rs.close();
            ps.close();
        } catch (SQLException e) {
        }
        
        return fastestGames;
    }
    
    public Map<String, Integer> getPlayerCoinsRanking(int limit) {
        Map<String, Integer> playersCoins = new HashMap<>();
        
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT name, coins FROM players " +
                    "ORDER BY coins DESC " +
                    "LIMIT ?");
            
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                playersCoins.put(rs.getString("name"), rs.getInt("coins"));
            }
            
            rs.close();
            ps.close();
        } catch (SQLException e) {
        }
        
        return playersCoins;
    }
    
    public List<PlayerScore> getRecentGames(UUID playerUUID, int limit) {
        List<PlayerScore> recentGames = new ArrayList<>();
        
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT p.name, g.score, g.length, g.duration_seconds, g.timestamp " +
                    "FROM game_history g " +
                    "JOIN players p ON g.player_uuid = p.uuid " +
                    "WHERE g.player_uuid = ? " +
                    "ORDER BY g.timestamp DESC " +
                    "LIMIT ?");
            
            ps.setString(1, playerUUID.toString());
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                PlayerScore score = new PlayerScore(
                        rs.getString("name"),
                        rs.getInt("score"),
                        rs.getInt("length"),
                        rs.getInt("duration_seconds"),
                        rs.getLong("timestamp")
                );
                recentGames.add(score);
            }
            
            rs.close();
            ps.close();
        } catch (SQLException e) {
        }
        
        return recentGames;
    }
    
    public int getPlayerScoreRank(UUID playerUUID) {
        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT COUNT(*) + 1 AS rank FROM " +
                "(SELECT player_uuid, MAX(score) as max_score FROM game_history GROUP BY player_uuid) AS scores " +
                "WHERE max_score > (SELECT COALESCE(MAX(score), 0) FROM game_history WHERE player_uuid = ?)"
            );
            
            stmt.setString(1, playerUUID.toString());
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                PreparedStatement checkStmt = conn.prepareStatement(
                        "SELECT COUNT(*) FROM game_history WHERE player_uuid = ?");
                checkStmt.setString(1, playerUUID.toString());
                
                ResultSet checkRs = checkStmt.executeQuery();
                if (checkRs.next() && checkRs.getInt(1) > 0) {
                    int rank = rs.getInt("rank");
                    checkRs.close();
                    checkStmt.close();
                    rs.close();
                    stmt.close();
                    return rank;
                }
                checkRs.close();
                checkStmt.close();
            }
            rs.close();
            stmt.close();
            
            return 0;
        } catch (SQLException e) {
            return 0;
        }
    }
} 