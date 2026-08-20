package dev.scripted.essentials.core;

import org.bukkit.Material;

/** Grouping used by the toggle menu's category tabs. */
public enum FeatureCategory {

    PLAYER("Player", Material.PLAYER_HEAD),
    TELEPORTATION("Teleportation", Material.ENDER_PEARL),
    INVENTORY("Inventory", Material.CHEST),
    MODERATION("Moderation", Material.IRON_BARS),
    WORLD("World", Material.GRASS_BLOCK),
    CHAT("Chat", Material.PAPER),
    SYSTEMS("Systems", Material.NETHER_STAR);

    private final String displayName;
    private final Material icon;

    FeatureCategory(String displayName, Material icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    public String displayName() {
        return displayName;
    }

    public Material icon() {
        return icon;
    }
}
