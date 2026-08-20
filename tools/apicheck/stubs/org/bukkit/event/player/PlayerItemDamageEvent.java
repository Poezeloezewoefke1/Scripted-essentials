package org.bukkit.event.player;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.inventory.ItemStack;

public class PlayerItemDamageEvent extends PlayerEvent implements Cancellable {
    public PlayerItemDamageEvent(Player who) { super(who); }
    public ItemStack getItem() { throw new UnsupportedOperationException(); }
    public int getDamage() { throw new UnsupportedOperationException(); }
    public void setDamage(int damage) { }
    @Override public boolean isCancelled() { throw new UnsupportedOperationException(); }
    @Override public void setCancelled(boolean cancel) { }
}
