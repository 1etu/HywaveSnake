package net.hywave.snake.cosmetics.shop;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.hywave.snake.Engine;
import net.hywave.snake.cosmetics.Cosmetic;
import net.hywave.snake.cosmetics.CosmeticManager;
import net.hywave.snake.cosmetics.CosmeticType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ShopGUI {
    private final Engine plugin;
    private final CosmeticManager cosmeticManager;
    
    private final Map<UUID, ShopPage> playerPages = new HashMap<>();
    private final Map<UUID, CosmeticType> playerCategories = new HashMap<>();
    private final Map<UUID, Integer> playerCurrentPage = new HashMap<>();
    
    private static final int ITEMS_PER_PAGE = 21;
    
    public enum ShopPage {
        MAIN_MENU,
        CATEGORY,
        CONFIRMATION
    }
    
    public ShopGUI(Engine plugin, CosmeticManager cosmeticManager) {
        this.plugin = plugin;
        this.cosmeticManager = cosmeticManager;
    }
    
    public void openShop(Player who) {
        cosmeticManager.loadPlayerData(who.getUniqueId());
        
        playerPages.put(who.getUniqueId(), ShopPage.MAIN_MENU);
        openMainMenu(who);
        who.playSound(who.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.0f);
    }
    
    public void openMainMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, "§8Snake Shop");
        
        ItemStack header = createGuiItem(Material.YELLOW_STAINED_GLASS_PANE, "§e§lSNAKE SHOP", "§7Browse and purchase cosmetics");
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, header);
        }
        
        inventory.setItem(4, createGuiItem(
            Material.GOLD_INGOT,
            "§6§lYour Balance",
            "§e" + plugin.getEconomyManager().getCoins(player) + " coins"
        ));
        
        inventory.setItem(19, createGuiItem(
            Material.LIME_CONCRETE,
            "§a§lSnake Skins",
            "§7Change your snake's appearance"
        ));
        
        inventory.setItem(21, createGuiItem(
            Material.BLAZE_POWDER,
            "§6§lTrail Effects",
            "§7Leave particles behind as you move"
        ));
        
        inventory.setItem(23, createGuiItem(
            Material.TNT,
            "§c§lDeath Effects",
            "§7Special effects when your snake dies"
        ));
        
        inventory.setItem(25, createGuiItem(
            Material.POTION,
            "§b§lPower-Ups",
            "§7Special abilities during gameplay"
        ));
        
        ItemStack footer = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", "");
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, footer);
        }
        
        inventory.setItem(49, createGuiItem(
            Material.BARRIER,
            "§c§lClose Shop",
            "§7Click to close the shop"
        ));
        
        player.openInventory(inventory);
    }
    
    public void openCategoryPage(Player player, CosmeticType type, int page) {
        List<Cosmetic> items = cosmeticManager.getCosmeticsByType(type);
        int max = (int) Math.ceil((double) items.size() / ITEMS_PER_PAGE);
        
        if (page < 0) page = 0;
        if (page >= max) page = max - 1;
        
        playerPages.put(player.getUniqueId(), ShopPage.CATEGORY);
        playerCategories.put(player.getUniqueId(), type);
        playerCurrentPage.put(player.getUniqueId(), page);
        
        Inventory inv = Bukkit.createInventory(null, 54, "§8" + type.getDisplayName());
        
        ItemStack header = createGuiItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "§b§l" + type.getDisplayName(), "§7" + type.getDescription());
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, header);
        }
        
        inv.setItem(4, createGuiItem(
            Material.GOLD_INGOT,
            "§6§lYour Balance",
            "§e" + plugin.getEconomyManager().getCoins(player) + " coins"
        ));
        
        int s_indx = page * ITEMS_PER_PAGE;
        int e_indx = Math.min(s_indx + ITEMS_PER_PAGE, items.size());
        
        Cosmetic equippped = cosmeticManager.getEquippedCosmetic(player.getUniqueId(), type);
        
        int slot = 9;
        for (int i = s_indx; i < e_indx; i++) {
            Cosmetic cosmetic = items.get(i);
            boolean owned = cosmeticManager.hasCosmetic(player.getUniqueId(), cosmetic.getId());
            boolean isEquipped = equippped != null && 
            equippped.getId().equals(cosmetic.getId());
            
                                inv.setItem(slot, cosmetic.createIcon(owned, isEquipped));
            slot++;
            
            if (slot % 9 == 0) slot += 2;
        }
        
        ItemStack footer = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", "");
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, footer);
        }
        
        inv.setItem(45, createGuiItem(
            Material.ARROW,
            "§e§lBack to Menu",
            "§7Return to main shop menu"
        ));
        
        if (page > 0) {
            inv.setItem(48, createGuiItem(
                Material.PAPER,
                "§e§lPrevious Page",
                "§7Go to page " + page
            ));
        }
        
        if (page < max - 1) {
            inv.setItem(50, createGuiItem(
                Material.PAPER,
                "§e§lNext Page",
                "§7Go to page " + (page + 2)
            ));
        }
        
        inv.setItem(49, createGuiItem(
            Material.BOOK,
            "§f§lPage " + (page + 1) + "/" + max,
            "§7Browsing " + type.getDisplayName()
        ));
        
        player.openInventory(inv);
    }
    
    public void handleInventoryClick(Player player, int slot, Inventory inventory) {
        UUID playerUUID = player.getUniqueId();
        ShopPage currentPage = playerPages.getOrDefault(playerUUID, ShopPage.MAIN_MENU);
        
        if (currentPage == ShopPage.MAIN_MENU) {
            handleMainMenuClick(player, slot);
        } else if (currentPage == ShopPage.CATEGORY) {
            handleCategoryClick(player, slot, inventory);
        }
    }
    
    private void handleMainMenuClick(Player player, int slot) {
        switch (slot) {
            case 19:// skin
                openCategoryPage(player, CosmeticType.SNAKE_SKIN, 0);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                break;
                
            case 21://trail
                openCategoryPage(player, CosmeticType.TRAIL_EFFECT, 0);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                break;
                
            case 23: // death effects
                openCategoryPage(player, CosmeticType.DEATH_EFFECT, 0);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                break;
                
            case 25: // powerups
                openCategoryPage(player, CosmeticType.POWER_UP, 0);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                break;
                
            case 49:
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.5f, 1.0f);
                break;
        }
    }
    
    private void handleCategoryClick(Player player, int slot, Inventory inventory) {
        UUID playerUUID = player.getUniqueId();
        CosmeticType category = playerCategories.get(playerUUID);
        int currentPage = playerCurrentPage.getOrDefault(playerUUID, 0);
        
        if (slot == 45) {
            openMainMenu(player);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            return;
        }
        
        if (slot == 48) {
            if (category != null && currentPage > 0) {
                openCategoryPage(player, category, currentPage - 1);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            }
            return;
        }
        
        if (slot == 50) {
            if (category != null) {
                List<Cosmetic> items = cosmeticManager.getCosmeticsByType(category);
                int maxPages = (int) Math.ceil((double) items.size() / ITEMS_PER_PAGE);
                
                if (currentPage < maxPages - 1) {
                    openCategoryPage(player, category, currentPage + 1);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                }
            }
            return;
        }
        
        if (slot >= 9 && slot < 45 && slot % 9 != 0 && (slot + 1) % 9 != 0) {
            ItemStack clicked = inventory.getItem(slot);
            if (clicked != null && clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()) {
                handleItemClick(player, clicked);
            }
        }
    }
    
    private void handleItemClick(Player player, ItemStack item) {
        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        UUID playerUUID = player.getUniqueId();
        CosmeticType category = playerCategories.get(playerUUID);
        
        Cosmetic selectedCosmetic = null;
        for (Cosmetic cosmetic : cosmeticManager.getCosmeticsByType(category)) {
            if (ChatColor.stripColor(cosmetic.getName()).equals(name)) {
                selectedCosmetic = cosmetic;
                break;
            }
        }
        
        if (selectedCosmetic == null) {
            return;
        }
        
        boolean owned = cosmeticManager.hasCosmetic(playerUUID, selectedCosmetic.getId());
        
        if (owned) {
            Cosmetic currentlyEquipped = cosmeticManager.getEquippedCosmetic(playerUUID, category);
            boolean alreadyEquipped = currentlyEquipped != null && 
                                     currentlyEquipped.getId().equals(selectedCosmetic.getId());
            
            if (alreadyEquipped) {
                player.sendMessage(ChatColor.YELLOW + selectedCosmetic.getName() + " is already equipped!");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.8f);
            } else {
                cosmeticManager.equipCosmetic(playerUUID, selectedCosmetic.getId());
                player.sendMessage(ChatColor.GREEN + "You equipped " + selectedCosmetic.getName() + "!");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
            }
            
            openCategoryPage(player, selectedCosmetic.getType(), playerCurrentPage.getOrDefault(playerUUID, 0));
        } else {
            if (plugin.getEconomyManager().canAfford(player, selectedCosmetic.getPrice())) {
                boolean success = cosmeticManager.purchaseCosmetic(player, selectedCosmetic.getId());
                
                if (success) {
                    player.sendMessage(ChatColor.GREEN + "You purchased " + selectedCosmetic.getName() + "!");
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.0f);
                    
                    cosmeticManager.equipCosmetic(playerUUID, selectedCosmetic.getId());
                    
                    openCategoryPage(player, selectedCosmetic.getType(), playerCurrentPage.getOrDefault(playerUUID, 0));
                } else {
                    player.sendMessage(ChatColor.RED + "Failed to purchase item!");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 0.8f);
                }
            } else {
                player.sendMessage(ChatColor.RED + "You don't have enough coins!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 0.8f);
            }
        }
    }
    
    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(name);
        
        List<String> loreList = new ArrayList<>();
        for (String line : lore) {
            loreList.add(line);
        }
        
        meta.setLore(loreList);
        item.setItemMeta(meta);
        
        return item;
    }
} 