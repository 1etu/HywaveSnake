package net.hywave.snake.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.hywave.snake.Engine;
import net.hywave.snake.model.GameSession;
import net.hywave.snake.state.PlayerState;

public class CommandManager implements CommandExecutor {
    private final Engine plugin;
    
    public CommandManager(Engine plugin) {
        this.plugin = plugin;
    }
    
    public void registerCommands() {
        plugin.getCommand("snake").setExecutor(this);
        
        LeaderboardCommand ldrbd = new LeaderboardCommand(plugin);
        plugin.getCommand("leaderboard").setExecutor(ldrbd);
        plugin.getCommand("leaderboard").setTabCompleter(ldrbd);
        
        setc = new SetCoinsCommand(plugin);
        plugin.getCommand("snake").setTabCompleter(setc);
    }
    
    private SetCoinsCommand setc;
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { return true; }
        
        Player player = (Player) sender;
        
        if (args.length == 0) { sendHelp(player);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "start":
                startGame(player);
                break;
            case "stop":
                stopGame(player);
                break;
            case "reset":
                resetGame(player);
                break;
            case "coins":
                showCoins(player);
                break;
            case "setcoins":
                if (player.hasPermission("snake.admin.setcoins")) {
                    String[] neww = new String[args.length - 1];
                    System.arraycopy(args, 1, neww, 0, args.length - 1);
                    return setc.onCommand(sender, command, label, neww);
                } else {
                    player.sendMessage("§c§l(!) §cYou have to be an ADMIN to use this command.");
                }
                break;
            case "shop":
                openShop(player);
                break;
            default:
                sendHelp(player);
                break;
        }
        
        return true;
    }
    
    private void sendHelp(Player player) {
        player.sendMessage("§a§m-----------------------------------------------------");
        player.sendMessage("§6§lSnake Commands");
        player.sendMessage("");
        player.sendMessage(" §e/snake start §7- Start a new game.");
        player.sendMessage(" §e/snake stop §7- Stop your current game.");
        player.sendMessage(" §e/snake reset §7- Reset your game session.");
        player.sendMessage(" §e/snake coins §7- View your coin balance.");
        player.sendMessage(" §e/snake shop §7- Open the cosmetics shop.");
        player.sendMessage(" §e/leaderboard §7- View leaderboards and stats.");
        player.sendMessage("");

        if (player.hasPermission("snake.admin.setcoins")) {
            player.sendMessage("§c§lAdmin Commands");
            player.sendMessage(" §e/snake setcoins <player> <amount> §7- Set player coins.");
            player.sendMessage("");
        }
        player.sendMessage("§a§m-----------------------------------------------------");
    }
    
    private void startGame(Player player) {
        GameSession session = plugin.getGameManager().getSession(player);
        
        if (session != null && session.getPlayerState() == PlayerState.PLAYING) {
            player.sendMessage("§c§l(!) §cYou are already playing a game!");
            return;
        }
        
        plugin.getGameManager().startGame(player);
        player.sendMessage("§aStarting Snake game...");
    }
    
    private void stopGame(Player player) {
        GameSession session = plugin.getGameManager().getSession(player);
        
        if (session == null || session.getPlayerState() != PlayerState.PLAYING) {
            player.sendMessage("§c§l(!) §cYou are not currently playing a game!");
            return;
        }
        
        session.setPlayerState(PlayerState.MAIN_MENU);
        plugin.getGameManager().stopGameLoop(player);
        player.sendMessage("§cGame stopped.");
    }
    
    private void resetGame(Player player) {
        GameSession session = plugin.getGameManager().getSession(player);
        
        if (session == null) {
            player.sendMessage("§c§l(!) §cYou don't have an active game session!");
            return;
        }
        
        plugin.getGameManager().removeSession(player);        
        plugin.getGameManager().createSession(player);
        player.sendMessage("§aGame reset successfully.");
    }
    
    private void showCoins(Player player) {
        int coins = plugin.getEconomyManager().getCoins(player);
        player.sendMessage("§6You have §e" + coins + " §6coins.");
    }
    
    private void openShop(Player player) {
        plugin.getGameManager().openShop(player);
    }
} 