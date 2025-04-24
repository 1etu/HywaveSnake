package net.hywave.snake.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.hywave.snake.Engine;

public class DebugCommand implements CommandExecutor {
    private final Engine plugin;
    
    public DebugCommand(Engine plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { return true; }
        
        Player player = (Player) sender;
        
        player.setWalkSpeed(0.2f);
        player.setFlySpeed(0.1f);
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.setWalkSpeed(0);
            player.setFlySpeed(0);
        }, 200L);
        
        return true;
    }
  
}
