package net.hywave.snake.model;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.block.data.BlockData;
import org.bukkit.Particle;
import org.bukkit.Sound;
import net.hywave.snake.Engine;
import net.hywave.snake.db.DatabaseManager;
import net.hywave.snake.economy.EconomyManager;
import net.hywave.snake.state.GameState;
import net.hywave.snake.state.PlayerState;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Random;
import net.hywave.snake.cosmetics.Cosmetic;
import net.hywave.snake.cosmetics.CosmeticType;
import net.hywave.snake.cosmetics.types.DeathEffect;
import net.hywave.snake.cosmetics.types.PowerUp;
import net.hywave.snake.cosmetics.types.SnakeSkin;
import net.hywave.snake.cosmetics.types.TrailEffect;

import java.util.LinkedList;

public class GameSession {
    @Getter
    private final UUID sessionId;
    
    @Getter
    private final Player player;
    
    @Getter
    private final GameBoard gameBoard;
    
    @Getter
    private Snake snake;
    
    @Getter
    @Setter
    private Position foodPosition;
    
    @Getter
    @Setter
    private int score = 0;
    
    @Getter
    @Setter
    private int speed = 1;
    
    @Getter
    @Setter
    private PlayerState playerState = PlayerState.INITIALIZING;
    
    @Getter
    @Setter
    private GameState gameState = GameState.WAITING;
    
    @Getter
    private Instant startTime;
    
    @Getter
    private final List<Position> wallPositions = new ArrayList<>();
    
    @Getter
    @Setter
    private Position powerUpPosition;
    
    @Getter
    @Setter
    private PowerUpType activePowerUp;
    
    @Getter
    @Setter
    private long powerUpExpireTime;
    
    @Getter
    @Setter
    private long powerUpSpawnTime;
    
    @Getter
    private final Random random = new Random();
    
    @Getter @Setter
    private BlockData customHeadBlock;
    
    @Getter @Setter
    private BlockData customBodyBlock;
    
    @Getter @Setter
    private Particle trailParticle;
    
    @Getter @Setter
    private int trailParticleCount;
    
    @Getter @Setter
    private float trailParticleOffsetX;
    
    @Getter @Setter
    private float trailParticleOffsetY;
    
    @Getter @Setter
    private float trailParticleOffsetZ;
    
    @Getter @Setter
    private float trailParticleSpeed;
    
    @Getter @Setter
    private Particle deathParticle;
    
    @Getter @Setter
    private int deathParticleCount;
    
    @Getter @Setter
    private Sound deathSound;
    
    @Getter @Setter
    private float deathSoundVolume;
    
    @Getter @Setter
    private float deathSoundPitch;
    
    @Getter @Setter
    private double speedBoostMultiplier = 1.0;
    
    @Getter @Setter
    private double coinMultiplier = 1.0;
    
    @Getter @Setter
    private boolean hasShield = false;
    
    private final DatabaseManager databaseManager;
    private final EconomyManager economyManager;
    private final Engine plugin;
    
    public GameSession(Player player) {
        this.sessionId = UUID.randomUUID();
        this.player = player;
        this.gameBoard = new GameBoard();
        this.playerState = PlayerState.INITIALIZING;
        this.gameState = GameState.WAITING;
        
        this.plugin = Engine.getInstance();
        this.databaseManager = plugin.getDatabaseManager();
        this.economyManager = plugin.getEconomyManager();
        
        loadPlayerCosmetics();
    }
    
    public void loadPlayerCosmetics() {
        if (plugin.getCosmeticManager() == null) return;
        
        try {
            this.customHeadBlock = null;
            this.customBodyBlock = null;
            
            Cosmetic snakeSkin = plugin.getCosmeticManager().getEquippedCosmetic(player.getUniqueId(), CosmeticType.SNAKE_SKIN);
            if (snakeSkin instanceof SnakeSkin) {
                SnakeSkin skin = (SnakeSkin) snakeSkin;
                this.customHeadBlock = skin.getHeadBlock();
                this.customBodyBlock = skin.getBodyBlock();
            }
            
            Cosmetic trailEffect = plugin.getCosmeticManager().getEquippedCosmetic(player.getUniqueId(), CosmeticType.TRAIL_EFFECT);
            if (trailEffect instanceof TrailEffect) {
                TrailEffect trail = (TrailEffect) trailEffect;
                this.trailParticle = trail.getParticleType();
                this.trailParticleCount = trail.getCount();
                this.trailParticleOffsetX = trail.getOffsetX();
                this.trailParticleOffsetY = trail.getOffsetY();
                this.trailParticleOffsetZ = trail.getOffsetZ();
                this.trailParticleSpeed = trail.getSpeed();
            }
            
            Cosmetic deathEffect = plugin.getCosmeticManager().getEquippedCosmetic(player.getUniqueId(), CosmeticType.DEATH_EFFECT);
            if (deathEffect instanceof DeathEffect) {
                DeathEffect effect = (DeathEffect) deathEffect;
                this.deathParticle = effect.getParticleType();
                this.deathParticleCount = effect.getParticleCount();
                this.deathSound = effect.getSound();
                this.deathSoundVolume = effect.getVolume();
                this.deathSoundPitch = effect.getPitch();
            }
            
            Cosmetic powerUp = plugin.getCosmeticManager().getEquippedCosmetic(player.getUniqueId(), CosmeticType.POWER_UP);
            if (powerUp instanceof PowerUp) {
                PowerUp pu = (PowerUp) powerUp;
                
                switch (pu.getPowerUpType()) {
                    case SPEED_BOOST:
                        this.speedBoostMultiplier = pu.getStrength();
                        break;
                    case EXTRA_COINS:
                        this.coinMultiplier = pu.getStrength();
                        break;
                    case SHIELD:
                        this.hasShield = true;
                        break;
                    default:
                        break;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("err: cant load cosmetics for session: " + player.getName() + " - " + e.getMessage());
        }
    }
    
    public void initializeGame() {
        score = 0;
        speed = 1;
        
        int centerX = gameBoard.getWidth() / 2;
        int centerY = gameBoard.getHeight() / 2;
        snake = new Snake(new Position(centerX, centerY), Direction.RIGHT);
        
        generateFood();
        
        powerUpPosition = null;
        activePowerUp = null;
        powerUpSpawnTime = 0;
        powerUpExpireTime = 0;
        
        playerState = PlayerState.PLAYING;
        gameState = GameState.RUNNING;
        
        startTime = Instant.now();
    }
    
    public void reset() {
        score = 0;
        snake = null;
        foodPosition = null;
        powerUpPosition = null;
        activePowerUp = null;
        powerUpSpawnTime = 0;
        powerUpExpireTime = 0;
        gameState = GameState.WAITING;
        playerState = PlayerState.MAIN_MENU;
    }
    
    public void generateFood() {
        if (snake == null) return;
        
        Position pos;
        do {
            pos = new Position(
                random.nextInt(gameBoard.getWidth()),
                random.nextInt(gameBoard.getHeight())
            );
        } while (isSnakeAtPosition(pos));
        
        foodPosition = pos;
    }
    
    private boolean isSnakeAtPosition(Position pos) {
        if (snake == null) return false;
        
        for (Position segment : snake.getBody()) {
            if (segment.equals(pos)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean checkFoodCollision() {
        if (snake == null || foodPosition == null) return false;
        
        return snake.getHead().equals(foodPosition);
    }
    
    public boolean isGameOver() {
        if (snake == null) return false;
        
        if (hasShield) {
            Position head = snake.getHead();
            
            boolean wallCollision = !gameBoard.isInBounds(head);
            boolean selfCollision = snake.collides();
            
            if (wallCollision || selfCollision) {
                hasShield = false;
                
                if (player != null && player.isOnline()) {
                    player.sendMessage("§b§lSHIELD ACTIVATED! §eYou've been saved once!");
                    player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.0f);
                }
                
                if (wallCollision) {
                    repositionAfterShieldActivation();
                } else if (selfCollision) {
                    int originalSize = snake.getBody().size();
                    while (snake.collides() && snake.getBody().size() > 2) {
                        snake.shrink();
                    }
                }
                
                return false;
            }
        }
        
        if (isPowerUpActive() && activePowerUp == PowerUpType.IMMUNITY) {
            return snake.collides();
        }
        
        if (isPowerUpActive() && activePowerUp == PowerUpType.GHOST_MODE) {
            Position head = snake.getHead();
            return !gameBoard.isInBounds(head);
        }
        
        if (snake.collides()) {
            return true;
        }
        
        Position head = snake.getHead();
        return !gameBoard.isInBounds(head);
    }
    
    private void repositionAfterShieldActivation() {
        if (snake == null) return;
        
        Position head = snake.getHead();
        Direction dir = snake.getDirection();
        
        Position safePos = head.copy();
        
        if (head.getX() <= 0) {
            safePos.setX(2);
        } else if (head.getX() >= gameBoard.getWidth() - 1) {
            safePos.setX(gameBoard.getWidth() - 3);
        }
        
        if (head.getY() <= 0) {
            safePos.setY(2);
        } else if (head.getY() >= gameBoard.getHeight() - 1) {
            safePos.setY(gameBoard.getHeight() - 3);
        }
        
        LinkedList<Position> newBody = new LinkedList<>();
        newBody.add(safePos);
        
        Direction safeDirection = dir;
        Position nextPos = safePos;
        for (int i = 0; i < 3; i++) {
            nextPos = nextPos.add(safeDirection.getOpposite());
            if (gameBoard.isInBounds(nextPos)) {
                newBody.add(nextPos);
            }
        }
        
        snake.getBody().clear();
        snake.getBody().addAll(newBody);
    }
    
    public int getSnakeLength() {
        return snake != null ? snake.getBody().size() : 0;
    }
    
    public void generatePowerUp() {
        if (snake == null) return;
        
        Position pos;
        do {
            pos = new Position(
                random.nextInt(gameBoard.getWidth()),
                random.nextInt(gameBoard.getHeight())
            );
        } while (isSnakeAtPosition(pos) || 
                (foodPosition != null && foodPosition.equals(pos)));
        
        powerUpPosition = pos;
        powerUpSpawnTime = System.currentTimeMillis();
    }
    
    public boolean checkPowerUpCollision() {
        if (snake == null || powerUpPosition == null) return false;
        
        return snake.getHead().equals(powerUpPosition);
    }
    
    public void applyPowerUp(PowerUpType type) {
        this.activePowerUp = type;
        this.powerUpExpireTime = System.currentTimeMillis() + 10000;
    }
    
    public boolean isPowerUpActive() {
        return activePowerUp != null && System.currentTimeMillis() < powerUpExpireTime;
    }
    
    public void clearPowerUp() {
        activePowerUp = null;
        powerUpPosition = null;
        powerUpSpawnTime = 0;
        powerUpExpireTime = 0;
    }
    
    public void clearPowerUpFromBoard() {
        powerUpPosition = null;
        powerUpSpawnTime = 0;
    }
} 