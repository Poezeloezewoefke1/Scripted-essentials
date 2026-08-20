package org.bukkit.inventory.meta;

import net.kyori.adventure.text.Component;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.persistence.PersistentDataHolder;

import java.util.List;
import java.util.Map;

public interface ItemMeta extends PersistentDataHolder, Cloneable {
    boolean hasDisplayName();
    Component displayName();
    void displayName(Component name);
    boolean hasLore();
    List<Component> lore();
    void lore(List<Component> lore);
    void addItemFlags(ItemFlag... flags);
    void removeItemFlags(ItemFlag... flags);
    boolean isUnbreakable();
    void setUnbreakable(boolean unbreakable);
    boolean addEnchant(Enchantment enchantment, int level, boolean ignoreLevelRestriction);
    boolean removeEnchant(Enchantment enchantment);
    boolean hasEnchant(Enchantment enchantment);
    Map<Enchantment, Integer> getEnchants();
    void setEnchantmentGlintOverride(Boolean override);
    Boolean getEnchantmentGlintOverride();
    boolean hasEnchantmentGlintOverride();
    void setCustomModelData(Integer data);
    boolean hasCustomModelData();
    Integer getCustomModelData();
    ItemMeta clone();
}
