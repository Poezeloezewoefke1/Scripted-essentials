package dev.scripted.forgotten.logic.bio;

/** What an IF instruction tests. */
public enum Condition {
    BUFFER_FULL,
    BUFFER_EMPTY,
    HOSTILE_NEAR,
    CROP_READY,
    DAY,
    PLAYER_NEAR,
    /** Whether the previous instruction failed: no target found, nothing harvested, nothing moved. */
    FAILED;

    public static Condition forItem(String item) {
        return switch (item) {
            case "minecraft:chest", "minecraft:barrel" -> BUFFER_FULL;
            case "minecraft:bucket" -> BUFFER_EMPTY;
            case "minecraft:rotten_flesh", "minecraft:bone" -> HOSTILE_NEAR;
            case "minecraft:wheat", "minecraft:carrot", "minecraft:potato", "minecraft:beetroot" -> CROP_READY;
            case "minecraft:daylight_detector" -> DAY;
            case "minecraft:sculk_sensor", "minecraft:calibrated_sculk_sensor" -> PLAYER_NEAR;
            case "minecraft:redstone_torch" -> FAILED;
            default -> null;
        };
    }
}
