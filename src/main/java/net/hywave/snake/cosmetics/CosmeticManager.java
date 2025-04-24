package net.hywave.snake.cosmetics;

import net.hywave.snake.Engine;
import net.hywave.snake.cosmetics.types.DeathEffect;
import net.hywave.snake.cosmetics.types.PowerUp;
import net.hywave.snake.cosmetics.types.SnakeSkin;
import net.hywave.snake.cosmetics.types.TrailEffect;
import net.hywave.snake.db.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class CosmeticManager {
    private final Engine plugin;
    private final DatabaseManager databaseManager;
    
    private final Map<String, Cosmetic> cosmetics = new HashMap<>();
    private final Map<UUID, Map<CosmeticType, String>> equippedCosmetics = new HashMap<>();
    private final Map<UUID, List<String>> ownedCosmetics = new HashMap<>();
    
    public CosmeticManager(Engine plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        
        setupCosmeticsTables();
        registerDefaultCosmetics();
    }
    
    private void setupCosmeticsTables() {
        try {
            Connection connection = databaseManager.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS player_cosmetics (" +
                    "player_uuid VARCHAR(36) NOT NULL, " +
                    "cosmetic_id VARCHAR(50) NOT NULL, " +
                    "purchase_date BIGINT NOT NULL, " +
                    "PRIMARY KEY (player_uuid, cosmetic_id), " +
                    "FOREIGN KEY (player_uuid) REFERENCES players(uuid))")) {
                statement.execute();
            }
            
            try (PreparedStatement statement = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS equipped_cosmetics (" +
                    "player_uuid VARCHAR(36) NOT NULL, " +
                    "cosmetic_type VARCHAR(20) NOT NULL, " +
                    "cosmetic_id VARCHAR(50) NOT NULL, " +
                    "PRIMARY KEY (player_uuid, cosmetic_type), " +
                    "FOREIGN KEY (player_uuid) REFERENCES players(uuid))")) {
                statement.execute();
            }
            
        } catch (SQLException e) {
        }
    }
    
    private void registerDefaultCosmetics() {
        registerCosmetic(new SnakeSkin(
            "default_green", "Default Green", 0, Material.LIME_WOOL,
            "The classic green snake skin",
            Bukkit.createBlockData(Material.LIME_CONCRETE),
            Bukkit.createBlockData(Material.LIME_WOOL),
            true
        ));
        
        registerCosmetic(new SnakeSkin(
            "blue_ice", "Blue Ice", 500, Material.BLUE_ICE,
            "A cool blue ice snake",
            Bukkit.createBlockData(Material.BLUE_CONCRETE),
            Bukkit.createBlockData(Material.BLUE_ICE),
            false
        ));
        
        registerCosmetic(new SnakeSkin(
            "red_hot", "Red Hot", 500, Material.RED_CONCRETE,
            "A fiery red snake",
            Bukkit.createBlockData(Material.RED_CONCRETE),
            Bukkit.createBlockData(Material.RED_WOOL),
            false
        ));
        
        registerCosmetic(new SnakeSkin(
            "golden", "Golden", 1000, Material.GOLD_BLOCK,
            "A luxurious golden snake",
            Bukkit.createBlockData(Material.GOLD_BLOCK),
            Bukkit.createBlockData(Material.YELLOW_WOOL),
            false
        ));
        
        registerCosmetic(new TrailEffect(
            "default_trail", "No Trail", 0, Material.GLASS,
            "No particle trail",
            null, 0, 0, 0, 0, 0,
            true
        ));
        
        registerCosmetic(new TrailEffect(
            "flame_trail", "Flame Trail", 800, Material.BLAZE_POWDER,
            "Leave fire behind as you move",
            Particle.FLAME, 3, 0.05f, 0.05f, 0.05f, 0.01f,
            false
        ));
        
        registerCosmetic(new TrailEffect(
            "heart_trail", "Heart Trail", 800, Material.RED_DYE,
            "Spread the love with a heart trail",
            Particle.HEART, 1, 0, 0, 0, 0,
            false
        ));
        
        registerCosmetic(new TrailEffect(
            "rainbow_trail", "Rainbow Trail", 1500, Material.SPECTRAL_ARROW,
            "A dazzling rainbow follows your snake",
            Particle.REDSTONE, 5, 1, 1, 1, 1,
            false
        ));
        
        registerCosmetic(new DeathEffect(
            "default_death", "Simple Pop", 0, Material.BONE,
            "A simple pop when you die",
            Particle.SMOKE_NORMAL, 10, Sound.ENTITY_CHICKEN_EGG, 1.0f, 1.0f,
            true
        ));
        
        registerCosmetic(new DeathEffect(
            "explosion", "Explosion", 700, Material.TNT,
            "Go out with a bang",
            Particle.EXPLOSION_LARGE, 1, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f,
            false
        ));
        
        registerCosmetic(new DeathEffect(
            "firework", "Firework Show", 1200, Material.FIREWORK_ROCKET,
            "A colorful firework display upon death",
            Particle.FIREWORKS_SPARK, 20, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 0.5f,
            false
        ));
        
        registerCosmetic(new PowerUp(
            "speed_boost", "Speed Boost", 600, Material.SUGAR,
            "Temporary speed increase at the start of a match",
            PowerUp.PowerUpType.SPEED_BOOST, 5, 1.5, 1,
            false
        ));
        
        registerCosmetic(new PowerUp(
            "shield", "Protective Shield", 1000, Material.SHIELD,
            "One-time immunity against crashing",
            PowerUp.PowerUpType.SHIELD, 0, 0, 1,
            false
        ));
        
        registerCosmetic(new PowerUp(
            "coin_magnet", "Coin Magnet", 800, Material.GOLD_INGOT,
            "Earn 25% more coins per game",
            PowerUp.PowerUpType.EXTRA_COINS, 0, 1.25, 1,
            false
        ));
        
        plugin.getLogger().info("Cosmetics -> Registered " + cosmetics.size() + " cosmetics");
    }
    
    public void registerCosmetic(Cosmetic cosmetic) {
        cosmetics.put(cosmetic.getId(), cosmetic);
    }
    
    public Cosmetic getCosmetic(String id) {
        return cosmetics.get(id);
    }
    
    public List<Cosmetic> getCosmeticsByType(CosmeticType type) {
        return cosmetics.values().stream()
                .filter(cosmetic -> cosmetic.getType() == type)
                .collect(Collectors.toList());
    }
    
    public boolean hasCosmetic(UUID playerUUID, String cosmeticId) {
        Cosmetic cosmetic = getCosmetic(cosmeticId);
        if (cosmetic != null && cosmetic.isDefault()) {
            return true;
        }
        
        if (ownedCosmetics.containsKey(playerUUID)) {
            return ownedCosmetics.get(playerUUID).contains(cosmeticId);
        }
        
        try {
            Connection connection = databaseManager.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT COUNT(*) AS count FROM player_cosmetics WHERE player_uuid = ? AND cosmetic_id = ?")) {
                statement.setString(1, playerUUID.toString());
                statement.setString(2, cosmeticId);
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getInt("count") > 0;
                    }
                }
            }
        } catch (SQLException e) {
        }
        
        return false;
    }
    
    public boolean purchaseCosmetic(Player player, String cosmeticId) {
        Cosmetic cosmetic = getCosmetic(cosmeticId);
        if (cosmetic == null) {
            return false;
        }
        
        
        if (hasCosmetic(player.getUniqueId(), cosmeticId)) {
            return false;
        }
        
        
        if (!plugin.getEconomyManager().canAfford(player, cosmetic.getPrice())) {
            return false;
        }
        
        
        if (!plugin.getEconomyManager().removeCoins(player, cosmetic.getPrice())) {
            return false;
        }
        
        
        try {
            Connection connection = databaseManager.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO player_cosmetics (player_uuid, cosmetic_id, purchase_date) VALUES (?, ?, ?)")) {
                statement.setString(1, player.getUniqueId().toString());
                statement.setString(2, cosmeticId);
                statement.setLong(3, System.currentTimeMillis());
                
                statement.executeUpdate();
                
                
                ownedCosmetics.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(cosmeticId);
                
                return true;
            }
        } catch (SQLException e) {
            
            
            plugin.getEconomyManager().addCoins(player, cosmetic.getPrice());
            return false;
        }
    }
    
    public void equipCosmetic(UUID playerUUID, String cosmeticId) {
        Cosmetic cosmetic = getCosmetic(cosmeticId);
        if (cosmetic == null) {
            return;
        }
        
        
        if (!hasCosmetic(playerUUID, cosmeticId)) {
            return;
        }
        
        
        try {
            Connection connection = databaseManager.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT OR REPLACE INTO equipped_cosmetics (player_uuid, cosmetic_type, cosmetic_id) VALUES (?, ?, ?)")) {
                statement.setString(1, playerUUID.toString());
                statement.setString(2, cosmetic.getType().name());
                statement.setString(3, cosmeticId);
                
                statement.executeUpdate();
                
                
                equippedCosmetics.computeIfAbsent(playerUUID, k -> new HashMap<>())
                        .put(cosmetic.getType(), cosmeticId);
            }
        } catch (SQLException e) {
        }
    }
    
    public Cosmetic getEquippedCosmetic(UUID playerUUID, CosmeticType type) {
        
        if (equippedCosmetics.containsKey(playerUUID) &&
            equippedCosmetics.get(playerUUID).containsKey(type)) {
            String cosmeticId = equippedCosmetics.get(playerUUID).get(type);
            return getCosmetic(cosmeticId);
        }
        
        
        try {
            Connection connection = databaseManager.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT cosmetic_id FROM equipped_cosmetics WHERE player_uuid = ? AND cosmetic_type = ?")) {
                statement.setString(1, playerUUID.toString());
                statement.setString(2, type.name());
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        String cosmeticId = resultSet.getString("cosmetic_id");
                        
                        
                        equippedCosmetics.computeIfAbsent(playerUUID, k -> new HashMap<>())
                                .put(type, cosmeticId);
                        
                        return getCosmetic(cosmeticId);
                    }
                }
            }
        } catch (SQLException e) {
        }
        
        
        return getDefaultCosmetic(type);
    }
    
    public Cosmetic getDefaultCosmetic(CosmeticType type) {
        return cosmetics.values().stream()
                .filter(cosmetic -> cosmetic.getType() == type && cosmetic.isDefault())
                .findFirst()
                .orElse(null);
    }
    
    public List<String> getOwnedCosmetics(UUID playerUUID) {
        
        if (ownedCosmetics.containsKey(playerUUID)) {
            return new ArrayList<>(ownedCosmetics.get(playerUUID));
        }
        
        
        List<String> owned = new ArrayList<>();
        
        try {
            Connection connection = databaseManager.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT cosmetic_id FROM player_cosmetics WHERE player_uuid = ?")) {
                statement.setString(1, playerUUID.toString());
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        owned.add(resultSet.getString("cosmetic_id"));
                    }
                }
            }
        } catch (SQLException e) {
        }
        
        
        for (Cosmetic cosmetic : cosmetics.values()) {
            if (cosmetic.isDefault()) {
                owned.add(cosmetic.getId());
            }
        }
        
        
        ownedCosmetics.put(playerUUID, owned);
        
        return owned;
    }
    
    public void loadPlayerData(UUID playerUUID) {
        
        getOwnedCosmetics(playerUUID);
        
        
        Map<CosmeticType, String> equipped = new HashMap<>();
        
        try {
            Connection connection = databaseManager.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT cosmetic_type, cosmetic_id FROM equipped_cosmetics WHERE player_uuid = ?")) {
                statement.setString(1, playerUUID.toString());
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        String typeStr = resultSet.getString("cosmetic_type");
                        String cosmeticId = resultSet.getString("cosmetic_id");
                        
                        CosmeticType type = CosmeticType.valueOf(typeStr);
                        equipped.put(type, cosmeticId);
                    }
                }
            }
        } catch (SQLException e) {
        }
        
        
        for (CosmeticType type : CosmeticType.values()) {
            if (!equipped.containsKey(type)) {
                Cosmetic defaultCosmetic = getDefaultCosmetic(type);
                if (defaultCosmetic != null) {
                    equipped.put(type, defaultCosmetic.getId());
                }
            }
        }
        
        equippedCosmetics.put(playerUUID, equipped);
    }
    
    public void clearCache(UUID playerUUID) {
        ownedCosmetics.remove(playerUUID);
        equippedCosmetics.remove(playerUUID);
    }
} 