package org.bukkit.inventory;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

public class ItemStack implements Cloneable {
    public ItemStack(Material type) { }
    public ItemStack(Material type, int amount) { }
    public Material getType() { throw new UnsupportedOperationException(); }
    public void setType(Material type) { }
    public int getAmount() { throw new UnsupportedOperationException(); }
    public void setAmount(int amount) { }
    public ItemMeta getItemMeta() { throw new UnsupportedOperationException(); }
    public boolean setItemMeta(ItemMeta meta) { throw new UnsupportedOperationException(); }
    public boolean hasItemMeta() { throw new UnsupportedOperationException(); }
    public int getMaxStackSize() { throw new UnsupportedOperationException(); }
    public void addUnsafeEnchantment(Enchantment enchantment, int level) { }
    public void addEnchantment(Enchantment enchantment, int level) { }
    public void removeEnchantment(Enchantment enchantment) { }
    public Map<Enchantment, Integer> getEnchantments() { throw new UnsupportedOperationException(); }
    public boolean containsEnchantment(Enchantment enchantment) { throw new UnsupportedOperationException(); }
    public boolean isSimilar(ItemStack other) { throw new UnsupportedOperationException(); }
    public byte[] serializeAsBytes() { throw new UnsupportedOperationException(); }
    public static ItemStack deserializeBytes(byte[] bytes) { throw new UnsupportedOperationException(); }
    @Override public ItemStack clone() { throw new UnsupportedOperationException(); }
}
