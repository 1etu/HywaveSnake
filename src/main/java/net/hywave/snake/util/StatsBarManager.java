package net.hywave.snake.util;

import net.hywave.snake.Engine;
import net.hywave.snake.model.GameSession;
import net.hywave.snake.model.PowerUpType;
import net.hywave.snake.state.PlayerState;
import net.hywave.snake.leaderboard.LeaderboardManager;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StatsBarManager {
    private static final Map<UUID, BossBar> tutBars = new HashMap<>();
    
    public static void displayMainMenuBar(Player player) {
        try {
            Engine engine = Engine.getInstance();

            int coins = 0;
            int position = 0;
            
            if (engine != null) {
                if (engine.getEconomyManager() != null) {
                    coins = engine.getEconomyManager().getCoins(player);
                }
                
                if (engine.getLeaderboardManager() != null) {
                    position = engine.getLeaderboardManager().getPlayerPosition(player.getUniqueId());
                }
            }
            
            StringBuilder actionBar = new StringBuilder();
            
            if (position > 0) {
                actionBar.append(ChatColor.AQUA)
                       .append("Position: #")
                       .append(position);
            } else {
                actionBar.append(ChatColor.AQUA)
                       .append("Position: -");
            }
            
            actionBar.append(ChatColor.DARK_GRAY).append(" • ");            
            actionBar.append(ChatColor.GOLD)
                   .append("🪙 Coins: ")
                   .append(coins);
            
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, 
                    new TextComponent(actionBar.toString()));
            
            showTutorialBossBar(player, PlayerState.MAIN_MENU);
        } catch (Exception e) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, 
                    new TextComponent(ChatColor.GOLD + "🪙 Coins: ??"));
            
            showTutorialBossBar(player, PlayerState.MAIN_MENU);
            
            if (Engine.getInstance() != null) {
            }
        }
    }
    
    public static void showTutorialBossBar(Player player, PlayerState state) {
        UUID playerId = player.getUniqueId();
        BossBar bar = tutBars.get(playerId);
        
        if (bar == null) {
            bar = Bukkit.createBossBar("", BarColor.WHITE, BarStyle.SOLID);
            tutBars.put(playerId, bar);
        }
        
        switch (state) {
            case MAIN_MENU:
                bar.setTitle("§eLeft-Click to Start • §7Right-Click for Settings • §6/shop to buy cosmetics");
                bar.setColor(BarColor.YELLOW);
                bar.setProgress(1.0);
                break;
            case PLAYING:
                bar.setTitle("§aUse WASD or Arrow Keys to control your snake");
                bar.setColor(BarColor.GREEN);
                bar.setProgress(1.0);
                break;
            case GAME_OVER:
                bar.setTitle("§cGame Over! §eClick to Play Again • §6/shop to spend your coins on cosmetics");
                bar.setColor(BarColor.RED);
                bar.setProgress(1.0);
                break;
            case LEADERBOARD:
                bar.setTitle("§bLeaderboard §7• Click to toggle between leaderboard types • §eClick to return");
                bar.setColor(BarColor.BLUE);
                bar.setProgress(1.0);
                break;
            case SETTINGS:
                bar.setTitle("§6Settings §7• Click to toggle settings • §eClick to return");
                bar.setColor(BarColor.YELLOW);
                bar.setProgress(1.0);
                break;
            default:
                bar.setTitle("");
                bar.setProgress(0);
                bar.removePlayer(player);
                return;
        }
        
        bar.addPlayer(player);
    }
    
    public static void removeTutorialBossBar(Player player) {
        UUID playerId = player.getUniqueId();
        BossBar bar = tutBars.remove(playerId);
        
        if (bar != null) {
            bar.removeAll();
        }
    }
    
    public static void displayGameStats(Player player, GameSession session) {
        if (session == null || player == null) return;
        
        StringBuilder statsBar = new StringBuilder();
        
        long elapsedSeconds = Duration.between(session.getStartTime(), Instant.now()).getSeconds();
        long minutes = elapsedSeconds / 60;
        long seconds = elapsedSeconds % 60;
        statsBar.append(ChatColor.YELLOW)
               .append(String.format("%02d:%02d", minutes, seconds))
               .append(ChatColor.GRAY)
               .append(" | ");
        
        if (session.getSnake() != null) {
            switch (session.getSnake().getDirection()) {
                case UP:
                    statsBar.append(ChatColor.WHITE).append("⬇");
                    break;
                case DOWN:
                    statsBar.append(ChatColor.WHITE).append("⬆");
                    break;
                case LEFT:
                    statsBar.append(ChatColor.WHITE).append("➡");
                    break;
                case RIGHT:
                    statsBar.append(ChatColor.WHITE).append("⬅");
                    break;
            }
            statsBar.append(ChatColor.GRAY).append(" | ");
        }
        
        statsBar.append(ChatColor.GOLD)
               .append("🪙 Coin: ")
               .append(ChatColor.GOLD)
               .append(session.getScore())
               .append(ChatColor.WHITE)
               .append(" | ");
        
        statsBar.append(ChatColor.GREEN)
               .append("◌ Length: ")
               .append(ChatColor.GREEN)
               .append(session.getSnakeLength());
        
        statsBar.append(ChatColor.WHITE)
               .append(" | ")
               .append(ChatColor.AQUA)
               .append("✵ Speed: ")
               .append(ChatColor.WHITE)
               .append("●".repeat(Math.min(session.getSpeed(), 5)))
               .append(ChatColor.DARK_GRAY)
               .append("●".repeat(Math.max(0, 5 - session.getSpeed())));
        
        if (session.isPowerUpActive()) {
            PowerUpType type = session.getActivePowerUp();
            long remainingMs = session.getPowerUpExpireTime() - System.currentTimeMillis();
            int seconds2 = (int) Math.ceil(remainingMs / 1000.0);
            
            statsBar.append(ChatColor.WHITE)
                   .append(" | ")
                   .append(type.getColor())
                   .append("★ ")
                   .append(type.getDisplayName())
                   .append(" (")
                   .append(seconds2)
                   .append("s)");
        }
        
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, 
                new TextComponent(statsBar.toString()));
        
        showTutorialBossBar(player, PlayerState.PLAYING);
    }
    
    public static void displayGameOverStats(Player player, GameSession session) {
        if (session == null || player == null) return;
        
        Engine engine = Engine.getInstance();
        int totalCoins = 0;
        if (engine != null && engine.getEconomyManager() != null) {
            totalCoins = engine.getEconomyManager().getCoins(player);
        }
        
        StringBuilder statsBar = new StringBuilder();
        statsBar.append(ChatColor.RED)
               .append("Game Over! ")
               .append(ChatColor.YELLOW)
               .append("Score: ")
               .append(ChatColor.GOLD)
               .append(session.getScore())
               .append(ChatColor.GRAY)
               .append(" | ")
               .append(ChatColor.GOLD)
               .append("🪙 ")
               .append(totalCoins)
               .append(ChatColor.GRAY)
               .append(" | ")
               .append(ChatColor.YELLOW)
               .append("Click to Play Again");
        
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, 
                new TextComponent(statsBar.toString()));
        
        showTutorialBossBar(player, PlayerState.GAME_OVER);
    }
    
    public static void displayLeaderboardSubtitle(Player player) {
        Engine engine = Engine.getInstance();
        if (engine != null && engine.getLeaderboardManager() != null) {
            player.sendTitle(
                "§b§lLEADERBOARD", 
                "", 
                10, 60, 20);
            
            engine.getLeaderboardManager().displayLeaderboard(
                player, 
                LeaderboardManager.LeaderboardType.HIGHEST_SCORE
            );
        } else {
            // fallback?
            player.sendTitle(
                "§b§lLEADERBOARD", 
                "", 
                10, 100, 20);
        }
        
        showTutorialBossBar(player, PlayerState.LEADERBOARD);
    }
    
    public static void displaySettingsSubtitle(Player player) {
        player.sendTitle(
            "§6§lSETTINGS", 
            "", 
            10, 100, 20);
        
        showTutorialBossBar(player, PlayerState.SETTINGS);
    }
    
    public static void updateActionBar(Player player, GameSession session) {
        if (session == null || player == null) return;
        
        PlayerState state = session.getPlayerState();
        
        switch (state) {
            case MAIN_MENU:
                displayMainMenuBar(player);
                break;
            case PLAYING:
                displayGameStats(player, session);
                break;
            case GAME_OVER:
                displayGameOverStats(player, session);
                break;
            case LEADERBOARD:
                displayLeaderboardSubtitle(player);
                break;
            case SETTINGS:
                displaySettingsSubtitle(player);
                break;
            default:
                removeTutorialBossBar(player);
                break;
        }
    }
} 