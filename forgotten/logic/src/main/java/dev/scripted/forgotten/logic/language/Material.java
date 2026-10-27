package dev.scripted.forgotten.logic.language;

/**
 * The world speaks in classes of material, not exact blocks: any log is WOOD, any leaves LEAF.
 * That is what makes the language learnable, and what lets a player answer in their own blocks.
 */
public enum Material {
    EARTH, STONE, WOOD, LEAF, SAND, PLANT, LIGHT, GLASS, WOOL, METAL;

    /** Classifies a block id, or returns null for anything that is not part of the language. */
    public static Material of(String block) {
        String path = block.contains(":") ? block.substring(block.indexOf(':') + 1) : block;
        if (path.endsWith("_wool")) {
            return WOOL;
        }
        if (path.endsWith("_leaves")) {
            return LEAF;
        }
        if (path.endsWith("_log") || path.endsWith("_wood") || path.endsWith("_planks") || path.endsWith("_stem")) {
            return WOOD;
        }
        if (path.endsWith("_sapling") || path.endsWith("_tulip") || path.equals("poppy") || path.equals("dandelion")
                || path.equals("cornflower") || path.equals("azure_bluet") || path.equals("oxeye_daisy")
                || path.equals("allium") || path.equals("blue_orchid") || path.equals("lily_of_the_valley")) {
            return PLANT;
        }
        if (path.equals("glass") || path.endsWith("_glass") || path.endsWith("glass_pane")) {
            return GLASS;
        }
        if (path.equals("torch") || path.equals("lantern") || path.equals("glowstone") || path.equals("sea_lantern")
                || path.equals("soul_torch") || path.equals("soul_lantern") || path.equals("shroomlight")
                || path.equals("jack_o_lantern") || path.equals("redstone_lamp") || path.equals("end_rod")) {
            return LIGHT;
        }
        if (path.equals("iron_block") || path.equals("gold_block") || path.equals("copper_block")
                || path.equals("iron_bars") || path.equals("chain") || path.equals("anvil")) {
            return METAL;
        }
        if (path.equals("sand") || path.equals("red_sand") || path.equals("sandstone") || path.equals("gravel")) {
            return SAND;
        }
        if (path.equals("dirt") || path.equals("grass_block") || path.equals("coarse_dirt") || path.equals("podzol")
                || path.equals("rooted_dirt") || path.equals("mud") || path.equals("mycelium") || path.equals("moss_block")) {
            return EARTH;
        }
        if (path.equals("stone") || path.equals("cobblestone") || path.equals("deepslate") || path.equals("andesite")
                || path.equals("diorite") || path.equals("granite") || path.equals("tuff") || path.equals("stone_bricks")
                || path.equals("cobbled_deepslate") || path.equals("mossy_cobblestone") || path.equals("calcite")
                || path.equals("smooth_stone")) {
            return STONE;
        }
        return null;
    }

    /** A representative block the world uses when it writes this material itself. */
    public String canonicalBlock() {
        return switch (this) {
            case EARTH -> "minecraft:dirt";
            case STONE -> "minecraft:stone";
            case WOOD -> "minecraft:oak_log";
            case LEAF -> "minecraft:oak_leaves";
            case SAND -> "minecraft:sand";
            case PLANT -> "minecraft:oak_sapling";
            case LIGHT -> "minecraft:lantern";
            case GLASS -> "minecraft:glass";
            case WOOL -> "minecraft:white_wool";
            case METAL -> "minecraft:iron_block";
        };
    }
}
