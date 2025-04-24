package net.hywave.snake.cosmetics;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class Cosmetic {
    private final String id;
    private final String name;
    private final CosmeticType type;
    private final int price;
    private final Material icon;
    private final String description;
    private final boolean isDefault;
    
    public Cosmetic(String id, String name, CosmeticType type, int price, Material icon, String description, boolean isDefault) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.price = price;
        this.icon = icon;
        this.description = description;
        this.isDefault = isDefault;
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public CosmeticType getType() {
        return type;
    }
    
    public int getPrice() {
        return price;
    }
    
    public Material getIcon() {
        return icon;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isDefault() {
        return isDefault;
    }
    
    public ItemStack createIcon(boolean owned) {
        return createIcon(owned, false);
    }
    
    public ItemStack createIcon(boolean owned, boolean equipped) {
        ItemStack itemStack = new ItemStack(icon);
        ItemMeta meta = itemStack.getItemMeta();
        
        String displayName = name;
        if (equipped) {
            displayName = "§a" + name + " §2✓";
        } else {
            displayName = "§a" + name;
        }
        
        meta.setDisplayName(displayName);
        
        List<String> lore = new ArrayList<>();
        lore.add("§7" + description);
        lore.add("");
        
        if (owned) {
            lore.add("§aOwned");
            if (equipped) {
                lore.add("§2Currently Equipped");
            } else {
                lore.add("§eClick to equip");
            }
        } else {
            lore.add("§6Price: §e" + price + " coins");
            lore.add("§eClick to purchase");
        }
        
        meta.setLore(lore);
        itemStack.setItemMeta(meta);
        
        return itemStack;
    }
    
    public abstract void apply(UUID playerUUID);
} 