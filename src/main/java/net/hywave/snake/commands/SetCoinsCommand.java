package net.hywave.snake.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import net.hywave.snake.Engine;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SetCoinsCommand implements CommandExecutor, TabCompleter {
    private final Engine plugin;
    
    public SetCoinsCommand(Engine plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("snake.admin.setcoins")) {
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /snake setcoins <player> <amount>");
            return true;
        }
        
        Player who = Bukkit.getPlayer(args[0]);
        if (who == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
            return true;
        }
        
        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[1]);
            return true;
        }
        
        if (amount < 0) {
            sender.sendMessage(ChatColor.RED + "Negative :D?");
            return true;
        }
        
        plugin.getEconomyManager().setCoins(who.getUniqueId(), amount);
        
        sender.sendMessage(ChatColor.GREEN + "Set " + who.getName() + "'s coins to " + amount);
        who.sendMessage(ChatColor.GOLD + "Your coins have been set to " + amount + " by an admin");
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("snake.admin.setcoins")) { return new ArrayList<>(); }
        
        if (args.length == 1) {
            String inp = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers()
                    .stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(inp))
                    .collect(Collectors.toList());
        }
        
        return new ArrayList<>();
    }
} 