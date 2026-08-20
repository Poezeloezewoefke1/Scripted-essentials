package dev.scripted.essentials.gui;

import dev.scripted.essentials.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/** Fluent {@link ItemStack} construction, used for every menu icon and every given item. */
public final class ItemBuilder {

    private final ItemStack item;
    private final List<Component> lore = new ArrayList<>();

    private ItemBuilder(Material material, int amount) {
        this.item = new ItemStack(material, amount);
    }

    public static ItemBuilder of(Material material) {
        return new ItemBuilder(material, 1);
    }

    public static ItemBuilder of(Material material, int amount) {
        return new ItemBuilder(material, amount);
    }

    public static ItemBuilder copyOf(ItemStack source) {
        ItemBuilder builder = new ItemBuilder(source.getType(), source.getAmount());
        builder.item.setItemMeta(source.getItemMeta());
        return builder;
    }

    public ItemBuilder name(String miniMessage) {
        return meta(meta -> meta.displayName(Text.item(miniMessage)));
    }

    public ItemBuilder name(Component component) {
        return meta(meta -> meta.displayName(component.decoration(
                net.kyori.adventure.text.format.TextDecoration.ITALIC, false)));
    }

    public ItemBuilder lore(String miniMessage) {
        lore.add(Text.item(miniMessage));
        return this;
    }

    public ItemBuilder lore(Component component) {
        lore.add(component.decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        return this;
    }

    public ItemBuilder loreLines(List<String> lines) {
        lines.forEach(this::lore);
        return this;
    }

    /** Adds an empty lore line, used to separate blocks of information in menu icons. */
    public ItemBuilder blank() {
        lore.add(Component.empty());
        return this;
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(Math.max(1, Math.min(item.getMaxStackSize(), amount)));
        return this;
    }

    /** Applies the enchantment shimmer without adding a real enchantment. */
    public ItemBuilder glow(boolean glowing) {
        return meta(meta -> meta.setEnchantmentGlintOverride(glowing));
    }

    /** Hides attribute modifiers, enchantments and similar clutter from the tooltip. */
    public ItemBuilder clean() {
        return meta(meta -> meta.addItemFlags(ItemFlag.values()));
    }

    public ItemBuilder unbreakable() {
        return meta(meta -> meta.setUnbreakable(true));
    }

    public ItemBuilder skull(OfflinePlayer owner) {
        return meta(meta -> {
            if (meta instanceof SkullMeta skullMeta) {
                skullMeta.setOwningPlayer(owner);
            }
        });
    }

    /** Stores a hidden string tag, used to recognise menu buttons and plugin-issued items. */
    public ItemBuilder tag(NamespacedKey key, String value) {
        return meta(meta -> meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value));
    }

    public ItemBuilder meta(java.util.function.Consumer<ItemMeta> mutator) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            mutator.accept(meta);
            item.setItemMeta(meta);
        }
        return this;
    }

    public ItemStack build() {
        if (!lore.isEmpty()) {
            meta(meta -> meta.lore(lore));
        }
        return item;
    }
}
