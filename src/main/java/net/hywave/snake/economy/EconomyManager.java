package net.hywave.snake.economy;

import lombok.Getter;
import net.hywave.snake.Engine;
import net.hywave.snake.db.DatabaseManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EconomyManager {
    private final Engine plugin;
    
    @Getter
    private final DatabaseManager databaseManager;
    
    private final Map<UUID, Integer> coinsCache = new HashMap<>();
    
    public EconomyManager(Engine plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }
    
    public int getCoins(Player player) {
        return getCoins(player.getUniqueId());
    }
    
    public int getCoins(UUID playerUUID) {
        if (coinsCache.containsKey(playerUUID)) {
            return coinsCache.get(playerUUID);
        }
        
        int coins = databaseManager.getPlayerCoins(playerUUID);
        coinsCache.put(playerUUID, coins);
        return coins;
    }
    
    public void setCoins(UUID playerUUID, int amount) {
        if (amount < 0) amount = 0;
        
        databaseManager.setPlayerCoins(playerUUID, amount);
        coinsCache.put(playerUUID, amount);
    }
    
    public void addCoins(Player player, int amount) {
        addCoins(player.getUniqueId(), amount);
        player.sendMessage(ChatColor.GOLD + "+" + amount + " coins! " + 
                ChatColor.YELLOW + "Total: " + getCoins(player));
    }
    
    public void addCoins(UUID playerUUID, int amount) {
        if (amount <= 0) return;
        
        databaseManager.addPlayerCoins(playerUUID, amount);
        
        if (coinsCache.containsKey(playerUUID)) {
            coinsCache.put(playerUUID, coinsCache.get(playerUUID) + amount);
        } else {
            coinsCache.put(playerUUID, databaseManager.getPlayerCoins(playerUUID));
        }
    }
    
    public boolean removeCoins(Player player, int amount) {
        boolean result = removeCoins(player.getUniqueId(), amount);
        if (result) {
            player.sendMessage(ChatColor.RED + "-" + amount + " coins! " + 
                    ChatColor.YELLOW + "Remaining: " + getCoins(player));
        } else {
            player.sendMessage(ChatColor.RED + "You don't have enough coins!");
        }
        return result;
    }
    
    public boolean removeCoins(UUID playerUUID, int amount) {
        if (amount <= 0) return true;
        
        if (!canAfford(playerUUID, amount)) {
            return false;
        }
        
        boolean result = databaseManager.removePlayerCoins(playerUUID, amount);
        
        if (result && coinsCache.containsKey(playerUUID)) {
            coinsCache.put(playerUUID, coinsCache.get(playerUUID) - amount);
        }
        
        return result;
    }
    
    public boolean canAfford(Player player, int amount) {
        return canAfford(player.getUniqueId(), amount);
    }
    
    public boolean canAfford(UUID playerUUID, int amount) {
        if (coinsCache.containsKey(playerUUID)) {
            return coinsCache.get(playerUUID) >= amount;
        }
        
        return databaseManager.playerHasEnoughCoins(playerUUID, amount);
    }
    
    public void refreshCache(UUID playerUUID) {
        coinsCache.put(playerUUID, databaseManager.getPlayerCoins(playerUUID));
    }
    
    public void clearCache(UUID playerUUID) {
        coinsCache.remove(playerUUID);
    }
    
    public void rewardGame(Player player, int score, int length) {
        int coinsEarned = calculateGameReward(score, length);
        addCoins(player, coinsEarned);
    }
    
    private int calculateGameReward(int score, int length) {
        return score + (length / 2);
    }
} 