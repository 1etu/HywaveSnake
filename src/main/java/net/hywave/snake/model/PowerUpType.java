package net.hywave.snake.model;

import lombok.Getter;
import org.bukkit.Material;
import net.md_5.bungee.api.ChatColor;

public enum PowerUpType {
    SPEED_BOOST("Speed Boost", ChatColor.AQUA, Material.LIGHT_BLUE_CONCRETE, 
            "Increases snake speed temporarily"),
    
    IMMUNITY("Immunity", ChatColor.GREEN, Material.LIME_CONCRETE, 
            "Prevents death from wall collisions"),
    
    DOUBLE_POINTS("Double Points", ChatColor.GOLD, Material.YELLOW_CONCRETE, 
            "Doubles all points earned"),
    
    GHOST_MODE("Ghost Mode", ChatColor.LIGHT_PURPLE, Material.PURPLE_CONCRETE, 
            "Pass through your own body");
    
    @Getter
    private final String displayName;
    
    @Getter
    private final ChatColor color;
    
    @Getter
    private final Material displayMaterial;
    
    @Getter
    private final String description;
    
    PowerUpType(String displayName, ChatColor color, Material displayMaterial, String description) {
        this.displayName = displayName;
        this.color = color;
        this.displayMaterial = displayMaterial;
        this.description = description;
    }
    
    public static PowerUpType getRandomPowerUp() {
        PowerUpType[] values = PowerUpType.values();
        return values[(int) (Math.random() * values.length)];
    }
} 