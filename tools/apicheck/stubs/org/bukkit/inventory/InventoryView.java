package org.bukkit.inventory;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.HumanEntity;

public interface InventoryView {
    Inventory getTopInventory();
    Inventory getBottomInventory();
    HumanEntity getPlayer();
    Component title();
    void close();
    ItemStack getItem(int slot);
    void setItem(int slot, ItemStack item);
    int convertSlot(int rawSlot);
}
