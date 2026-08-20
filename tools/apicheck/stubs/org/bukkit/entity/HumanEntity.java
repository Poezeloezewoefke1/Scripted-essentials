package org.bukkit.entity;

import org.bukkit.GameMode;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.PlayerInventory;

public interface HumanEntity extends LivingEntity {
    PlayerInventory getInventory();
    Inventory getEnderChest();
    InventoryView getOpenInventory();
    InventoryView openInventory(Inventory inventory);
    void closeInventory();
    GameMode getGameMode();
    void setGameMode(GameMode mode);
    int getExpToLevel();
}
