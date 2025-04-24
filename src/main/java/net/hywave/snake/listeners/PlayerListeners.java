package net.hywave.snake.listeners;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;

import net.hywave.snake.Engine;
import net.hywave.snake.model.Direction;
import net.hywave.snake.model.GameSession;
import net.hywave.snake.state.PlayerState;
import net.hywave.snake.util.SoundEffects;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerListeners implements Listener {
    private final Engine plugin;
    private static long tickCounter = 0;
    
    public PlayerListeners(Engine plugin) {
        this.plugin = plugin;
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> tickCounter++, 1L, 1L);
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        player.setHealth(20);
        player.setGameMode(GameMode.SPECTATOR);
        plugin.getGameManager().handlePlayerJoin(player);
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        plugin.getGameManager().removeSession(player);
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        if (plugin.getGameWorld() != null) {
            event.setRespawnLocation(plugin.getGameWorld().getSpawnLocation());
            
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                plugin.getGameManager().handlePlayerJoin(player);
            }, 5L);
        }
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        
        if (event.getFrom().getWorld() == plugin.getGameWorld() && 
            event.getTo().getWorld() != plugin.getGameWorld()) {
            
            plugin.getGameManager().removeSession(player);
        }

        else if (event.getFrom().getWorld() != plugin.getGameWorld() && 
                 event.getTo().getWorld() == plugin.getGameWorld()) {
            
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                plugin.getGameManager().handlePlayerJoin(player);
            }, 5L);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        GameSession session = plugin.getGameManager().getSession(player);
        
        if (session == null) {
            return;
        }
        
        if (session.getPlayerState() == PlayerState.MAIN_MENU) {
            if (event.getAction() == Action.LEFT_CLICK_AIR || 
                event.getAction() == Action.LEFT_CLICK_BLOCK) {
                plugin.getGameManager().startGame(player);
                
                SoundEffects.playButtonClickSound(player);
                
                player.sendMessage("§aStarting Snake game...");
                event.setCancelled(true);
            } else if (event.getAction() == Action.RIGHT_CLICK_AIR || 
                       event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (player.isSneaking()) {
                    session.setPlayerState(PlayerState.LEADERBOARD);
                    
                    SoundEffects.playButtonClickSound(player);
                    
                    player.sendMessage("§eLeaderboards coming soon!");
                } else {
                    session.setPlayerState(PlayerState.SETTINGS);
                    
                    SoundEffects.playButtonClickSound(player);
                    
                    player.sendMessage("§eSettings coming soon!");
                }
                event.setCancelled(true);
            }
        } else if (session.getPlayerState() == PlayerState.PLAYING) {
            event.setCancelled(true);
        } else if (session.getPlayerState() == PlayerState.GAME_OVER ||
                  session.getPlayerState() == PlayerState.LEADERBOARD ||
                  session.getPlayerState() == PlayerState.SETTINGS) {
            if (event.getAction() == Action.LEFT_CLICK_AIR || 
                event.getAction() == Action.LEFT_CLICK_BLOCK ||
                event.getAction() == Action.RIGHT_CLICK_AIR ||
                event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                
                SoundEffects.playButtonClickSound(player);
                
                session.setPlayerState(PlayerState.MAIN_MENU);
                plugin.getGameManager().getGameAreas().get(player.getUniqueId()).updateBoard(session);
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            GameSession session = plugin.getGameManager().getSession(player);
            
            if (session != null && session.getPlayerState() == PlayerState.PLAYING) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        GameSession session = plugin.getGameManager().getSession(player);
        
        if (session != null && session.getPlayerState() == PlayerState.PLAYING) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        if (player.getWorld() != plugin.getGameWorld()) {
            return;
        }
        
        GameSession session = plugin.getGameManager().getSession(player);
        
        if (session == null) {
            return;
        }
        
        if (session.getPlayerState() == PlayerState.INITIALIZING) {
            event.setCancelled(true);
            return;
        }
        
        Location from = event.getFrom();
        Location to = event.getTo();
        
        if (to == null) return;
        
        Location fixedLocation = from.clone();
        fixedLocation.setYaw(to.getYaw());
        fixedLocation.setPitch(to.getPitch());
        
        event.setTo(fixedLocation);
        if (session.getPlayerState() == PlayerState.PLAYING) {
            double dx = to.getX() - from.getX();
            double dz = to.getZ() - from.getZ();
            
            if (Math.abs(dx) > 0.001 || Math.abs(dz) > 0.001) {
                handleWASDMovement(player, dx, dz);
            }
        }
    }
    
    private void handleWASDMovement(Player player, double dx, double dz) {
        GameSession session = plugin.getGameManager().getSession(player);
        
        if (session == null || session.getPlayerState() != PlayerState.PLAYING) {
            return;
        }
        
        Direction direction = null;
        
        if (Math.abs(dx) > Math.abs(dz)) {
            direction = dx > 0 ? Direction.RIGHT : Direction.LEFT;
        } else {
            direction = dz > 0 ? Direction.DOWN : Direction.UP;
        }
        
        if (direction != null) {
            // snake_dir_change_%player_uuid%
            UUID playerUuid = player.getUniqueId();
            String cooldownKey = "snake_dir_change_" + playerUuid;
            Long lastChangeTime = PlayerDataStore.getData(cooldownKey);
            long currentTime = tickCounter;
            
            if (lastChangeTime == null || currentTime - lastChangeTime > 5) {
                plugin.getGameManager().setDirection(player, direction);
                PlayerDataStore.setData(cooldownKey, currentTime);
            }
        }
    }
    
    private static class PlayerDataStore {
        private static final Map<String, Long> dataStore = new HashMap<>();
        
        public static Long getData(String key) {
            return dataStore.get(key);
        }
        
        public static void setData(String key, Long value) {
            dataStore.put(key, value);
        }
        
        public static void clearData(String key) {
            dataStore.remove(key);
        }
    }
} 