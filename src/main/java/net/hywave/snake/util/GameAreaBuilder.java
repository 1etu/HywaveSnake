package net.hywave.snake.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import net.hywave.snake.Engine;
import net.hywave.snake.model.GameBoard;
import net.hywave.snake.model.GameSession;
import net.hywave.snake.model.Position;
import net.hywave.snake.state.PlayerState;
import net.hywave.snake.model.PowerUpType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameAreaBuilder {
    private final Engine plugin;
    private final Player player;
    private final GameBoard gameBoard;
    private final int offsetX;
    private final int offsetZ;
    
    private static final int GAME_Y = 100;
    private static final int PLAYER_HEIGHT = 15;

    private Location originLocation;
    
    private final List<BlockDisplay> displays = new ArrayList<>();
    private TextDisplay menutexti;
    private TextDisplay scoretexti;
    private final Map<Position, BlockDisplay> boardBlocks = new HashMap<>();
    
    public GameAreaBuilder(Engine plugin, Player player, GameBoard gameBoard, int offsetX, int offsetZ) {
        this.plugin = plugin;
        this.player = player;
        this.gameBoard = gameBoard;
        this.offsetX = offsetX;
        this.offsetZ = offsetZ;
    }
    
    public void buildGameArea(Runnable completionCallback) {
        World world = plugin.getGameWorld();
        if (world == null) {
            plugin.getLogger().severe("err with building game area");
            return;
        }
        
        originLocation = new Location(world, offsetX, GAME_Y, offsetZ);
        
        configurePlayer();
        positionPlayer();
        
        
        if (!world.isChunkLoaded(originLocation.getBlockX() >> 4, originLocation.getBlockZ() >> 4)) {
            world.loadChunk(originLocation.getBlockX() >> 4, originLocation.getBlockZ() >> 4);
        }
        
        
        cleanupExistingEntities();
        
        
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            buildFloorAndWalls();
            
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                buildGameBoard();
                
                
                Bukkit.getScheduler().runTaskLater(plugin, () -> {

                    
                    createGameMenu();;
                    createScoreText();
                    
                    
                    positionPlayer();
                    
                    
                    if (completionCallback != null) {
                        completionCallback.run();
                    }
                }, 5L);
            }, 5L);
        }, 5L);
    }
    
    private void configurePlayer() {
        
        player.setGameMode(org.bukkit.GameMode.SPECTATOR);
        
        
        player.setAllowFlight(true);
        player.setFlying(true);
        
        
        
        player.setFlySpeed(0.1f);
        player.setWalkSpeed(0.1f);
        
        
        player.setFallDistance(0);
        
        
        centerPlayerView(player);
    }
    
    private void positionPlayer() {
        
        centerPlayerView(player);
    }
    
    private void cleanupExistingEntities() {
        
        for (BlockDisplay display : displays) {
            if (display != null && !display.isDead()) {
                display.remove();
            }
        }
        displays.clear();
        boardBlocks.clear();
        
        if (menutexti != null && !menutexti.isDead()) {
            menutexti.remove();
            menutexti = null;
        }
        
        if (scoretexti != null && !scoretexti.isDead()) {
            scoretexti.remove();
            scoretexti = null;
        }
        
        
        player.getWorld().getEntities().stream()
            .filter(entity -> entity instanceof Display)
            .filter(entity -> entity.getLocation().distance(originLocation) < 30)
            .forEach(entity -> {
                try {
                    entity.remove();
                } catch (Exception e) {
                }
            });
    }
    
    private void buildFloorAndWalls() {
        World world = originLocation.getWorld();
        int width = gameBoard.getWidth();
        int height = gameBoard.getHeight();
        
        
        for (int x = -2; x <= width + 1; x++) {
            for (int z = -2; z <= height + 1; z++) {
                Location floorLoc = originLocation.clone().add(x, -1, z);
                createBlock(world, floorLoc, Material.GRAY_CONCRETE);
            }
        }
        
        
        for (int x = -1; x <= width; x++) {
            for (int z = -1; z <= height; z++) {
                if (x == -1 || x == width || z == -1 || z == height) {
                    Location borderLoc = originLocation.clone().add(x, 0, z);
                    createWallBlock(world, borderLoc);
                }
            }
        }
    }
    
    private void createWallBlock(World world, Location location) {
        BlockDisplay display = createBlock(world, location, Material.BLACK_CONCRETE);
        displays.add(display);
    }
    
    private void buildGameBoard() {
        World world = originLocation.getWorld();
        
        
        for (int x = 0; x < gameBoard.getWidth(); x++) {
            for (int z = 0; z < gameBoard.getHeight(); z++) {
                Location blockLoc = originLocation.clone().add(x, 0, z);
                BlockDisplay display = createBlock(world, blockLoc, Material.BLACK_WOOL);
                
                Position pos = new Position(x, z);
                boardBlocks.put(pos, display);
                displays.add(display);
            }
        }
    }
    
    private BlockDisplay createBlock(World world, Location location, Material material) {
        BlockDisplay display = world.spawn(location, BlockDisplay.class, entity -> {
            entity.setBlock(Bukkit.createBlockData(material));
            
            
            Transformation transformation = new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 0),
                    new Vector3f(1, 1, 1),
                    new AxisAngle4f(0, 0, 0, 0)
            );
            entity.setTransformation(transformation);
            
            
            entity.setVisibleByDefault(false);
            try {
                player.showEntity(plugin, entity);
            } catch (Exception e) {
            }
        });
        
        return display;
    }
    
    @SuppressWarnings("deprecation")
    private void createGameMenu() {
        World world = originLocation.getWorld();
        
        double cx = originLocation.getX() + (gameBoard.getWidth() / 2.0);
        double cz = originLocation.getZ() + (gameBoard.getHeight() / 2.0);
        
        Location textlocation = new Location(world, cx, originLocation.getY() + 1, cz);
        
        GameSession session = plugin.getGameManager().getSession(player);
        PlayerState state = session != null ? session.getPlayerState() : PlayerState.MAIN_MENU;
        
        String menuuuu;
        if (state == PlayerState.MAIN_MENU) {
            menuuuu = "§eLeft-Click to Start\n§7Right-Click for Settings";
        } else if (state == PlayerState.GAME_OVER) {
            int score = session != null ? session.getScore() : 0;
            menuuuu = "§eScore: §6" + score + "\n§7Click to play again";
        } else if (state == PlayerState.LEADERBOARD) {
            menuuuu = "§7Press D to return";
        } else if (state == PlayerState.SETTINGS) {
            menuuuu = "§7Click to toggle options";
        } else {
            menuuuu = "§eLeft-Click to Start\n§7Right-Click for Settings";
        }
        
        menutexti = world.spawn(textlocation, TextDisplay.class, entity -> {
            entity.setText(menuuuu);
            entity.setAlignment(TextDisplay.TextAlignment.CENTER);
            
            entity.setBillboard(Display.Billboard.FIXED);
            entity.setRotation(0, 0);
            
            entity.setBackgroundColor(org.bukkit.Color.fromARGB(100, 0, 0, 0));
            entity.setShadowed(true);
            
            entity.setVisibleByDefault(false);
            try {
                player.showEntity(plugin, entity);
            } catch (Exception e) {
            }
        });
    }
    
    @SuppressWarnings("deprecation")
    private void createScoreText() {
        World world = originLocation.getWorld();
        
        double scoreX = originLocation.getX() + 1;
        double scoreZ = originLocation.getZ() + 1;
        
        Location scorelocation = new Location(world, scoreX, originLocation.getY() + 1, scoreZ);
        
        scoretexti = world.spawn(scorelocation, TextDisplay.class, entity -> {
            entity.setText("§6Score: §e0");
            entity.setAlignment(TextDisplay.TextAlignment.LEFT);
            
            
            entity.setBillboard(Display.Billboard.FIXED);
            entity.setRotation(0, 0); 
            
            
            entity.setBackgroundColor(org.bukkit.Color.fromARGB(80, 0, 0, 0));
            entity.setShadowed(true);
            
            
            entity.setVisibleByDefault(false);
            try {
                player.showEntity(plugin, entity);
            } catch (Exception e) {
            }
        });
    }
    
    public void updateBoard(GameSession session) {
        if (session == null) return;
        
        PlayerState state = session.getPlayerState();
        
        
        if (menutexti != null) {
            if (state == PlayerState.MAIN_MENU || 
                state == PlayerState.LEADERBOARD || 
                state == PlayerState.SETTINGS || 
                state == PlayerState.GAME_OVER) {
                
                
                if (state == PlayerState.MAIN_MENU) {
                    menutexti.setText("§6§lSNAKE GAME");
                } else if (state == PlayerState.GAME_OVER) {
                    menutexti.setText("§c§lGAME OVER\n§eScore: §6" + session.getScore());
                } else if (state == PlayerState.LEADERBOARD) {
                    menutexti.setText("§6§lLEADERBOARD");
                } else if (state == PlayerState.SETTINGS) {
                    menutexti.setText("§6§lSETTINGS");
                }
                
                
                try {
                    player.showEntity(plugin, menutexti);
                } catch (Exception e) {}
                
            } else {
                
                try {
                    player.hideEntity(plugin, menutexti);
                } catch (Exception e) {}
            }
        }
        
        
        if (scoretexti != null) {
            if (state == PlayerState.PLAYING) {
                
                scoretexti.setText("§6Score: §e" + session.getScore());
                
                
                try {
                    player.showEntity(plugin, scoretexti);
                } catch (Exception e) {}
            } else {
                
                try {
                    player.hideEntity(plugin, scoretexti);
                } catch (Exception e) {}
            }
        }
        
        
        for (Position pos : boardBlocks.keySet()) {
            BlockDisplay display = boardBlocks.get(pos);
            display.setBlock(Bukkit.createBlockData(Material.BLACK_WOOL));
        }
        
        
        if (state == PlayerState.PLAYING) {
            
            if (session.getPlayer() != null && plugin.getCosmeticManager() != null) {
                session.loadPlayerCosmetics();
            }
            
            
            if (session.getSnake() != null) {
                for (Position snakePos : session.getSnake().getBody()) {
                    if (boardBlocks.containsKey(snakePos)) {
                        
                        if (session.getCustomBodyBlock() != null) {
                            boardBlocks.get(snakePos).setBlock(session.getCustomBodyBlock());
                        } else {
                            boardBlocks.get(snakePos).setBlock(Bukkit.createBlockData(Material.LIME_WOOL));
                        }
                        
                        
                        if (session.getTrailParticle() != null) {
                            Location particleLoc = originLocation.clone().add(
                                snakePos.getX() + 0.5, 
                                0.5, 
                                snakePos.getY() + 0.5
                            );
                            
                            player.spawnParticle(
                                session.getTrailParticle(),
                                particleLoc,
                                session.getTrailParticleCount(),
                                session.getTrailParticleOffsetX(),
                                session.getTrailParticleOffsetY(),
                                session.getTrailParticleOffsetZ(),
                                session.getTrailParticleSpeed()
                            );
                        }
                    }
                }
                
                
                Material headMaterial = Material.GREEN_CONCRETE;
                if (session.isPowerUpActive() && session.getActivePowerUp() == PowerUpType.GHOST_MODE) {
                    headMaterial = Material.EMERALD_BLOCK; 
                }
                
                
                Position head = session.getSnake().getHead();
                if (boardBlocks.containsKey(head)) {
                    if (session.getCustomHeadBlock() != null) {
                        boardBlocks.get(head).setBlock(session.getCustomHeadBlock());
                    } else {
                        boardBlocks.get(head).setBlock(Bukkit.createBlockData(headMaterial));
                    }
                }
            }
            
            
            Position food = session.getFoodPosition();
            if (food != null && boardBlocks.containsKey(food)) {
                boardBlocks.get(food).setBlock(Bukkit.createBlockData(Material.RED_CONCRETE));
            }
            
            
            Position powerUp = session.getPowerUpPosition();
            if (powerUp != null && boardBlocks.containsKey(powerUp)) {
                
                long currentTime = System.currentTimeMillis();
                boolean blink = (currentTime / 500) % 2 == 0;
                
                Material powerUpMaterial = blink ? Material.DIAMOND_BLOCK : Material.EMERALD_BLOCK;
                boardBlocks.get(powerUp).setBlock(Bukkit.createBlockData(powerUpMaterial));
            }
            
            
            if (session.isPowerUpActive()) {
                PowerUpType activeType = session.getActivePowerUp();
                
                
                long remainingMs = session.getPowerUpExpireTime() - System.currentTimeMillis();
                int remainingSeconds = (int) Math.ceil(remainingMs / 1000.0);
                
                
                if (scoretexti != null) {
                    String powerUpText = String.format("%s%s (%ds)",
                            activeType.getColor(), activeType.getDisplayName(), remainingSeconds);
                    scoretexti.setText("§6Score: §e" + session.getScore() + "\n" + powerUpText);
                }
            }
        } 
        
        else if (state == PlayerState.MAIN_MENU) {
            
            renderDecorativeSnake();
        }
    }
    
    
    private void renderDecorativeSnake() {
        int width = gameBoard.getWidth();
        int height = gameBoard.getHeight();
        
        
        for (int i = 0; i < Math.min(width, height) / 2; i++) {
            int x = width / 2 + i;
            int y = height / 2;
            
            Position pos = new Position(x, y);
            if (boardBlocks.containsKey(pos)) {
                boardBlocks.get(pos).setBlock(Bukkit.createBlockData(Material.LIME_WOOL));
            }
        }
        
        
        Position head = new Position(width / 2 + Math.min(width, height) / 2, height / 2);
        if (boardBlocks.containsKey(head)) {
            boardBlocks.get(head).setBlock(Bukkit.createBlockData(Material.GREEN_CONCRETE));
        }
        
        
        Position food = new Position(width / 2 - 2, height / 2);
        if (boardBlocks.containsKey(food)) {
            boardBlocks.get(food).setBlock(Bukkit.createBlockData(Material.RED_CONCRETE));
        }
    }
    
    public void destroyGameArea() {
        
        for (BlockDisplay display : displays) {
            if (display != null && !display.isDead()) {
                try {
                    display.remove();
                } catch (Exception e) {
                }
            }
        }
        displays.clear();
        boardBlocks.clear();
        
        
        if (menutexti != null && !menutexti.isDead()) {
            try {
                menutexti.remove();
            } catch (Exception e) {
            }
            menutexti = null;
        }
        
        
        if (scoretexti != null && !scoretexti.isDead()) {
            try {
                scoretexti.remove();
            } catch (Exception e) {
            }
            scoretexti = null;
        }
        
        
        if (player.isOnline()) {
            player.setFlying(false);
            player.setAllowFlight(false);
            player.setWalkSpeed(0.2f); 
            player.setFlySpeed(0.1f);  
            player.setGameMode(org.bukkit.GameMode.SPECTATOR);
            
            
            World gameWorld = plugin.getGameWorld();
            if (gameWorld != null) {
                player.teleport(gameWorld.getSpawnLocation());
            }
        }
    }
    
    public void centerPlayerView(Player player) {
        if (originLocation == null || player == null) return;
        
        World world = originLocation.getWorld();
        if (world == null) return;
        
        
        double centerX = originLocation.getX() + (gameBoard.getWidth() / 2.0);
        double centerZ = originLocation.getZ() + (gameBoard.getHeight() / 2.0);
        
        
        Location playerLoc = new Location(world, centerX, originLocation.getY() + PLAYER_HEIGHT, centerZ);
        
        
        playerLoc.setPitch(90f);
        playerLoc.setYaw(0f);
        
        
        player.teleport(playerLoc);
    }
    
    public void cleanupGameElements() {
        
        for (Position pos : boardBlocks.keySet()) {
            BlockDisplay display = boardBlocks.get(pos);
            if (display != null && !display.isDead()) {
                display.setBlock(Bukkit.createBlockData(Material.BLACK_WOOL));
            }
        }
        
        
        if (menutexti != null && !menutexti.isDead()) {
            try {
                player.hideEntity(plugin, menutexti);
            } catch (Exception e) {
                
            }
        }
        
        if (scoretexti != null && !scoretexti.isDead()) {
            try {
                player.hideEntity(plugin, scoretexti);
            } catch (Exception e) {
                
            }
        }
        
        
        centerPlayerView(player);
    }
    
    public Location getOriginLocation() {
        return originLocation;
    }
} 