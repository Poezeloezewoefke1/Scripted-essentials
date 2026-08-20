package org.bukkit.event.inventory;

import org.bukkit.entity.HumanEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class InventoryClickEvent extends InventoryEvent implements Cancellable {
    public HumanEntity getWhoClicked() { throw new UnsupportedOperationException(); }
    public int getRawSlot() { throw new UnsupportedOperationException(); }
    public int getSlot() { throw new UnsupportedOperationException(); }
    public Inventory getClickedInventory() { throw new UnsupportedOperationException(); }
    public ItemStack getCurrentItem() { throw new UnsupportedOperationException(); }
    public void setCurrentItem(ItemStack item) { }
    public ItemStack getCursor() { throw new UnsupportedOperationException(); }
    public ClickType getClick() { throw new UnsupportedOperationException(); }
    public InventoryAction getAction() { throw new UnsupportedOperationException(); }
    public boolean isShiftClick() { throw new UnsupportedOperationException(); }
    public boolean isLeftClick() { throw new UnsupportedOperationException(); }
    public boolean isRightClick() { throw new UnsupportedOperationException(); }
    @Override public boolean isCancelled() { throw new UnsupportedOperationException(); }
    @Override public void setCancelled(boolean cancel) { }
}
