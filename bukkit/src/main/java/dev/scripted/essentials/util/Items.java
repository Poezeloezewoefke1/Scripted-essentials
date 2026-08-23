package dev.scripted.essentials.util;

import dev.scripted.essentials.config.YamlDocument;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Stores and restores inventory contents in YAML.
 *
 * <p>Items are written as base64 of the vanilla NBT that {@code serializeAsBytes} produces,
 * rather than Bukkit's own serialization. That keeps the data readable by any platform that can
 * parse an item's NBT, so a kit saved by the Paper plugin can be read by the Fabric mod.
 *
 * <p>Empty slots are written as empty strings so positions survive the round trip.
 */
public final class Items {

    private static final String EMPTY = "";

    private Items() {
    }

    public static void writeArray(YamlDocument document, String path, ItemStack[] items) {
        List<String> encoded = new ArrayList<>(items.length);
        for (ItemStack item : items) {
            encoded.add(encode(item));
        }
        document.set(path, encoded);
    }

    /** Reads an array written by {@link #writeArray}, padded or trimmed to {@code size}. */
    public static ItemStack[] readArray(YamlDocument document, String path, int size) {
        ItemStack[] items = new ItemStack[size];
        List<String> encoded = document.getStringList(path);
        for (int index = 0; index < Math.min(size, encoded.size()); index++) {
            items[index] = decode(encoded.get(index));
        }
        return items;
    }

    public static String encode(ItemStack item) {
        if (isEmpty(item)) {
            return EMPTY;
        }
        return Base64.getEncoder().encodeToString(item.serializeAsBytes());
    }

    /** Returns null for an empty entry, or for data this server cannot read. */
    public static ItemStack decode(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return null;
        }
        try {
            return ItemStack.deserializeBytes(Base64.getDecoder().decode(encoded));
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Data from a newer server, or a hand-edited entry. Losing one slot beats
            // failing the whole kit or snapshot.
            return null;
        }
    }

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
