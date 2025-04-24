package net.hywave.snake.cosmetics.types;

import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

import net.hywave.snake.cosmetics.Cosmetic;
import net.hywave.snake.cosmetics.CosmeticType;

import java.util.UUID;

public class SnakeSkin extends Cosmetic {
    private final BlockData headBlock;
    private final BlockData bodyBlock;
    
    public SnakeSkin(String id, String name, int price, Material icon, String description, 
                     BlockData headBlock, BlockData bodyBlock, boolean isDefault) {
        super(id, name, CosmeticType.SNAKE_SKIN, price, icon, description, isDefault);
        this.headBlock = headBlock;
        this.bodyBlock = bodyBlock;
    }
    
    public BlockData getHeadBlock() {
        return headBlock;
    }
    
    public BlockData getBodyBlock() {
        return bodyBlock;
    }
    
    @Override
    public void apply(UUID playerUUID) {}
} 