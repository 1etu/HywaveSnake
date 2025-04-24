package net.hywave.snake.cosmetics.types;

import org.bukkit.Material;

import net.hywave.snake.cosmetics.Cosmetic;
import net.hywave.snake.cosmetics.CosmeticType;

import java.util.UUID;

public class PowerUp extends Cosmetic {
    public enum PowerUpType {
        SPEED_BOOST("Speed Boost", "Temporary speed increase at start"),
        SLOW_MOTION("Slow Motion", "Slows down enemy snakes for a few seconds"), 
        SHIELD("Shield", "One-time immunity against crashing"),
        EXTRA_COINS("Extra Coins", "Earn more coins per game"),
        LUCKY_START("Lucky Start", "Random bonus at match start");
        
        private final String name;
        private final String description;
        
        PowerUpType(String name, String description) {
            this.name = name;
            this.description = description;
        }
        
        public String getName() {
            return name;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private final PowerUpType powerUpType;
    private final int duration;
    private final double strength;
    private final int usages;
    
    public PowerUp(String id, String name, int price, Material icon, String description,
                  PowerUpType powerUpType, int duration, double strength, int usages, boolean isDefault) {
        super(id, name, CosmeticType.POWER_UP, price, icon, description, isDefault);
        this.powerUpType = powerUpType;
        this.duration = duration;
        this.strength = strength;
        this.usages = usages;
    }
    
    public PowerUpType getPowerUpType() {
        return powerUpType;
    }
    
    public int getDuration() {
        return duration;
    }
    
    public double getStrength() {
        return strength;
    }
    
    public int getUsages() {
        return usages;
    }
    
    @Override
    public void apply(UUID playerUUID) {}
} 