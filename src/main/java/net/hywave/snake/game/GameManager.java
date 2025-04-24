package net.hywave.snake.game;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.GameMode;
import org.bukkit.ChatColor;

import net.hywave.snake.Engine;
import net.hywave.snake.model.Direction;
import net.hywave.snake.model.GameSession;
import net.hywave.snake.model.Position;
import net.hywave.snake.state.GameState;
import net.hywave.snake.state.PlayerState;
import net.hywave.snake.util.GameAreaBuilder;
import net.hywave.snake.util.SoundEffects;
import net.hywave.snake.util.StatsBarManager;
import net.hywave.snake.model.PowerUpType;
import net.hywave.snake.db.DatabaseManager;
import net.hywave.snake.economy.EconomyManager;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;

public class GameManager {
    private final Engine plugin;
    
    @Getter
    private final Map<UUID, GameSession> gameSessions = new ConcurrentHashMap<>();
    
    @Getter
    private final Map<UUID, BukkitTask> gameLoops = new ConcurrentHashMap<>();
    
    @Getter
    private final Map<UUID, GameAreaBuilder> gameAreas = new ConcurrentHashMap<>();
    
    
    private final Map<UUID, Location> temporaryBlocks = new ConcurrentHashMap<>();
    

    @SuppressWarnings("unused")
    private final long TICK_RATE = 5L; 
    
    
    private final long[] SPEED_TICKS = {7L, 5L, 4L, 3L, 2L}; 
    
    
    private final Map<UUID, Integer> comboCounts = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastFoodCollectionTime = new ConcurrentHashMap<>();
    
    
    private static final long COMBO_TIMEOUT = 3000; 
    
    
    private final long UI_UPDATE_RATE = 1L; 
    
    
    private static final int POWER_UP_CHANCE = 10; 
    
    
    private static final long POWER_UP_TIMEOUT = 15000; 
    
    private final Random random = new Random();
    
    
    private final DatabaseManager databaseManager;
    private final EconomyManager economyManager;
    
    public GameManager(Engine plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        this.economyManager = plugin.getEconomyManager();
        
        
        startUiUpdateTask();
    }
    
    private void startUiUpdateTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            
            for (GameSession session : gameSessions.values()) {
                if (session != null && session.getPlayer() != null && session.getPlayer().isOnline()) {
                    StatsBarManager.updateActionBar(session.getPlayer(), session);
                }
            }
        }, UI_UPDATE_RATE, UI_UPDATE_RATE);
    }
    
    public void handlePlayerJoin(Player player) {
        
        databaseManager.createOrUpdatePlayer(player);
        
        
        if (player.getWorld() != plugin.getGameWorld()) {
            World gameWorld = plugin.getGameWorld();
            if (gameWorld != null) {
                Location spawnLoc = gameWorld.getSpawnLocation();
                
                
                createTemporaryBlock(player, spawnLoc);
                
                
                player.teleport(spawnLoc.clone().add(0, 1, 0));
                
                
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    createSession(player);
                }, 20L); 
            } else {
            }
        } else {
            
            createSession(player);
        }
    }
    
    private void createTemporaryBlock(Player player, Location location) {
        
        Location blockLoc = location.clone();
        Block block = blockLoc.getBlock();
        block.setType(Material.GLASS);
        
        
        temporaryBlocks.put(player.getUniqueId(), blockLoc);
        
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            
            if (gameAreas.containsKey(player.getUniqueId())) {
                block.setType(Material.AIR);
                temporaryBlocks.remove(player.getUniqueId());
            }
        }, 100L); 
    }
    
    public GameSession getSession(Player player) {
        return gameSessions.get(player.getUniqueId());
    }
    
    public GameSession createSession(Player player) {
        
        removeSession(player);
        
        
        GameSession session = new GameSession(player);
        gameSessions.put(player.getUniqueId(), session);
        
        
        session.setPlayerState(PlayerState.INITIALIZING);
        
        
        UUID playerId = player.getUniqueId();
        int hashCode = Math.abs(playerId.hashCode());
        int offsetX = (hashCode % 500) * 50; 
        int offsetZ = (hashCode / 500) * 50;
        
        
        GameAreaBuilder gameArea = new GameAreaBuilder(plugin, player, session.getGameBoard(), offsetX, offsetZ);
        gameAreas.put(player.getUniqueId(), gameArea);
        
        
        Bukkit.getScheduler().runTask(plugin, () -> {
            gameArea.buildGameArea(() -> {
                
                session.setPlayerState(PlayerState.MAIN_MENU);
                
                
                gameArea.updateBoard(session);
                
                
                Location blockLoc = temporaryBlocks.get(player.getUniqueId());
                if (blockLoc != null) {
                    blockLoc.getBlock().setType(Material.AIR);
                    temporaryBlocks.remove(player.getUniqueId());
                }
                
                plugin.getLogger().info("GameArea -> Created for player: " + player.getName());
            });
        });
        
        return session;
    }
    
    public void removeSession(Player player) {
        UUID playerId = player.getUniqueId();
        
        
        stopGameLoop(player);
        
        
        if (gameAreas.containsKey(playerId)) {
            gameAreas.get(playerId).destroyGameArea();
            gameAreas.remove(playerId);
        }
        
        
        Location blockLoc = temporaryBlocks.get(playerId);
        if (blockLoc != null) {
            blockLoc.getBlock().setType(Material.AIR);
            temporaryBlocks.remove(playerId);
        }
        
        
        StatsBarManager.removeTutorialBossBar(player);
        
        
        gameSessions.remove(playerId);
    }
    
    public void startGame(Player player) {
        GameSession session = getSession(player);
        
        if (session == null) {
            session = createSession(player);
        }
        
        
        session.reset();
        session.initializeGame();
        session.setPlayerState(PlayerState.PLAYING);
        
        
        if (session.getSpeedBoostMultiplier() > 1.0) {
            
            double boost = session.getSpeedBoostMultiplier();
            int speedLevel = (int) Math.min(SPEED_TICKS.length - 1, Math.ceil(boost));
            
            
            session.setSpeed(Math.min(speedLevel, SPEED_TICKS.length - 1));
            
            
            player.sendMessage(ChatColor.AQUA + "Speed Boost activated! Starting at level " + session.getSpeed());
        }
        
        
        startGameLoop(player);
        
        
        gameAreas.get(player.getUniqueId()).updateBoard(session);
    }
    
    @SuppressWarnings("unused")
    private void startCountdown(Player player, GameSession session) {
        
        final PlayerState originalState = session.getPlayerState();
        session.setPlayerState(PlayerState.INITIALIZING);
        
        
        player.sendTitle("", "§a3", 5, 10, 5);
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 0.8f);
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.sendTitle("", "§e2", 5, 10, 5);
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 0.9f);
        }, 20L);
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.sendTitle("", "§c1", 5, 10, 5);
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 1.0f);
        }, 40L);
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            session.setPlayerState(originalState);
            session.setPlayerState(PlayerState.PLAYING);
            startGameLoop(player);
        }, 60L);
    }
    
    @SuppressWarnings("unused")
    private void setupPlayerForGame(Player player) {
        
        player.setGameMode(GameMode.SPECTATOR);
        
        
        
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setFlySpeed(0.1f);
        player.setWalkSpeed(0.1f);
        
        

        if (plugin.getKeyHandler() != null) {
            plugin.getKeyHandler().setupPlayerForKeyDetection(player);
        }
        
        
        GameSession session = getSession(player);
        if (session != null && gameAreas.containsKey(player.getUniqueId())) {
            gameAreas.get(player.getUniqueId()).centerPlayerView(player);
        }
    }
    
    public void startGameLoop(Player player) {
        UUID playerId = player.getUniqueId();
        GameSession session = gameSessions.get(playerId);
        
        if (session == null || gameLoops.containsKey(playerId)) {
            return;
        }
        
        
        int speedLevel = Math.min(session.getSpeed(), SPEED_TICKS.length) - 1;
        long tickRate = SPEED_TICKS[speedLevel];
        
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            
            if (session.getPlayerState() != PlayerState.PLAYING || 
                session.getGameState() != GameState.RUNNING) {
                return;
            }
            
            
            updateGame(session);
            
            
            gameAreas.get(playerId).updateBoard(session);
            
        }, tickRate, tickRate);
        
        gameLoops.put(playerId, task);
    }
    
    public void stopGameLoop(Player player) {
        UUID playerId = player.getUniqueId();
        if (gameLoops.containsKey(playerId)) {
            gameLoops.get(playerId).cancel();
            gameLoops.remove(playerId);
        }
    }
    
    private void updateGame(GameSession session) {
        Player player = session.getPlayer();
        UUID playerId = player.getUniqueId();
        
        
        updatePowerUpStatus(session);
        
        
        checkPowerUpTimeout(session);
        
        
        session.getSnake().move();
        
        
        SoundEffects.playMove(session.getPlayer());
        
        
        handlePowerUpCollection(session);
        
        
        if (session.isGameOver()) {
            session.getSnake().setAlive(false);
            session.setGameState(GameState.ENDED);
            session.setPlayerState(PlayerState.GAME_OVER);
            
            
            session.clearPowerUp();
            
            
            boolean hasCustomDeathEffects = false;
            
            
            Position headPos = session.getSnake().getHead();
            if (headPos != null) {
                GameAreaBuilder gameArea = gameAreas.get(playerId);
                if (gameArea != null) {
                    Location headLoc = gameArea.getOriginLocation().clone().add(
                        headPos.getX() + 0.5, 
                        1.0, 
                        headPos.getY() + 0.5
                    );
                    
                    
                    if (session.getDeathSound() != null) {
                        player.playSound(player.getLocation(), 
                                         session.getDeathSound(), 
                                         session.getDeathSoundVolume(), 
                                         session.getDeathSoundPitch());
                        hasCustomDeathEffects = true;
                    }
                    
                    
                    if (session.getDeathParticle() != null) {
                        player.spawnParticle(
                            session.getDeathParticle(), 
                            headLoc,
                            session.getDeathParticleCount(),
                            0.3, 0.3, 0.3,
                            0.1
                        );
                        hasCustomDeathEffects = true;
                    }
                }
            }
            
            
            if (!hasCustomDeathEffects) {
                SoundEffects.playGameOverSound(session.getPlayer());
            }
            
            
            recordGameResult(session);
            
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    session.reset();
                    session.setPlayerState(PlayerState.MAIN_MENU);
                    
                    
                    gameAreas.get(playerId).cleanupGameElements();
                    gameAreas.get(playerId).updateBoard(session);
                }
            }, 60L); 
            
            return;
        }
        
        
        if (session.checkFoodCollision()) {
            
            session.getSnake().grow();
            
            
            SoundEffects.playCollect(session.getPlayer());
            
            
            processCombo(session);
            
            
            int combo = comboCounts.getOrDefault(playerId, 0);
            int baseIncrease = 1 + (combo > 1 ? combo / 2 : 0);
            
            
            int multiplier = session.isPowerUpActive() && 
                             session.getActivePowerUp() == PowerUpType.DOUBLE_POINTS ? 2 : 1;
            int scoreIncrease = baseIncrease * multiplier;
            
            session.setScore(session.getScore() + scoreIncrease);
            
            
            checkMilestones(session);
            
            
            String scoreText = "§6+" + scoreIncrease;
            if (combo > 1) {
                scoreText += " §f(§e" + combo + "x Combo§f)";
            }
            if (multiplier > 1) {
                scoreText += " §6§l2x";
            }
            player.sendTitle("", scoreText, 5, 15, 5);
            
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && session.getPlayerState() == PlayerState.PLAYING) {
                    
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.2f);
                }
            }, 1L);
            
            
            session.generateFood();
            
            
            checkPowerUpSpawn(session);
        } else {
            
            session.getSnake().shrink();
        }
    }
    
    
    private void processCombo(GameSession session) {
        Player player = session.getPlayer();
        UUID playerId = player.getUniqueId();
        
        
        long currentTime = System.currentTimeMillis();
        long lastFoodTime = lastFoodCollectionTime.getOrDefault(playerId, 0L);
        
        
        if (currentTime - lastFoodTime <= COMBO_TIMEOUT) {
            
            int combo = comboCounts.getOrDefault(playerId, 0) + 1;
            comboCounts.put(playerId, combo);
            
            
            if (combo >= 3) {
                float pitch = Math.min(1.0f + (combo * 0.1f), 2.0f);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, pitch);
            }
            
            
            if (combo == 5) {
                increaseSpeed(session);
            } else if (combo == 10) {
                increaseSpeed(session);
            }
        } else {
            
            comboCounts.put(playerId, 1);
        }
        
        
        lastFoodCollectionTime.put(playerId, currentTime);
    }
    
    
    private void increaseSpeed(GameSession session) {
        Player player = session.getPlayer();
        UUID playerId = player.getUniqueId();
        
        
        if (session.getSpeed() >= SPEED_TICKS.length) return;
        
        
        int newSpeed = session.getSpeed() + 1;
        session.setSpeed(newSpeed);
        
        
        player.sendTitle("", "§b§lSPEED UP! §f→ §bLevel " + newSpeed, 10, 30, 10);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.5f, 1.0f);
        
        
        if (gameLoops.containsKey(playerId)) {
            gameLoops.get(playerId).cancel();
            gameLoops.remove(playerId);
            startGameLoop(player);
        }
    }
    
    
    private void checkMilestones(GameSession session) {
        Player player = session.getPlayer();
        int score = session.getScore();
        
        
        if (score == 10) {
            player.sendTitle("§e§lMILESTONE", "§a10 Points Reached!", 10, 40, 10);
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        } else if (score == 25) {
            player.sendTitle("§e§lIMPRESSIVE", "§a25 Points Reached!", 10, 40, 10);
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
        } else if (score == 50) {
            player.sendTitle("§6§lAMAZING", "§a50 Points Reached!", 10, 40, 10);
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.5f);
        } else if (score == 100) {
            player.sendTitle("§d§lLEGENDARY", "§a100 Points Reached!", 10, 40, 10);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_DEATH, 0.5f, 1.0f);
        }
        
        
        int length = session.getSnakeLength();
        if (length == 15) {
            player.sendTitle("", "§a§lSnake Growing!", 10, 30, 10);
        } else if (length == 30) {
            player.sendTitle("", "§6§lMassive Snake!", 10, 30, 10);
        }
    }
    
    public void setDirection(Player player, Direction direction) {
        GameSession session = getSession(player);
        if (session == null || session.getSnake() == null) {
            return;
        }
        
        
        if (session.getSnake().isValidMove(direction)) {
            
            Direction oldDirection = session.getSnake().getDirection();
            
            
            session.getSnake().setDirection(direction);
            
            
            SoundEffects.playDirection(player);
            
            
            if (!direction.equals(oldDirection) && session.getPlayerState() == PlayerState.PLAYING) {
                
                String arrow = "";
                switch (direction) {
                    case UP:
                        arrow = "⬇";
                        break;
                    case DOWN:
                        arrow = "⬆";
                        break;
                    case LEFT:
                        arrow = "➡";
                        break;
                    case RIGHT:
                        arrow = "⬅";
                        break;
                }
                
                
                player.sendTitle("", "§f" + arrow, 5, 20, 5);
            }
        }
    }
    
    public void cleanup() {
        
        for (BukkitTask task : gameLoops.values()) {
            task.cancel();
        }
        gameLoops.clear();
        
        
        for (GameAreaBuilder area : gameAreas.values()) {
            area.destroyGameArea();
        }
        gameAreas.clear();
        
        
        for (Location loc : temporaryBlocks.values()) {
            loc.getBlock().setType(Material.AIR);
        }
        temporaryBlocks.clear();
        
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            StatsBarManager.removeTutorialBossBar(player);
        }
        
        
        gameSessions.clear();
    }
    
    
    private void updatePowerUpStatus(GameSession session) {
        if (session == null) return;
        
        
        if (session.isPowerUpActive() && System.currentTimeMillis() >= session.getPowerUpExpireTime()) {
            
            Player player = session.getPlayer();
            PowerUpType expiredType = session.getActivePowerUp();
            
            player.sendTitle("", expiredType.getColor() + expiredType.getDisplayName() + " §fEnded!", 5, 20, 5);
            
            
            session.clearPowerUp();
            
            
            if (expiredType == PowerUpType.SPEED_BOOST) {
                restartGameLoopWithCurrentSpeed(session);
            }
        }
    }
    
    
    private void checkPowerUpSpawn(GameSession session) {
        
        if (session.getPowerUpPosition() != null || session.getActivePowerUp() != null) {
            return;
        }
        
        
        if (random.nextInt(POWER_UP_CHANCE) == 0) {
            session.generatePowerUp();
            
            
            session.getPlayer().sendTitle("", "§d§lPower-Up Spawned!", 5, 30, 5);
        }
    }
    
    
    private void handlePowerUpCollection(GameSession session) {
        if (session.checkPowerUpCollision()) {
            Player player = session.getPlayer();
            
            
            PowerUpType type = PowerUpType.getRandomPowerUp();
            
            
            session.applyPowerUp(type);
            
            
            player.sendTitle(
                type.getColor() + type.getDisplayName() + " §fActivated!", 
                type.getDescription(), 
                10, 40, 10
            );
            
            
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.0f, 1.0f);
            
            
            if (type == PowerUpType.SPEED_BOOST) {
                
                int originalSpeed = session.getSpeed();
                session.setSpeed(Math.min(originalSpeed + 2, SPEED_TICKS.length));
                
                
                restartGameLoopWithCurrentSpeed(session);
            }
        }
    }
    
    
    private void restartGameLoopWithCurrentSpeed(GameSession session) {
        Player player = session.getPlayer();
        UUID playerId = player.getUniqueId();
        
        
        if (gameLoops.containsKey(playerId)) {
            gameLoops.get(playerId).cancel();
            gameLoops.remove(playerId);
        }
        
        
        startGameLoop(player);
    }
    
    
    private void checkPowerUpTimeout(GameSession session) {
        if (session.getPowerUpPosition() == null || session.getPowerUpSpawnTime() == 0) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - session.getPowerUpSpawnTime() > POWER_UP_TIMEOUT) {
            
            session.clearPowerUpFromBoard();
            
            
            Player player = session.getPlayer();
            player.sendTitle("", "§7Power-Up Despawned", 5, 20, 5);
        }
    }
    
    
    private void recordGameResult(GameSession session) {
        if (session == null) return;
        
        Player player = session.getPlayer();
        UUID playerUUID = player.getUniqueId();
        int score = session.getScore();
        int length = session.getSnakeLength();
        
        
        long durationSeconds = 0;
        if (session.getStartTime() != null) {
            durationSeconds = Duration.between(session.getStartTime(), Instant.now()).getSeconds();
        }
        
        
        databaseManager.recordGameResult(playerUUID, score, length, (int) durationSeconds);
        
        
        int modifiedScore = score;
        int modifiedLength = length;
        
        if (session.getCoinMultiplier() > 1.0) {
            double multiplier = session.getCoinMultiplier();
            modifiedScore = (int) Math.ceil(score * multiplier);
            modifiedLength = (int) Math.ceil(length * multiplier);
            
            
            player.sendMessage(ChatColor.GOLD + "Coin Multiplier: " + ChatColor.YELLOW + "x" + multiplier);
        }
        
        
        economyManager.rewardGame(player, modifiedScore, modifiedLength);
    }
    
    public void openShop(Player player) {
        if (plugin.getShopGUI() != null) {
            plugin.getShopGUI().openShop(player);
        }
    }
} 