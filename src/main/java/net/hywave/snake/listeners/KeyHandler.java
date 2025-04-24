package net.hywave.snake.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers.PlayerAction;

import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.GameMode;

import net.hywave.snake.Engine;
import net.hywave.snake.model.Direction;
import net.hywave.snake.model.GameSession;
import net.hywave.snake.state.PlayerState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class KeyHandler implements Listener {
    private final Engine plugin;
    private final ProtocolManager protocolManager;

    private final Map<UUID, Long> lastDirectionChange = new ConcurrentHashMap<>();
    private final Map<UUID, Direction> lastDirection = new ConcurrentHashMap<>();
    
    // cache merge [w,a,s,d] 
    private final Map<UUID, Boolean> pressedW = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> pressedA = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> pressedS = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> pressedD = new ConcurrentHashMap<>();
    
    private final boolean DEBUG_MODE = false;
    private static final long COOLDOWN_MS = 50;
    
    public KeyHandler(Engine plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        
        Bukkit.getPluginManager().registerEvents(this, plugin);
        registerKeyListeners();
        plugin.getLogger().info("dbg: done binding kb packets in " + COOLDOWN_MS + "ms");
        
        if (DEBUG_MODE) { startDebugTask(); }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        resetKeyState(player.getUniqueId());
        
        if (DEBUG_MODE) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage(ChatColor.GREEN + "dbg: please test & report key handling performance");
            }, 40L);
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        resetKeyState(player.getUniqueId());
    }
    
    @EventHandler
    public void onPlayerToggleSprint(PlayerToggleSprintEvent event) {
        Player player = event.getPlayer();
        GameSession session = plugin.getGameManager().getSession(player);
        
        if (!isValidGameState(player, session)) { return; }
        
        if (event.isSprinting()) {
            pressedW.put(player.getUniqueId(), true);
            if (DEBUG_MODE) player.sendMessage(ChatColor.YELLOW + "dbg: sprint+w");
            updateDirection(player, Direction.UP);
        }
    }
    
    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        GameSession session = plugin.getGameManager().getSession(player);
        
        if (!isValidGameState(player, session)) {
            return;
        }
        
        if (event.isSneaking()) {
            pressedS.put(player.getUniqueId(), true);
            if (DEBUG_MODE) player.sendMessage(ChatColor.YELLOW + "dbg: sneak+s");
            updateDirection(player, Direction.DOWN);
        }
    }
    
    private void resetKeyState(UUID playerId) {
        pressedW.put(playerId, false);
        pressedA.put(playerId, false);
        pressedS.put(playerId, false);
        pressedD.put(playerId, false);
        lastDirection.remove(playerId);
    }
    
    private void registerKeyListeners() {
        protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.HIGHEST, 
                PacketType.Play.Client.STEER_VEHICLE) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                handleSteerVehicle(event);
            }
        });
        
        protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.HIGHEST, 
                PacketType.Play.Client.ENTITY_ACTION) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                handleEntityAction(event);
            }
        });
        
        protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.HIGHEST, 
                PacketType.Play.Client.POSITION, PacketType.Play.Client.POSITION_LOOK) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                handlePosition(event);
            }
        });
        
        protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.HIGHEST,
                PacketType.Play.Client.HELD_ITEM_SLOT) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                Engine engine = (Engine) plugin;
                GameSession session = engine.getGameManager().getSession(player);
                
                if (!isValidGameState(player, session)) {
                    return;
                }
                
                int newSlot = event.getPacket().getIntegers().read(0);
                
                Direction direction = null;
                switch (newSlot) {
                    case 0:
                        direction = Direction.UP;
                        break;
                    case 1:
                        direction = Direction.RIGHT;
                        break;
                    case 2:
                        direction = Direction.DOWN;
                        break;
                    case 3:
                        direction = Direction.LEFT;
                        break;
                    default:
                        break;
                }
                
                if (direction != null) {
                    if (DEBUG_MODE) player.sendMessage(ChatColor.GOLD + "dbg: hb " + newSlot + " - dir: " + direction);
                    updateDirection(player, direction);
                }
            }
        });
        
        Bukkit.getScheduler().runTaskTimer(plugin, this::processKeyStates, 1L, 1L);
    }
    
    private void handleSteerVehicle(PacketEvent event) {
        Player player = event.getPlayer();
        GameSession session = plugin.getGameManager().getSession(player);
        
        if (!isValidGameState(player, session)) { return; }
        
        try {
            PacketContainer packet = event.getPacket();
            
            // paket 0x16 - field (sideways, forward, flags) - bit mask 0x1 jump 0x2 unmount
            if (packet.getFloat().size() < 2) {
                return;
            }
            
            float forward = packet.getFloat().read(0);
            float sideways = packet.getFloat().read(1);
            
            UUID playerId = player.getUniqueId();
            
            if (Math.abs(forward) > 0.01f) {
                boolean isW = forward > 0;
                boolean isS = forward < 0;
                pressedW.put(playerId, isW);
                pressedS.put(playerId, isS);
                
                if (isW) {
                    if (DEBUG_MODE) player.sendMessage(ChatColor.AQUA + "dbg: w pressed");
                    updateDirection(player, Direction.UP);
                } else if (isS) {
                    if (DEBUG_MODE) player.sendMessage(ChatColor.AQUA + "dbg: s pressed");
                    updateDirection(player, Direction.DOWN);
                }
            }
            
            if (Math.abs(sideways) > 0.01f) {
                boolean isD = sideways > 0;
                boolean isA = sideways < 0;
                pressedD.put(playerId, isD);
                pressedA.put(playerId, isA);
                
                if (isD) {
                    if (DEBUG_MODE) player.sendMessage(ChatColor.AQUA + "dbg: sv+d");
                    updateDirection(player, Direction.RIGHT);
                } else if (isA) {
                    if (DEBUG_MODE) player.sendMessage(ChatColor.AQUA + "dbg: sv+a");
                    updateDirection(player, Direction.LEFT);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("err: " + e.getMessage());
        }
    }
    
    private void handleEntityAction(PacketEvent event) {
        Player player = event.getPlayer();
        GameSession session = plugin.getGameManager().getSession(player);
        
        if (!isValidGameState(player, session)) {
            return;
        }
        
        try {
            PacketContainer packet = event.getPacket();
            PlayerAction action = packet.getPlayerActions().read(0);
            
            UUID playerId = player.getUniqueId();
            
            switch (action) {
                case START_SPRINTING:
                    pressedW.put(playerId, true);
                    if (DEBUG_MODE) player.sendMessage(ChatColor.GREEN + "dbg: START_SPRINTING - W");
                    updateDirection(player, Direction.UP);
                    break;
                case STOP_SPRINTING:
                    pressedW.put(playerId, false);
                    break;
                case START_SNEAKING:
                    pressedS.put(playerId, true);
                    if (DEBUG_MODE) player.sendMessage(ChatColor.GREEN + "dbg: START_SNEAKING - S detected");
                    updateDirection(player, Direction.DOWN);
                    break;
                case STOP_SNEAKING:
                    pressedS.put(playerId, false);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("err action: " + e.getMessage());
        }
    }
    
    private void handlePosition(PacketEvent event) {
        Player player = event.getPlayer();
        GameSession session = plugin.getGameManager().getSession(player);
        
        if (!isValidGameState(player, session)) {
            return;
        }
        
        try {
            double x = event.getPacket().getDoubles().read(0) - player.getLocation().getX();
            double z = event.getPacket().getDoubles().read(2) - player.getLocation().getZ();
            
            if (Math.abs(x) < 0.0001 && Math.abs(z) < 0.0001) {
                return;
            }
            
            UUID playerId = player.getUniqueId();
            Direction direction = null;

            if (Math.abs(x) > Math.abs(z)) {
                if (x > 0) {
                    pressedD.put(playerId, true);
                    direction = Direction.RIGHT;
                    if (DEBUG_MODE) player.sendMessage(ChatColor.BLUE + "pos pckt - d");
                } else {
                    pressedA.put(playerId, true);
                    direction = Direction.LEFT;
                    if (DEBUG_MODE) player.sendMessage(ChatColor.BLUE + "pos pckt - a");
                }
            } else {
                if (z > 0) {
                    pressedS.put(playerId, true);
                    direction = Direction.DOWN;
                    if (DEBUG_MODE) player.sendMessage(ChatColor.BLUE + "pos pckt - s");
                } else {
                    pressedW.put(playerId, true);
                    direction = Direction.UP;
                    if (DEBUG_MODE) player.sendMessage(ChatColor.BLUE + "pos pckt - w");
                }
            }
            
            if (direction != null) {
                updateDirection(player, direction);
            }
        } catch (Exception e) {
        }
    }
    
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        GameSession session = plugin.getGameManager().getSession(player);
        
        if (!isValidGameState(player, session)) {
            return;
        }
        
        double dx = event.getTo().getX() - event.getFrom().getX();
        double dz = event.getTo().getZ() - event.getFrom().getZ();
        
        if (Math.abs(dx) < 0.0001 && Math.abs(dz) < 0.0001) { return; }
        
        UUID playerId = player.getUniqueId();
        Direction direction = null;
        
        if (Math.abs(dx) > Math.abs(dz)) {
            if (dx > 0) {
                pressedD.put(playerId, true);
                direction = Direction.RIGHT;
                if (DEBUG_MODE) player.sendMessage(ChatColor.DARK_GREEN + "mov ev - d");
            } else {
                pressedA.put(playerId, true);
                direction = Direction.LEFT;
                if (DEBUG_MODE) player.sendMessage(ChatColor.DARK_GREEN + "mov ev - a");
            }
        } else {
            if (dz > 0) {
                pressedS.put(playerId, true);
                direction = Direction.DOWN;
                if (DEBUG_MODE) player.sendMessage(ChatColor.DARK_GREEN + "mov ev - s");
            } else {
                pressedW.put(playerId, true);
                direction = Direction.UP;
                if (DEBUG_MODE) player.sendMessage(ChatColor.DARK_GREEN + "mov ev - w");
            }
        }
        
        if (direction != null) {
            updateDirection(player, direction);
        }
    }
    
    private void processKeyStates() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            GameSession session = plugin.getGameManager().getSession(player);
            
            if (!isValidGameState(player, session)) { continue; }
            
            UUID playerId = player.getUniqueId();
            
            boolean w = pressedW.getOrDefault(playerId, false);
            boolean a = pressedA.getOrDefault(playerId, false);
            boolean s = pressedS.getOrDefault(playerId, false);
            boolean d = pressedD.getOrDefault(playerId, false);
            
            Direction direction = null;
            if (w) direction = Direction.UP;
            else if (s) direction = Direction.DOWN;
            else if (a) direction = Direction.LEFT;
            else if (d) direction = Direction.RIGHT;
            
            if (direction != null && direction != lastDirection.getOrDefault(playerId, null)) {
                updateDirection(player, direction);
                resetKeyState(playerId);
            }
        }
    }
    
    private void updateDirection(Player player, Direction direction) {
        GameSession session = plugin.getGameManager().getSession(player);
        if (!isValidGameState(player, session)) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        long lastChange = lastDirectionChange.getOrDefault(playerId, 0L);
        
        if (now - lastChange > COOLDOWN_MS) {
            // cache lazim
            Direction current = lastDirection.getOrDefault(playerId, null);
            if (direction != current) {
                plugin.getGameManager().setDirection(player, direction);
                if (DEBUG_MODE) {
                    player.sendMessage(ChatColor.GOLD + "dir set " + direction);
                }
                
                lastDirectionChange.put(playerId, now);
                lastDirection.put(playerId, direction);
            }
        }
    }
    
    private boolean isValidGameState(Player player, GameSession session) {
        return session != null && 
               player.getWorld() == plugin.getGameWorld() &&
               session.getPlayerState() == PlayerState.PLAYING;
    }
    
    private void startDebugTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                GameSession session = plugin.getGameManager().getSession(player);
                if (session != null && session.getPlayerState() == PlayerState.PLAYING) {
                    UUID playerId = player.getUniqueId();
                    Direction current = lastDirection.getOrDefault(playerId, null);
                    if (current != null) {
                        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                            net.md_5.bungee.api.chat.TextComponent.fromLegacyText(ChatColor.YELLOW + "current directaion: " + current));
                    }
                }
            }
        }, 20L, 20L);
    }
    
    public void cleanup() {
        protocolManager.removePacketListeners(plugin);
        lastDirectionChange.clear();
        lastDirection.clear();
        pressedW.clear();
        pressedA.clear();
        pressedS.clear();
        pressedD.clear();
    }
    
    public void setupPlayerForKeyDetection(Player player) {
        if (player == null) return;
        
        player.setGameMode(GameMode.SPECTATOR);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setFlySpeed(0.1f);
        player.setWalkSpeed(0.1f);
        
        resetKeyState(player.getUniqueId());
    }
} 