package org.bukkit.entity;

import org.bukkit.GameMode;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.PlayerInventory;

public interface HumanEntity extends LivingEntity, InventoryHolder {
    PlayerInventory getInventory();
    Inventory getEnderChest();
    InventoryView getOpenInventory();
    InventoryView openInventory(Inventory inventory);
    void closeInventory();
    GameMode getGameMode();
    void setGameMode(GameMode mode);
    int getExpToLevel();
}
