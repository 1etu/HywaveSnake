package net.hywave.snake.leaderboard;

import lombok.Getter;
import net.hywave.snake.Engine;
import net.hywave.snake.db.DatabaseManager;
import net.hywave.snake.db.player.PlayerScore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LeaderboardManager {
    private final Engine plugin;
    
    @Getter
    private final DatabaseManager databaseManager;
    
    private static final int DEFAULT_LIMIT = 10;
    private static final int MINIMUM_SCORE_FOR_FASTEST = 5;
    
    public enum LeaderboardType {
        HIGHEST_SCORE("Highest Score", ChatColor.GOLD),
        LONGEST_SNAKE("Longest Snake", ChatColor.GREEN),
        FASTEST_GAMES("Fastest Games", ChatColor.AQUA),
        MOST_COINS("Most Coins", ChatColor.YELLOW);
        
        private final String displayName;
        private final ChatColor color;
        
        LeaderboardType(String displayName, ChatColor color) {
            this.displayName = displayName;
            this.color = color;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public ChatColor getColor() {
            return color;
        }
    }
    
    public LeaderboardManager(Engine plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }
    
    public void displayLeaderboard(Player player, LeaderboardType type) {
        displayLeaderboard(player, type, DEFAULT_LIMIT);
    }
    
    public void displayLeaderboard(Player player, LeaderboardType type, int limit) {
        player.sendMessage(ChatColor.DARK_GRAY + "═════════ " + type.getColor() + type.getDisplayName() + 
                           ChatColor.DARK_GRAY + " ═════════");
        
        CompletableFuture.runAsync(() -> {
            switch (type) {
                case HIGHEST_SCORE:
                    List<PlayerScore> topScores = databaseManager.getTopScores(limit);
                    
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (topScores.isEmpty()) {
                            player.sendMessage(ChatColor.GRAY + "No scores recorded yet.");
                        } else {
                            int rank = 1;
                            for (PlayerScore score : topScores) {
                                String message = formatRank(rank) + " " + formatPlayerName(score.getPlayerName()) + 
                                        " - " + formatScore("Score", score.getScore(), ChatColor.GOLD) + " " +
                                        formatScore("Length", score.getLength(), ChatColor.GREEN) + " " +
                                        formatTime(score.getFormattedTime());
                                player.sendMessage(message);
                                rank++;
                            }
                        }
                    });
                    break;
                    
                case LONGEST_SNAKE:
                    List<PlayerScore> topLengths = databaseManager.getTopLengths(limit);
                    
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (topLengths.isEmpty()) {
                            player.sendMessage(ChatColor.GRAY + "No scores recorded yet.");
                        } else {
                            int rank = 1;
                            for (PlayerScore score : topLengths) {
                                String message = formatRank(rank) + " " + formatPlayerName(score.getPlayerName()) + 
                                        " - " + formatScore("Length", score.getLength(), ChatColor.GREEN) + " " +
                                        formatScore("Score", score.getScore(), ChatColor.GOLD) + " " +
                                        formatTime(score.getFormattedTime());
                                player.sendMessage(message);
                                rank++;
                            }
                        }
                    });
                    break;
                    
                case FASTEST_GAMES:
                    List<PlayerScore> fastestGames = databaseManager.getFastestGames(MINIMUM_SCORE_FOR_FASTEST, limit);
                    
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (fastestGames.isEmpty()) {
                            player.sendMessage(ChatColor.GRAY + "No scores recorded yet.");
                        } else {
                            int rank = 1;
                            for (PlayerScore score : fastestGames) {
                                String message = formatRank(rank) + " " + formatPlayerName(score.getPlayerName()) + 
                                        " - " + formatTime(score.getFormattedTime()) + " " +
                                        formatScore("Score", score.getScore(), ChatColor.GOLD) + " " +
                                        formatScore("Length", score.getLength(), ChatColor.GREEN);
                                player.sendMessage(message);
                                rank++;
                            }
                        }
                    });
                    break;
                    
                case MOST_COINS:
                    var coinRanking = databaseManager.getPlayerCoinsRanking(limit);
                    
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (coinRanking.isEmpty()) {
                            player.sendMessage(ChatColor.GRAY + "No players with coins yet.");
                        } else {
                            int rank = 1;
                            for (var entry : coinRanking.entrySet()) {
                                String message = formatRank(rank) + " " + formatPlayerName(entry.getKey()) + 
                                        " - " + formatScore("Coins", entry.getValue(), ChatColor.GOLD);
                                player.sendMessage(message);
                                rank++;
                            }
                        }
                    });
                    break;
            }
        });
        
        player.sendMessage(ChatColor.DARK_GRAY + "═════════════════════════════════");
    }
    
    public void displayPlayerStats(Player player, UUID targetUUID) {
        Player target = Bukkit.getPlayer(targetUUID);
        String playerName = target != null ? target.getName() : "Unknown";
        
        player.sendMessage(ChatColor.DARK_GRAY + "═════════ " + ChatColor.YELLOW + "Player Stats: " + 
                           ChatColor.WHITE + playerName + ChatColor.DARK_GRAY + " ═════════");
        
        CompletableFuture.runAsync(() -> {
            int coins = databaseManager.getPlayerCoins(targetUUID);
            List<PlayerScore> recentGames = databaseManager.getRecentGames(targetUUID, 5);
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage(ChatColor.YELLOW + "Coins: " + ChatColor.GOLD + coins);
                player.sendMessage("");
                player.sendMessage(ChatColor.YELLOW + "Recent Games:");
                
                if (recentGames.isEmpty()) {
                    player.sendMessage(ChatColor.GRAY + "No games recorded yet.");
                } else {
                    for (PlayerScore score : recentGames) {
                        String message = ChatColor.GRAY + score.getFormattedDate() + " " +
                                formatScore("Score", score.getScore(), ChatColor.GOLD) + " " +
                                formatScore("Length", score.getLength(), ChatColor.GREEN) + " " +
                                formatTime(score.getFormattedTime());
                        player.sendMessage(message);
                    }
                }
                
                player.sendMessage(ChatColor.DARK_GRAY + "═════════════════════════════════");
            });
        });
    }
    
    private String formatRank(int rank) {
        ChatColor color;
        switch (rank) {
            case 1:
                color = ChatColor.GOLD;
                break;
            case 2:
                color = ChatColor.GRAY;
                break;
            case 3:
                color = ChatColor.DARK_RED;
                break;
            default:
                color = ChatColor.WHITE;
                break;
        }
        
        return color + "#" + rank;
    }
    
    private String formatPlayerName(String name) {
        return ChatColor.WHITE + name;
    }
    
    private String formatScore(String label, int value, ChatColor color) {
        return ChatColor.GRAY + label + ": " + color + value;
    }
    
    private String formatTime(String time) {
        return ChatColor.GRAY + "Time: " + ChatColor.AQUA + time;
    }
    
    
    public int getPlayerPosition(UUID playerUUID) {
        return databaseManager.getPlayerScoreRank(playerUUID);
    }
} 