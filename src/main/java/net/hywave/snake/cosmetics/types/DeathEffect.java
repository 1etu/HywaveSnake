package net.hywave.snake.cosmetics.types;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;

import net.hywave.snake.cosmetics.Cosmetic;
import net.hywave.snake.cosmetics.CosmeticType;

import java.util.UUID;

public class DeathEffect extends Cosmetic {
    private final Particle particleType;
    private final int particleCount;
    private final Sound sound;
    private final float volume;
    private final float pitch;
    
    public DeathEffect(String id, String name, int price, Material icon, String description,
                       Particle particleType, int particleCount, Sound sound, float volume, float pitch, boolean isDefault) {
        super(id, name, CosmeticType.DEATH_EFFECT, price, icon, description, isDefault);
        this.particleType = particleType;
        this.particleCount = particleCount;
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
    }
    
    public Particle getParticleType() {
        return particleType;
    }
    
    public int getParticleCount() {
        return particleCount;
    }
    
    public Sound getSound() {
        return sound;
    }
    
    public float getVolume() {
        return volume;
    }
    
    public float getPitch() {
        return pitch;
    }
    
    @Override
    public void apply(UUID playerUUID) {}
} 