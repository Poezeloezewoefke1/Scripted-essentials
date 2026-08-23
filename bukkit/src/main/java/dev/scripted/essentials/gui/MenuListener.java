package dev.scripted.essentials.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

/** Routes inventory events for plugin menus and blocks item movement out of them. */
public final class MenuListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Menu menu)) {
            return;
        }
        Inventory clicked = event.getClickedInventory();
        boolean clickedMenu = clicked != null && clicked.equals(event.getView().getTopInventory());

        if (clickedMenu) {
            event.setCancelled(true);
            menu.handleClick(event);
            return;
        }
        // A click in the player's own inventory: shift-clicking would push items into the menu.
        if (!menu.allowBottomInventory() || event.isShiftClick()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof Menu menu)) {
            return;
        }
        int topSize = event.getView().getTopInventory().getSize();
        boolean touchesMenu = event.getRawSlots().stream().anyMatch(slot -> slot < topSize);
        if (touchesMenu || !menu.allowBottomInventory()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof Menu menu) {
            menu.fireClose(event);
        }
    }
}
