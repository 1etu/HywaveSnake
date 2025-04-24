package net.hywave.snake.commands;

import net.hywave.snake.Engine;
import net.hywave.snake.leaderboard.LeaderboardManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LeaderboardCommand implements CommandExecutor, TabCompleter {
    private final Engine plugin;
    
    public LeaderboardCommand(Engine plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) { return true; }
        
        Player player = (Player) sender;
        
        if (args.length == 0) { return true; }
        
        switch (args[0].toLowerCase()) {
            case "score":
            case "scores":
            case "highscores":
            showLeaderboard(player, args);
                break;
                
            case "length":
                showLengthLeaderboard(player, args);
                break;
                
            case "speed":
            case "fastest":
            case "time":
                showSpeedLeaderboard(player, args);
                break;
                
            case "coins":
                showCoinsLeaderboard(player, args);
                break;
                
            case "stats":
                showPlayerStats(player, args);
                break;
                
            case "help":
            default:
                break;
        }
        
        return true;
    }

    
    private void showLeaderboard(Player player, String[] args) {
        int lmt = 10;
        if (args.length > 1) {
            try {
                lmt = Integer.parseInt(args[1]);
                lmt = Math.max(1, Math.min(100, lmt));
            } catch (NumberFormatException e) {}
        }
        
        plugin.getLeaderboardManager().displayLeaderboard(
            player, 
            LeaderboardManager.LeaderboardType.HIGHEST_SCORE,
            lmt
        );
    }
    
    private void showLengthLeaderboard(Player player, String[] args) {
        int lmt = 10;
        if (args.length > 1) {
            try {
                lmt = Integer.parseInt(args[1]);
                lmt = Math.max(1, Math.min(100, lmt));
            } catch (NumberFormatException e) {}
        }
        
        plugin.getLeaderboardManager().displayLeaderboard(
            player, 
            LeaderboardManager.LeaderboardType.LONGEST_SNAKE,
            lmt
        );
    }
    
    private void showSpeedLeaderboard(Player player, String[] args) {
        int lmt = 10;
        if (args.length > 1) {
            try {
                lmt = Integer.parseInt(args[1]);
                lmt = Math.max(1, Math.min(100, lmt));
            } catch (NumberFormatException e) {}
        }
        
        plugin.getLeaderboardManager().displayLeaderboard(
            player, 
            LeaderboardManager.LeaderboardType.FASTEST_GAMES,
            lmt
        );
    }
    
    private void showCoinsLeaderboard(Player player, String[] args) {
        int lmt = 10;
        if (args.length > 1) {
            try {
                lmt = Integer.parseInt(args[1]);
                lmt = Math.max(1, Math.min(100, lmt));
            } catch (NumberFormatException e) {}
        }
        
        plugin.getLeaderboardManager().displayLeaderboard(
            player, 
            LeaderboardManager.LeaderboardType.MOST_COINS,
            lmt
        );
    }
    
    private void showPlayerStats(Player player, String[] args) {
        Player target = player;
        
        if (args.length > 1) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
                return;
            }
        }
        
        plugin.getLeaderboardManager().displayPlayerStats(player, target.getUniqueId());
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            String inp = args[0].toLowerCase();
            List<String> comp = Arrays.asList("score", "length", "fastest", "coins", "stats", "help");
            
            return comp.stream()
                    .filter(s -> s.startsWith(inp))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("stats")) {
            String inp = args[1].toLowerCase();
            List<String> pn = new ArrayList<>();
            
            for (Player p : Bukkit.getOnlinePlayers()) {
                pn.add(p.getName());
            }
            
            return pn.stream()
                    .filter(name -> name.toLowerCase().startsWith(inp))
                    .collect(Collectors.toList());
        }
        
        return new ArrayList<>();
    }
} 