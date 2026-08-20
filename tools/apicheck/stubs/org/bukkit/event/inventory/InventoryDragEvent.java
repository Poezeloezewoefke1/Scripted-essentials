package org.bukkit.event.inventory;

import org.bukkit.event.Cancellable;

import java.util.Set;

public class InventoryDragEvent extends InventoryEvent implements Cancellable {
    public Set<Integer> getRawSlots() { throw new UnsupportedOperationException(); }
    @Override public boolean isCancelled() { throw new UnsupportedOperationException(); }
    @Override public void setCancelled(boolean cancel) { }
}
