package net.hywave.snake.config;

import org.bukkit.configuration.file.FileConfiguration;
import net.hywave.snake.Engine;

public class ConfigManager {
    private final Engine plugin;
    private FileConfiguration config;
    
    private int gameTickRate = 5;
    private int initialSnakeLength = 3;
    
    public ConfigManager(Engine plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    private void loadConfig() {
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
        
        this.gameTickRate = config.getInt("game.tick-rate", 5);
        this.initialSnakeLength = config.getInt("game.initial-snake-length", 3);
    }
    
    public void saveConfig() {
        config.set("game.tick-rate", gameTickRate);
        config.set("game.initial-snake-length", initialSnakeLength);
        
        plugin.saveConfig();
    }
    
    public int getGameTickRate() {
        return gameTickRate;
    }
    
    public int getInitialSnakeLength() {
        return initialSnakeLength;
    }
} 