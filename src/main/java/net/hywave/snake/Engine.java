package net.hywave.snake;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.World.Environment;
import org.bukkit.WorldType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;

import net.hywave.snake.game.GameManager;
import net.hywave.snake.listeners.PlayerListeners;
import net.hywave.snake.listeners.KeyHandler;
import net.hywave.snake.commands.CommandManager;
import net.hywave.snake.commands.DebugCommand;
import net.hywave.snake.config.ConfigManager;
import net.hywave.snake.db.DatabaseManager;
import net.hywave.snake.economy.EconomyManager;
import net.hywave.snake.leaderboard.LeaderboardManager;
import net.hywave.snake.cosmetics.CosmeticManager;
import net.hywave.snake.cosmetics.shop.ShopGUI;
import net.hywave.snake.listeners.ShopListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import lombok.Getter;

public class Engine extends JavaPlugin implements Listener {
    
    @Getter
    private static Engine instance;
    
    @Getter
    private GameManager gameManager;
    
    @Getter
    private ConfigManager configManager;
    
    @Getter
    private KeyHandler keyHandler;
    
    @Getter
    private World gameWorld;
    
    @Getter
    private DatabaseManager databaseManager;
    
    @Getter
    private EconomyManager economyManager;
    
    @Getter
    private LeaderboardManager leaderboardManager;
    
    @Getter
    private CosmeticManager cosmeticManager;
    
    @Getter
    private ShopGUI shopGUI;
    
    private String wn;
    private boolean wr = false;
    
    @Override
    public void onEnable() {
        instance = this;
        
        getServer().getPluginManager().registerEvents(this, this);
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        wn = "void_" + sdf.format(new Date());
        
        getLogger().info("World -> Creating new" + wn);
        createGameWorld();
    }
    
    private void createGameWorld() {
        Bukkit.getScheduler().runTask(this, () -> {
            try {
                World existingWorld = Bukkit.getWorld(wn);
                if (existingWorld != null) {
                    gameWorld = existingWorld;
                    getLogger().info("World -> Using existing: " + wn);
                } else {
                    WorldCreator wc = new WorldCreator(wn);
                    wc.environment(Environment.NORMAL);
                    wc.type(WorldType.FLAT);
                    
                    wc.generatorSettings("{\"layers\": [{\"block\": \"minecraft:air\", \"height\": 1}], \"biome\":\"minecraft:the_void\"}");
                    
                    gameWorld = wc.createWorld();
                    
                    if (gameWorld == null) {
                        getLogger().severe("World -> ERROR: Failed to create void world - world is null?");
                        return;
                    }
                }
                
                gameWorld.setSpawnLocation(0, 100, 0);
                gameWorld.setKeepSpawnInMemory(true);
                gameWorld.setAutoSave(false);
                
                gameWorld.setGameRuleValue("doDaylightCycle", "false");
                gameWorld.setGameRuleValue("doWeatherCycle", "false");
                gameWorld.setGameRuleValue("doMobSpawning", "false");
                gameWorld.setGameRuleValue("fallDamage", "false");
                gameWorld.setGameRuleValue("doFireTick", "false");
                
                getLogger().info("World -> Setup OK. " + wn);
                
                completeInitialization();
                
                wr = true;
                
                for (Player player : Bukkit.getOnlinePlayers()) {
                    gameManager.handlePlayerJoin(player);
                }
            } catch (Exception e) {}
        });
    }
    
    private void completeInitialization() {
        this.configManager = new ConfigManager(this);
        this.databaseManager = new DatabaseManager(this);
        this.economyManager = new EconomyManager(this, databaseManager);
        this.leaderboardManager = new LeaderboardManager(this, databaseManager);
        this.cosmeticManager = new CosmeticManager(this);
        this.shopGUI = new ShopGUI(this, cosmeticManager);
        
        this.gameManager = new GameManager(this);
        
        getServer().getPluginManager().registerEvents(new PlayerListeners(this), this);
        getServer().getPluginManager().registerEvents(new ShopListener(this, shopGUI), this);
        
        try {
            this.keyHandler = new KeyHandler(this);
            getLogger().info("KeyHandler -> Registered successfully");
        } catch (Exception e) {
            getLogger().warning("KeyHandler -> ERROR: " + e.getMessage());
            getLogger().warning("NOTE: Please make sure that Protocol Lib is installed!");
        }
        
        new CommandManager(this).registerCommands();
        getCommand("debug").setExecutor(new DebugCommand(this));
        
        getLogger().info("GameManager -> Successfully initialized.");
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent evt) {
        if (!wr) {
            evt.disallow(Result.KICK_OTHER, "§cServer is still initializing. Please try again in a few moments!");
            return;
        }
        
        evt.allow();
    }
    
    @Override
    public void onDisable() {
        for (Player who : Bukkit.getOnlinePlayers()) {
            who.kickPlayer("§cServer is shutting down!");
        }

        if (gameManager != null) { gameManager.cleanup(); }
        
        if (keyHandler != null) {
            try {
                keyHandler.cleanup();
            } catch (Exception e) {
                getLogger().warning("Error during KeyHandler cleanup: " + e.getMessage());
            }
        }
        
        if (databaseManager != null) { databaseManager.closeConnection(); }
        
        if (gameWorld != null) {
            for (Player player : gameWorld.getPlayers()) {
                player.kickPlayer("§cServer is shutting down!");
            }
            
            try {
                Bukkit.unloadWorld(gameWorld, false);
                getLogger().info("World -> Unloaded: " + wn);
            } catch (Exception e) {}
        }        
    }
}
