package org.bukkit.inventory;

public interface PlayerInventory extends Inventory {
    ItemStack getItemInMainHand();
    void setItemInMainHand(ItemStack item);
    ItemStack getItemInOffHand();
    void setItemInOffHand(ItemStack item);
    int getHeldItemSlot();
    void setHeldItemSlot(int slot);
    ItemStack[] getArmorContents();
    void setArmorContents(ItemStack[] items);
    ItemStack[] getExtraContents();
    ItemStack getHelmet();
    void setHelmet(ItemStack helmet);
    ItemStack getChestplate();
    void setChestplate(ItemStack chestplate);
    ItemStack getLeggings();
    void setLeggings(ItemStack leggings);
    ItemStack getBoots();
    void setBoots(ItemStack boots);
}
