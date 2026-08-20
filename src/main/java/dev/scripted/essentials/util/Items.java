package dev.scripted.essentials.util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Stores and restores inventory contents in YAML, empty slots included. */
public final class Items {

    private Items() {
    }

    /** Writes an inventory array, keeping null slots so positions survive the round trip. */
    public static void writeArray(ConfigurationSection section, String path, ItemStack[] items) {
        List<ItemStack> list = new ArrayList<>(items.length);
        for (ItemStack item : items) {
            list.add(item);
        }
        section.set(path, list);
    }

    /** Reads an array written by {@link #writeArray}, padded or trimmed to {@code size}. */
    public static ItemStack[] readArray(ConfigurationSection section, String path, int size) {
        ItemStack[] items = new ItemStack[size];
        List<?> raw = section.getList(path);
        if (raw == null) {
            return items;
        }
        for (int index = 0; index < Math.min(size, raw.size()); index++) {
            if (raw.get(index) instanceof ItemStack item) {
                items[index] = item;
            }
        }
        return items;
    }

    /** True when the stack holds nothing worth saving. */
    public static boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir() || item.getAmount() <= 0;
    }

    public static int countNonEmpty(ItemStack[] items) {
        int count = 0;
        for (ItemStack item : items) {
            if (!isEmpty(item)) {
                count++;
            }
        }
        return count;
    }
}
