package org.bukkit.inventory;

import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;

import java.util.HashMap;
import java.util.List;

public interface Inventory extends Iterable<ItemStack> {
    int getSize();
    ItemStack getItem(int index);
    void setItem(int index, ItemStack item);
    HashMap<Integer, ItemStack> addItem(ItemStack... items);
    HashMap<Integer, ItemStack> removeItem(ItemStack... items);
    ItemStack[] getContents();
    void setContents(ItemStack[] items);
    ItemStack[] getStorageContents();
    void setStorageContents(ItemStack[] items);
    void clear();
    void clear(int index);
    boolean contains(Material material);
    int first(Material material);
    int firstEmpty();
    boolean isEmpty();
    List<HumanEntity> getViewers();
    InventoryHolder getHolder();
    int getMaxStackSize();
}
