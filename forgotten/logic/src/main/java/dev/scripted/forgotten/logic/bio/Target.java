package dev.scripted.forgotten.logic.bio;

/** What a MOVE instruction walks toward. */
public enum Target {
    MATURE_CROP,
    CONTAINER,
    HOSTILE,
    OWNER,
    HOME,
    ITEM;

    public static Target forItem(String item) {
        if (item.endsWith("_bed")) {
            return HOME;
        }
        return switch (item) {
            case "minecraft:wheat", "minecraft:carrot", "minecraft:potato", "minecraft:beetroot",
                 "minecraft:wheat_seeds", "minecraft:nether_wart" -> MATURE_CROP;
            case "minecraft:chest", "minecraft:barrel", "minecraft:trapped_chest" -> CONTAINER;
            case "minecraft:rotten_flesh", "minecraft:bone", "minecraft:zombie_head", "minecraft:skeleton_skull" -> HOSTILE;
            case "minecraft:player_head", "minecraft:totem_of_undying" -> OWNER;
            case "minecraft:item_frame", "minecraft:glow_item_frame" -> ITEM;
            default -> null;
        };
    }
}
