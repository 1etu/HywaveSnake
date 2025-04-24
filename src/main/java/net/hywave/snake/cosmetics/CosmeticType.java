package net.hywave.snake.cosmetics;

public enum CosmeticType {
    SNAKE_SKIN("Snake Skins", "Change your snake's appearance"),
    TRAIL_EFFECT("Trail Effects", "Leave particles behind as you move"),
    DEATH_EFFECT("Death Effects", "Special effects when your snake dies"),
    POWER_UP("Power-Ups", "Special abilities during gameplay");
    
    private final String displayName;
    private final String description;
    
    CosmeticType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
} 