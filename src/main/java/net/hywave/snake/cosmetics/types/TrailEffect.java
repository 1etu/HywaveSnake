package net.hywave.snake.cosmetics.types;

import org.bukkit.Material;
import org.bukkit.Particle;

import net.hywave.snake.cosmetics.Cosmetic;
import net.hywave.snake.cosmetics.CosmeticType;

import java.util.UUID;

public class TrailEffect extends Cosmetic {
    private final Particle particleType;
    private final int count;
    private final float offsetX;
    private final float offsetY;
    private final float offsetZ;
    private final float speed;
    
    public TrailEffect(String id, String name, int price, Material icon, String description,
                       Particle particleType, int count, float offsetX, float offsetY, float offsetZ, float speed, boolean isDefault) {
        super(id, name, CosmeticType.TRAIL_EFFECT, price, icon, description, isDefault);
        this.particleType = particleType;
        this.count = count;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.speed = speed;
    }
    
    public Particle getParticleType() {
        return particleType;
    }
    
    public int getCount() {
        return count;
    }
    
    public float getOffsetX() {
        return offsetX;
    }
    
    public float getOffsetY() {
        return offsetY;
    }
    
    public float getOffsetZ() {
        return offsetZ;
    }
    
    public float getSpeed() {
        return speed;
    }
    
    @Override
    public void apply(UUID playerUUID) {}
} 