package org.bukkit.inventory;

public interface EntityEquipment {
    void setItemInMainHand(ItemStack item);
    ItemStack getItemInMainHand();
    void setItemInOffHand(ItemStack item);
    void setHelmet(ItemStack item);
    void setChestplate(ItemStack item);
    void setLeggings(ItemStack item);
    void setBoots(ItemStack item);
    void setItemInMainHandDropChance(float chance);
    void setHelmetDropChance(float chance);
}
