package net.hywave.snake.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

import net.hywave.snake.Engine;
import net.hywave.snake.cosmetics.shop.ShopGUI;

public class ShopListener implements Listener {
    private final Engine plugin;
    private final ShopGUI shopGUI;
    
    public ShopListener(Engine plugin, ShopGUI shopGUI) {
        this.plugin = plugin;
        this.shopGUI = shopGUI;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().startsWith("§8Snake Shop") || 
            event.getView().getTitle().startsWith("§8Snake Skins") ||
            event.getView().getTitle().startsWith("§8Trail Effects") ||
            event.getView().getTitle().startsWith("§8Death Effects") ||
            event.getView().getTitle().startsWith("§8Power-Ups")) {
            
            event.setCancelled(true);
            
            if (event.getWhoClicked() instanceof Player && event.getCurrentItem() != null) {
                Player player = (Player) event.getWhoClicked();
                shopGUI.handleInventoryClick(player, event.getRawSlot(), event.getClickedInventory());
            }
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getCosmeticManager().clearCache(player.getUniqueId());
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.HAND && 
            event.getAction().name().contains("RIGHT_CLICK") && 
            event.getPlayer().isSneaking()) {
            shopGUI.openShop(event.getPlayer());
            event.setCancelled(true);
        }
    }
} 